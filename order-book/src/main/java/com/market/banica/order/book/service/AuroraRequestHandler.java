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

import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    private final HashMap<String, BiConsumer<Aurora.AuroraRequest, StreamObserver<Aurora.AuroraResponse>>> strategy = new HashMap<>();

    @Autowired
    public AuroraRequestHandler(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
        populateStrategyMap(strategy);
    }

    private void populateStrategyMap(HashMap<String, BiConsumer<Aurora.AuroraRequest, StreamObserver<Aurora.AuroraResponse>>> strategy) {
        strategy.put("/", (req, res) -> orderBookService.getOrderBookItemLayers(req, res));
        strategy.put("subscribe", (req, res) -> orderBookService.announceItemInterest(req, res));
        strategy.put("unsubscribe", (req, res) -> orderBookService.cancelItemSubscription(req, res));
    }


    @Override
    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        String[] topicSplit = request.getTopic().split(SPLIT_SLASH_REGEX);
        String prefix = topicSplit[0];
        if (!request.getTopic().startsWith("orderbook/")) {
            LOGGER.debug("Not valid request, starts with: {}, expected: 'orderbook'", prefix);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid request").asException());
            return;
        }

        // orderbook/eggs/15
        // orderbook/eggs=subscribe
        // orderbook/eggs=unsubscribe

        String topic = request.getTopic().substring(10, request.getTopic().length());

        Optional<String> key = strategy.keySet()
                .stream()
                .filter(topic::contains)
                .findFirst();

        if (!key.isPresent()){
            return;
        }

        strategy.get(key.get()).accept(request, responseObserver);
    }

    @Override
    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED.withDescription("Method is not implemented").asException());
    }

    private boolean isValidNumber(String number) {
        return NUMBER_CHECK_PATTERN.matcher(number).matches();
    }
}
