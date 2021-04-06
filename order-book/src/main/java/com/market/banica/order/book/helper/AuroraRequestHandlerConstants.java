package com.market.banica.order.book.helper;

import java.util.regex.Pattern;

public class AuroraRequestHandlerConstants {

    public static final String SPLIT_SLASH_REGEX = "/+";
    public static final String SPLIT_EQUALS_REGEX = "=";
    public static final String NUMBER_CHECK_REGEX = "^\\d+$";

    public static final String ORDERBOOK = "orderbook";
    public static final String INVALID_REQUEST = "Invalid request";
    public static final String METHOD_IS_NOT_IMPLEMENTED = "Method is not implemented";
    public static final String IN_CANCEL_ITEM_SUBSCRIPTION_METHOD = "Forwarding to orderBookService.cancelItemSubscription method";
    public static final String IN_ANNOUNCE_ITEM_INTEREST_METHOD = "Forwarding to orderBookService.announceItemInterest method";
    public static final String IN_GET_ORDER_BOOK_ITEM_LAYERS_METHOD = "Forwarding to orderBookService.getOrderBookItemLayers method";
    public static final String INVALID_REQUEST_PREFIX = "Not valid request, starts with: {}, expected: 'orderbook'";

    public static final String SUBSCRIBE = "subscribe";

    public static final Pattern NUMBER_CHECK_PATTERN =
            Pattern.compile(NUMBER_CHECK_REGEX);

}
