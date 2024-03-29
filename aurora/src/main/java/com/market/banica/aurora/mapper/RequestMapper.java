package com.market.banica.aurora.mapper;


import com.aurora.Aurora;
import com.google.protobuf.Any;
import com.market.AvailabilityResponse;
import com.market.BuySellProductResponse;
import com.market.ProductBuySellRequest;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.config.StubManager;
import com.orderbook.CancelSubscriptionRequest;
import com.orderbook.CancelSubscriptionResponse;
import com.orderbook.InterestsRequest;
import com.orderbook.InterestsResponse;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractStub;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.management.ServiceNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.NoSuchObjectException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@NoArgsConstructor
@Service
public class RequestMapper {

    public static final String SPLIT_SLASH_REGEX = "/+";
    public static final String SPLIT_EQUALS_REGEX = "=";
    public static final String NUMBER_CHECK_REGEX = "^\\d+$";
    public static final String ORDERBOOK = "orderbook";
    public static final String AURORA = "aurora";
    public static final String MARKET = "market";

    public static final String AVAILABILITY_ACTION = "availability";
    public static final String RETURN_ACTION = "return";
    public static final String BUY_ACTION = "buy";
    public static final String SELL_ACTION = "sell";

    public static final String BAD_PUBLISHER_REQUEST = "Unknown Publisher";
    public static final String IN_CANCEL_ITEM_SUBSCRIPTION = "Forwarding to orderbook - cancelItemSubscription.";
    public static final String IN_ANNOUNCE_ITEM_INTEREST = "Forwarding to orderbook - announceItemInterest.";
    public static final String IN_GET_ORDER_BOOK_ITEM_LAYERS = "Forwarding to orderbook - getOrderBookItemLayers.";
    public static final String IN_AURORA_REQUEST = "Forwarding to aurora - request.";
    public static final String SUBSCRIBE = "subscribe";
    public static final Pattern NUMBER_CHECK_PATTERN =
            Pattern.compile(NUMBER_CHECK_REGEX);
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestMapper.class);
    private ChannelManager channelManager;
    private StubManager stubManager;

    @Autowired
    public RequestMapper(StubManager stubManager, ChannelManager channelManager) {
        this.stubManager = stubManager;
        this.channelManager = channelManager;
    }

    public Aurora.AuroraResponse renderRequest(Aurora.AuroraRequest incomingRequest) throws NoSuchObjectException, ServiceNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String destinationOfMessage = incomingRequest.getTopic().split("/")[0];
        Optional<ManagedChannel> channelByKey = channelManager.getChannelByKey(destinationOfMessage);

        if (!channelByKey.isPresent()) {
            throw new NoSuchObjectException("Channel " + destinationOfMessage + " is not available at the moment.");
        }

        if (destinationOfMessage.contains(ORDERBOOK)) {
            return this.renderOrderbookMapping(incomingRequest, channelByKey.get());
        } else if (destinationOfMessage.contains(AURORA)) {
            return this.renderAuroraMapping(incomingRequest, channelByKey.get());
        } else if (destinationOfMessage.contains(MARKET)) {
            return this.renderMarketMapping(incomingRequest, channelByKey.get());
        }

        throw new ServiceNotFoundException(BAD_PUBLISHER_REQUEST + ". Requested publisher is: " + destinationOfMessage);
    }

    private Aurora.AuroraResponse renderAuroraMapping(Aurora.AuroraRequest incomingRequest, ManagedChannel channelByKey) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        LOGGER.debug("Mapping request for aurora.");
        AbstractStub<? extends AbstractBlockingStub<?>> auroraStub = stubManager.getBlockingStub(channelByKey, AURORA);

        Method auroraRequest = auroraStub.getClass().getMethod("request", Aurora.AuroraRequest.class);

        LOGGER.info(IN_AURORA_REQUEST);
        return (Aurora.AuroraResponse) auroraRequest.invoke(auroraStub, incomingRequest);
    }

    private boolean isValidNumber(String number) {
        return NUMBER_CHECK_PATTERN.matcher(number).matches();
    }

    private Aurora.AuroraResponse renderOrderbookMapping(Aurora.AuroraRequest incomingRequest, ManagedChannel channelByKey) {
        LOGGER.debug("Mapping messages for orderbook.");
        String[] topicSplit = incomingRequest.getTopic().split(SPLIT_SLASH_REGEX);


        AbstractBlockingStub<? extends AbstractBlockingStub<?>> orderbookBlockingStub = stubManager.getBlockingStub(channelByKey, ORDERBOOK);

        try {
            if (topicSplit.length == 3 && isValidNumber(topicSplit[2])) {
                return processItemOrderBookRequest(incomingRequest, orderbookBlockingStub, topicSplit);

            } else if (topicSplit.length == 2 && topicSplit[1].contains(SPLIT_EQUALS_REGEX)) {
                return renderOrderbookRequestSubscribeRequest(incomingRequest, orderbookBlockingStub, topicSplit[1]);
            }
        } catch (Exception e) {
            RuntimeException runtimeException = new RuntimeException("Got error from orderbook with message " + e.getMessage());
            runtimeException.setStackTrace(e.getStackTrace());
            throw runtimeException;
        }

        throw new IllegalArgumentException("Client requested an unsupported message from orderbook. Message is: " + incomingRequest.getTopic());
    }

    private Aurora.AuroraResponse renderOrderbookRequestSubscribeRequest(Aurora.AuroraRequest incomingRequest, AbstractBlockingStub<? extends AbstractBlockingStub<?>> orderBookServiceBlockingStub, String s) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String command = s.split(SPLIT_EQUALS_REGEX)[1].toLowerCase(Locale.ROOT);
        String itemName = s.split(SPLIT_EQUALS_REGEX)[0].toLowerCase(Locale.ROOT);

        if (command.equals(SUBSCRIBE)) {
            return processSubscribeForItemRequest(incomingRequest, orderBookServiceBlockingStub, itemName);
        }
        LOGGER.info(IN_CANCEL_ITEM_SUBSCRIPTION);

        return processCancelItemSubscriptionRequest(incomingRequest, orderBookServiceBlockingStub, itemName);
    }


    private Aurora.AuroraResponse processCancelItemSubscriptionRequest(Aurora.AuroraRequest incomingRequest, AbstractBlockingStub<? extends AbstractBlockingStub<?>> orderBookServiceBlockingStub, String itemName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CancelSubscriptionRequest build = CancelSubscriptionRequest.newBuilder()
                .setClientId(incomingRequest.getClientId())
                .setItemName(itemName)
                .build();

        Method orderBookCancelItemSubscriptionRequest = orderBookServiceBlockingStub.getClass().getMethod("cancelItemSubscription", CancelSubscriptionRequest.class);
        CancelSubscriptionResponse cancelSubscriptionResponse = (CancelSubscriptionResponse) orderBookCancelItemSubscriptionRequest.invoke(orderBookServiceBlockingStub, build);

        return Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(cancelSubscriptionResponse))
                .build();
    }

    private Aurora.AuroraResponse processSubscribeForItemRequest(Aurora.AuroraRequest incomingRequest, AbstractBlockingStub<? extends AbstractBlockingStub<?>> orderBookServiceBlockingStub, String itemName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        LOGGER.info(IN_ANNOUNCE_ITEM_INTEREST);

        InterestsRequest interestsRequest = InterestsRequest.newBuilder()
                .setClientId(incomingRequest.getClientId())
                .setItemName(itemName)
                .build();

        Method announceItemInterest = orderBookServiceBlockingStub.getClass().getMethod("announceItemInterest", InterestsRequest.class);
        InterestsResponse interestsResponse = (InterestsResponse) announceItemInterest.invoke(orderBookServiceBlockingStub, interestsRequest);

        return Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(interestsResponse))
                .build();
    }

    private Aurora.AuroraResponse processItemOrderBookRequest(Aurora.AuroraRequest incomingRequest, AbstractBlockingStub<? extends AbstractBlockingStub<?>> orderBookServiceBlockingStub, String[] topicSplit) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        LOGGER.info(IN_GET_ORDER_BOOK_ITEM_LAYERS);

        ItemOrderBookRequest build = ItemOrderBookRequest.newBuilder()
                .setClientId(incomingRequest.getClientId())
                .setItemName(topicSplit[1])
                .setQuantity(Long.parseLong(topicSplit[2]))
                .build();

        Method orderBookGetItemLayers = orderBookServiceBlockingStub.getClass().getMethod("getOrderBookItemLayers", ItemOrderBookRequest.class);
        ItemOrderBookResponse orderBookItemLayers = (ItemOrderBookResponse) orderBookGetItemLayers.invoke(orderBookServiceBlockingStub, build);

        return Aurora.AuroraResponse
                .newBuilder()
                .setMessage(Any.pack(orderBookItemLayers))
                .build();
    }

    private Aurora.AuroraResponse renderMarketMapping(Aurora.AuroraRequest incomingRequest, ManagedChannel channelByKey) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        LOGGER.debug("Mapping messages for market.");

        AbstractBlockingStub<? extends AbstractBlockingStub<?>> marketBlockingStub = stubManager.getBlockingStub(channelByKey, MARKET);

        String[] topicSplit = incomingRequest.getTopic().split(SPLIT_SLASH_REGEX);
        String itemName = topicSplit[2];
        double itemPrice = Double.parseDouble(topicSplit[3]);
        long itemQuantity = Long.parseLong(topicSplit[4]);
        String marketName = topicSplit[0].split("-")[1].toUpperCase(Locale.ROOT);

        ProductBuySellRequest.Builder request = ProductBuySellRequest.newBuilder()
                .setItemName(itemName)
                .setItemPrice(itemPrice)
                .setItemQuantity(itemQuantity)
                .setMarketName(marketName);

        String requestPrefix = topicSplit[1];

        Method marketCheckAvailability = marketBlockingStub.getClass().getMethod("checkAvailability", ProductBuySellRequest.class);
        Method marketReturnPendingProduct = marketBlockingStub.getClass().getMethod("returnPendingProduct", ProductBuySellRequest.class);
        Method marketBuyProduct = marketBlockingStub.getClass().getMethod("buyProduct", ProductBuySellRequest.class);
        Method marketSellProduct = marketBlockingStub.getClass().getMethod("sellProduct", ProductBuySellRequest.class);

        switch (requestPrefix) {
            case AVAILABILITY_ACTION:
                return Aurora.AuroraResponse.newBuilder()
                        .setMessage(Any.pack((AvailabilityResponse) marketCheckAvailability.invoke(marketBlockingStub, request.build())))
                        .build();
            case RETURN_ACTION:
                return Aurora.AuroraResponse.newBuilder()
                        .setMessage(Any.pack((BuySellProductResponse) marketReturnPendingProduct.invoke(marketBlockingStub, request.build())))
                        .build();
            case BUY_ACTION:
                return Aurora.AuroraResponse.newBuilder()
                        .setMessage(Any.pack((BuySellProductResponse) marketBuyProduct.invoke(marketBlockingStub, request.build())))
                        .build();
            case SELL_ACTION:
                return Aurora.AuroraResponse.newBuilder()
                        .setMessage(Any.pack((BuySellProductResponse) marketSellProduct.invoke(marketBlockingStub, request.build())))
                        .build();
            default:
                throw new IllegalArgumentException("Client requested an unsupported message from market. Message is: " + incomingRequest.getTopic());
        }
    }
}
