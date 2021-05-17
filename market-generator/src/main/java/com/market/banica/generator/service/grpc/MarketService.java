package com.market.banica.generator.service.grpc;

import com.market.CatalogueRequest;
import com.market.CatalogueResponse;
import com.market.MarketDataRequest;
import com.market.MarketServiceGrpc;
import com.market.TickResponse;
import com.market.banica.generator.service.MarketState;
import com.market.banica.generator.service.SubscriptionManager;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketService extends MarketServiceGrpc.MarketServiceImplBase {

    private static final Logger LOGGER = LogManager.getLogger(MarketService.class);

    private final SubscriptionManager subscriptionManager;

    private final MarketState marketState;

    @Autowired
    public MarketService(SubscriptionManager subscriptionManager, MarketState marketState) {
        this.subscriptionManager = subscriptionManager;
        this.marketState = marketState;
    }

    @Override
    public StreamObserver<MarketDataRequest> subscribeForItem(StreamObserver<TickResponse> responseObserver) {

        final ServerCallStreamObserver<TickResponse> serverCallStreamObserver =
                (ServerCallStreamObserver<TickResponse>) responseObserver;
        serverCallStreamObserver.disableAutoRequest();

        class OnReadyHandler implements Runnable {
            private boolean wasReady = false;

            @Override
            public void run() {
                while (serverCallStreamObserver.isReady() && !wasReady) {
                    wasReady = true;
                    serverCallStreamObserver.request(1);
                }
            }
        }

        final OnReadyHandler onReadyHandler = new OnReadyHandler();
        serverCallStreamObserver.setOnReadyHandler(onReadyHandler);

        return new StreamObserver<MarketDataRequest>() {
            @Override
            public void onNext(MarketDataRequest request) {
                try {

                    boolean hasSuccessfulBootstrap = bootstrapGeneratedTicks(request, serverCallStreamObserver);
                    if (hasSuccessfulBootstrap) {
                        subscriptionManager.subscribe(request, serverCallStreamObserver);
                    }

                    if (serverCallStreamObserver.isReady()) {
                        serverCallStreamObserver.request(1);
                    } else {
                        onReadyHandler.wasReady = false;
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    serverCallStreamObserver.onError(
                            Status.UNKNOWN.withDescription("Error handling request.").withCause(throwable).asException());
                }
            }

            public void onError(Throwable throwable) {
                LOGGER.warn("Unable to request.");
                LOGGER.error(throwable.getMessage());
            }

            public void onCompleted() {
                LOGGER.info("onCompleted called.");
            }
        };


    }

    @Override
    public void requestCatalogue(CatalogueRequest request, StreamObserver<CatalogueResponse> responseObserver) {
        super.requestCatalogue(request, responseObserver);
    }

    private boolean bootstrapGeneratedTicks(MarketDataRequest request,
                                            StreamObserver<TickResponse> responseStreamObserver) {
        String goodName = request.getGoodName();
        ServerCallStreamObserver<TickResponse> cancellableSubscriber = (ServerCallStreamObserver<TickResponse>) responseStreamObserver;
        for (TickResponse tick : marketState.generateMarketTicks(goodName)) {
            if (cancellableSubscriber.isCancelled()) {
                responseStreamObserver.onError(Status.CANCELLED
                        .withDescription(responseStreamObserver + " has stopped requesting product " + goodName)
                        .asException());
                return false;
            }
            responseStreamObserver.onNext(tick);
        }
        return true;
    }

}
