package com.market.banica.order.book.service;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.order.book.service.grpc.OrderBookService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.market.banica.order.book.helper.ConstantsAuroraRequestHandler.*;

import java.util.Locale;


@Component
public class AuroraRequestHandler extends AuroraServiceGrpc.AuroraServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraRequestHandler.class);

    private final OrderBookService orderBookService;

    @Autowired
    public AuroraRequestHandler(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }


    @Override
    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        String[] topicSplit = request.getTopic().split(SPLIT_SLASH_REGEX);
        String prefix = topicSplit[0];
        if (!prefix.equals(ORDERBOOK)) {
            LOGGER.debug(INVALID_REQUEST_PREFIX, prefix);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(INVALID_REQUEST).asException());
            return;
        }
        if (topicSplit.length == 3 && isValidNumber(topicSplit[2])) {
            LOGGER.info(IN_GET_ORDER_BOOK_ITEM_LAYERS_METHOD);
            orderBookService.getOrderBookItemLayers(request, responseObserver);
        } else if (topicSplit.length == 2 && topicSplit[1].contains(SPLIT_EQUALS_REGEX)) {
            String command = topicSplit[1].split(SPLIT_EQUALS_REGEX)[1].toLowerCase(Locale.ROOT);

            if (command.equals(SUBSCRIBE)) {
                LOGGER.info(IN_ANNOUNCE_ITEM_INTEREST_METHOD);

                orderBookService.announceItemInterest(request, responseObserver);
                return;
            }
            LOGGER.info(IN_CANCEL_ITEM_SUBSCRIPTION_METHOD);

            orderBookService.cancelItemSubscription(request, responseObserver);
        }
    }

    @Override
    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED.withDescription(METHOD_IS_NOT_IMPLEMENTED).asException());
    }

    private boolean isValidNumber(String number) {
        return NUMBER_CHECK_PATTERN.matcher(number).matches();
    }
}
