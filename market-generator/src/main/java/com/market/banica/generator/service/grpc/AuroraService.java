package com.market.banica.generator.service.grpc;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.TickResponse;
import com.market.banica.generator.service.MarketState;
import com.market.banica.generator.service.SubscriptionManager;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuroraService extends AuroraServiceGrpc.AuroraServiceImplBase {

    private final SubscriptionManager subscriptionManager;

    private final MarketState marketState;

    @Autowired
    public AuroraService(SubscriptionManager subscriptionManager, MarketState marketState) {
        this.subscriptionManager = subscriptionManager;
        this.marketState = marketState;
    }

    @Override
    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        boolean hasSuccessfulBootstrap = bootstrapGeneratedTicks(request, responseObserver);
        if (hasSuccessfulBootstrap) {
            subscriptionManager.subscribe(request, responseObserver);
        }
    }

    @Override
    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        super.request(request, responseObserver);
    }

    private boolean bootstrapGeneratedTicks(Aurora.AuroraRequest request,
                                            StreamObserver<Aurora.AuroraResponse> responseStreamObserver) {
        String goodName = subscriptionManager.getGoodNameFromRequest(request);
        ServerCallStreamObserver<Aurora.AuroraResponse> cancellableSubscriber = (ServerCallStreamObserver<Aurora.AuroraResponse>) responseStreamObserver;
        for (TickResponse tick : marketState.generateMarketTicks(goodName)) {
            if (cancellableSubscriber.isCancelled()) {
                responseStreamObserver.onError(Status.CANCELLED
                        .withDescription(responseStreamObserver + " has stopped requesting product " + goodName)
                        .asException());
                return false;
            }
            responseStreamObserver.onNext(subscriptionManager.convertTickResponseToAuroraResponse(tick));
        }
        return true;
    }

}
