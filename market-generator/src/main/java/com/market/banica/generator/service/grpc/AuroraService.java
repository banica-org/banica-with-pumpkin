package com.market.banica.generator.service.grpc;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.TickResponse;
import com.market.banica.generator.service.MarketState;
import com.market.banica.generator.service.MarketSubscriptionManager;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuroraService extends AuroraServiceGrpc.AuroraServiceImplBase {

    private final MarketSubscriptionManager marketSubscriptionManager;

    private final MarketState marketState;

    @Autowired
    public AuroraService(MarketSubscriptionManager marketSubscriptionManager, MarketState marketState) {
        this.marketSubscriptionManager = marketSubscriptionManager;
        this.marketState = marketState;
    }

    @Override
    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        boolean hasSuccessfulBootstrap = bootstrapGeneratedTicks(request, responseObserver);
        if (hasSuccessfulBootstrap) {
            marketSubscriptionManager.subscribe(request, responseObserver);
        }
    }

    @Override
    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        super.request(request, responseObserver);
    }

    private boolean bootstrapGeneratedTicks(Aurora.AuroraRequest request,
                                            StreamObserver<Aurora.AuroraResponse> responseStreamObserver) {
        String goodName = marketSubscriptionManager.getGoodNameFromRequest(request);

        for (TickResponse tick : marketState.generateMarketTicks(goodName)) {
            if (Context.current().isCancelled()) {
                responseStreamObserver.onError(Status.CANCELLED
                        .withDescription(responseStreamObserver.toString() + " has stopped requesting product " + goodName)
                        .asException());
                return false;
            }
            responseStreamObserver.onNext(marketSubscriptionManager.convertTickResponseToAuroraResponse(tick));
        }
        return true;
    }

}
