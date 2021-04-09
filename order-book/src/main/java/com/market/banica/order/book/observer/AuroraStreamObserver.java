package com.market.banica.order.book.observer;

import com.aurora.Aurora;
import com.market.banica.order.book.model.ItemMarket;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class AuroraStreamObserver implements StreamObserver<Aurora.AuroraResponse> {
    private final ItemMarket itemMarket;

    private static final Logger LOGGER = LogManager.getLogger(AuroraStreamObserver.class);


    @Autowired
    public AuroraStreamObserver(ItemMarket itemMarket) {
        this.itemMarket = itemMarket;
    }

    @Override
    public void onNext(Aurora.AuroraResponse response) {
        itemMarket.updateItem(response);
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.warn("Unable to request");
        LOGGER.error(throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        LOGGER.info("Market data gathered");
    }
}
