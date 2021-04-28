package com.market.banica.order.book.observer;

import com.aurora.Aurora;
import com.market.TickResponse;
import com.market.banica.common.exception.IncorrectResponseException;
import com.market.banica.order.book.model.ItemMarket;
import com.market.banica.order.book.service.grpc.AuroraClient;
import com.orderbook.ReconnectionResponse;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class AuroraStreamObserver implements StreamObserver<Aurora.AuroraResponse> {

    private static final Logger LOGGER = LogManager.getLogger(AuroraStreamObserver.class);

    private final ItemMarket itemMarket;
    private final AuroraClient auroraClient;


    @Autowired
    public AuroraStreamObserver(ItemMarket itemMarket, AuroraClient auroraClient) {
        this.itemMarket = itemMarket;
        this.auroraClient = auroraClient;
    }

    @Override
    public void onNext(Aurora.AuroraResponse response) {

        if (response.getMessage().is(TickResponse.class)) {
            itemMarket.updateItem(response);
        } else if (response.getMessage().is(ReconnectionResponse.class)) {
            auroraClient.reconnectToMarket(response);
        } else {
            throw new IncorrectResponseException("Response is not supported!");
        }

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
