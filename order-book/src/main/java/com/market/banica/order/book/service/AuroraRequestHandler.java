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

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class AuroraRequestHandler extends AuroraServiceGrpc.AuroraServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraRequestHandler.class);


    private final OrderBookService orderBookService;

    private static final String SPLIT_SLASH_REGEX = "/+";
    private static final String SPLIT_EQUALS_REGEX = "=";
    private static final String NUMBER_CHECK_REGEX = "^\\d+$";

    private static final String SUBSCRIBE = "subscribe";

    private static final Pattern NUMBER_CHECK_PATTERN =
            Pattern.compile(NUMBER_CHECK_REGEX);


    @Autowired
    public AuroraRequestHandler(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }


    @Override
    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        String[] topicSplit = request.getTopic().split(SPLIT_SLASH_REGEX);
        String prefix = topicSplit[0];
        if (!prefix.equals("orderbook")) {
            LOGGER.debug("Not valid request, starts with: {}, expected: 'orderbook'", prefix);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid request").asException());
            return;
        }
        if (topicSplit.length == 3 && isValidNumber(topicSplit[2])) {
            LOGGER.info("Forwarding to orderBookService.getOrderBookItemLayers method");
            orderBookService.getOrderBookItemLayers(request, responseObserver);
        } else if (topicSplit.length == 2 && topicSplit[1].contains(SPLIT_EQUALS_REGEX)) {
            String command = topicSplit[1].split(SPLIT_EQUALS_REGEX)[1].toLowerCase(Locale.ROOT);

            if (command.equals(SUBSCRIBE)) {
                LOGGER.info("Forwarding to orderBookService.announceItemInterest method");

                orderBookService.announceItemInterest(request, responseObserver);
                return;
            }
            LOGGER.info("Forwarding to orderBookService.cancelItemSubscription method");

            orderBookService.cancelItemSubscription(request, responseObserver);
        }
    }

    @Override
    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED.withDescription("Method is not implemented").asException());
    }

    private boolean isValidNumber(String number) {
        return NUMBER_CHECK_PATTERN.matcher(number).matches();
    }
}
