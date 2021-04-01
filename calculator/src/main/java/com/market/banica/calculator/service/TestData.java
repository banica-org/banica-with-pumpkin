package com.market.banica.calculator.service;

import com.market.Origin;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.*;

@Component
public class TestData {

    public Map<String, ItemOrderBookResponse> getTestData() {
        Map<String, ItemOrderBookResponse> data = new LinkedHashMap<>();
        addProductToDatabase(data, "banica", 156000.0, 1);//2+0.9+124 +12750 =12876,9
        addProductToDatabase(data, "pumpkin", 0.003, 300);
        addProductToDatabase(data, "milk", 1.0, 2);
        addProductToDatabase(data, "crusts", 0.0001, 200);//0,5+0.12=0,62
        addProductToDatabase(data, "water", 0.01, 300);
        addProductToDatabase(data, "eggs", 0.01, 12);
        addProductToDatabase(data, "sauce", 4.0, 150);//1,5 +1 + 82,5 =85
        addProductToDatabase(data, "sugar", 0.02, 50);
        addProductToDatabase(data, "ketchup", 0.03, 50);//0.65+1 = 1,65
        addProductToDatabase(data, "tomatoes", 0.01, 65);
        return data;
    }

    private void addProductToDatabase(Map<String, ItemOrderBookResponse> data, String productName, double price, int quantity) {
        data.put(productName, generateItoOrderBookResponse(productName, price, quantity));
    }

    private ItemOrderBookResponse generateItoOrderBookResponse(String productName, double price, int quantity) {

        return ItemOrderBookResponse
                .newBuilder()
                .setItemName(productName)
                .addAllOrderbookLayers(generateOrderBookLayersList(productName, price, quantity))
                .build();

    }

    private List<OrderBookLayer> generateOrderBookLayersList(String productName, double price, int quantity) {

        List<OrderBookLayer> orderBookLayers = new ArrayList<>();

        orderBookLayers.add(generateOrderBookLayer(productName, price, quantity));

        return orderBookLayers;
    }

    private OrderBookLayer generateOrderBookLayer(String productName, double price, long quantity) {
        switch (productName) {
            case "banica":
                return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();

            case "pumpkin":
                return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();

            case "milk":
                return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();

            case "crusts":
                return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();

            case "water":
                return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();

            case "eggs":
                return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();
            case "sauce":
                return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();
            case "sugar":
                return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();

            case "ketchup":
                return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();

            case "tomatoes":
                return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();

            default:
                return null;

        }

    }

    private Origin getRandomOrigin() {
        return Origin.forNumber(new Random().nextInt(4));
    }

    private long getRandomQuantity() {
        return 50L + (long) (Math.random() * (100 - 50L));
    }

    private double getRandomPrice() {
        return Double.parseDouble(new DecimalFormat("#0.00").format(1 + (20 - 1) * new Random().nextDouble()));
    }

}
