package com.market.banica.order.book.service;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.order.book.service.grpc.OrderBookService;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class AuroraRequestHandler extends AuroraServiceGrpc.AuroraServiceImplBase {
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
        if (!topicSplit[0].equals("orderbook")) {
            //TODO add logger
            return;
        }
        if (topicSplit.length == 3 && isValidNumber(topicSplit[2])) {
            orderBookService.getOrderBookItemLayers(request, responseObserver);
        } else if (topicSplit.length == 2 && topicSplit[1].contains(SPLIT_EQUALS_REGEX)) {
            String command = topicSplit[1].split(SPLIT_EQUALS_REGEX)[1].toLowerCase(Locale.ROOT);

            if (command.equals(SUBSCRIBE)) {
                orderBookService.announceItemInterest(request, responseObserver);
                return;
            }

            orderBookService.cancelItemSubscription(request, responseObserver);
        }
        // orderbook/eggs/15
        // orderbook/eggs=subscribe -> market/eggs
        // orderbook/eggs=unsubscribe
    }

    @Override
    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        super.subscribe(request, responseObserver);
        //todo throw exception
    }

    private boolean isValidNumber(String number) {
        return NUMBER_CHECK_PATTERN.matcher(number).matches();
    }
}
