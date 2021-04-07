package com.market.banica.aurora.mapper;


import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.Any;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.config.StubManager;
import com.orderbook.CancelSubscriptionRequest;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsRequest;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.management.ServiceNotFoundException;
import java.rmi.NoSuchObjectException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class RequestMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestMapper.class);

    private final ChannelManager channelManager;
    private final StubManager stubManager;

    public static final String SPLIT_SLASH_REGEX = "/+";
    public static final String SPLIT_EQUALS_REGEX = "=";
    public static final String NUMBER_CHECK_REGEX = "^\\d+$";

    public static final String ORDERBOOK = "orderbook";
    public static final String AURORA = "aurora";
    public static final String BAD_PUBLISHER_REQUEST = "Unknown Publisher";
    public static final String IN_CANCEL_ITEM_SUBSCRIPTION_METHOD = "Forwarding to orderBookService.cancelItemSubscription method";
    public static final String IN_ANNOUNCE_ITEM_INTEREST_METHOD = "Forwarding to orderBookService.announceItemInterest method";
    public static final String IN_GET_ORDER_BOOK_ITEM_LAYERS_METHOD = "Forwarding to orderBookService.getOrderBookItemLayers method";

    public static final String SUBSCRIBE = "subscribe";

    public static final Pattern NUMBER_CHECK_PATTERN =
            Pattern.compile(NUMBER_CHECK_REGEX);

    @Autowired
    public RequestMapper(StubManager stubManager, ChannelManager channelManager) {
        this.stubManager = stubManager;
        this.channelManager = channelManager;
    }

    public Aurora.AuroraResponse renderRequest(Aurora.AuroraRequest incomingRequest) throws NoSuchObjectException, ServiceNotFoundException {
        String destinationOfMessage = incomingRequest.getTopic().split("/")[0];
        Optional<ManagedChannel> channelByKey = channelManager.getChannelByKey(destinationOfMessage);

        if (!channelByKey.isPresent()) {
            throw new NoSuchObjectException("Channel " + destinationOfMessage + " is not available at the moment.");
        }

        if (destinationOfMessage.contains(ORDERBOOK)) {
            return this.processOrderBookMapping(incomingRequest, channelByKey);
        } else if (destinationOfMessage.contains(AURORA)) {
            return processAuroraRequest(incomingRequest, channelByKey);
        }

        throw new ServiceNotFoundException(BAD_PUBLISHER_REQUEST + ". Requested publisher is: " + destinationOfMessage);
    }

    private Aurora.AuroraResponse processAuroraRequest(Aurora.AuroraRequest incomingRequest, Optional<ManagedChannel> channelByKey) {
        LOGGER.info("Mapping request for aurora.");
        AuroraServiceGrpc.AuroraServiceBlockingStub auroraBlockingStub = stubManager.getAuroraBlockingStub(channelByKey.get());

        Aurora.AuroraResponse request = auroraBlockingStub.request(incomingRequest);
        return request;
    }


    private boolean isValidNumber(String number) {
        return NUMBER_CHECK_PATTERN.matcher(number).matches();
    }

    private Aurora.AuroraResponse processOrderBookMapping(Aurora.AuroraRequest incomingRequest, Optional<ManagedChannel> channelByKey) {
        LOGGER.info("Mapping messages for orderbook.");
        String[] topicSplit = incomingRequest.getTopic().split(SPLIT_SLASH_REGEX);

        try {
            if (topicSplit.length == 3 && isValidNumber(topicSplit[2])) {
                return processItemOrderBookRequest(incomingRequest, channelByKey, topicSplit);

            } else if (topicSplit.length == 2 && topicSplit[1].contains(SPLIT_EQUALS_REGEX)) {
                String command = topicSplit[1].split(SPLIT_EQUALS_REGEX)[1].toLowerCase(Locale.ROOT);

                OrderBookServiceGrpc.OrderBookServiceBlockingStub orderbookStub = stubManager.getOrderbookBlockingStub(channelByKey.get());

                String itemName = topicSplit[1];

                if (command.equals(SUBSCRIBE)) {
                    return processSubscribeForItemRequest(incomingRequest, orderbookStub, itemName);
                }
                LOGGER.info(IN_CANCEL_ITEM_SUBSCRIPTION_METHOD);

                return processCancelItemSubscriptionRequest(incomingRequest, orderbookStub, itemName);
            }
        } catch (Exception e) {
            RuntimeException runtimeException = new RuntimeException("Got error from orderbook with message " + e.getMessage());
            runtimeException.setStackTrace(e.getStackTrace());
            runtimeException.initCause(e.getCause());
            throw runtimeException;
        }

        throw new IllegalArgumentException("Client requested an unsupported message from orderbook. Message is: " + incomingRequest.getTopic());
    }


    private Aurora.AuroraResponse processCancelItemSubscriptionRequest(Aurora.AuroraRequest incomingRequest, OrderBookServiceGrpc.OrderBookServiceBlockingStub orderbookStub, String itemName) {
        CancelSubscriptionRequest build = CancelSubscriptionRequest.newBuilder()
                .setClientId(incomingRequest.getClientId())
                .setItemName(itemName)
                .build();

        CancelSubscriptionResponse cancelSubscriptionResponse = orderbookStub.cancelItemSubscription(build);

        return Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(cancelSubscriptionResponse))
                .build();
    }


    private Aurora.AuroraResponse processSubscribeForItemRequest(Aurora.AuroraRequest incomingRequest, OrderBookServiceGrpc.OrderBookServiceBlockingStub orderbookStub, String itemName) {
        LOGGER.info(IN_ANNOUNCE_ITEM_INTEREST_METHOD);

        InterestsResponse interestsResponse = orderbookStub.announceItemInterest(InterestsRequest.newBuilder()
                .setClientId(incomingRequest.getClientId())
                .setItemName(itemName)
                .build());

        return Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(interestsResponse))
                .build();
    }

    private Aurora.AuroraResponse processItemOrderBookRequest(Aurora.AuroraRequest incomingRequest, Optional<ManagedChannel> channelByKey, String[] topicSplit) {
        LOGGER.info(IN_GET_ORDER_BOOK_ITEM_LAYERS_METHOD);
        OrderBookServiceGrpc.OrderBookServiceBlockingStub orderbookStub = stubManager.getOrderbookBlockingStub(channelByKey.get());
        ItemOrderBookRequest build = ItemOrderBookRequest.newBuilder()
                .setClientId(incomingRequest.getClientId())
                .setItemName(topicSplit[1])
                .setQuantity(Long.parseLong(topicSplit[2]))
                .build();

        ItemOrderBookResponse orderBookItemLayers = orderbookStub.getOrderBookItemLayers(build);

        return Aurora.AuroraResponse
                .newBuilder()
                .setMessage(Any.pack(orderBookItemLayers))
                .build();
    }
}
