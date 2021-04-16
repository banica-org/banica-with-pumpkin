package com.market.banica.calculator.service;

import com.market.Origin;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookLayer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
public class TestData {

    public Map<String, ItemOrderBookResponse> getTestData() {
        Map<String, ItemOrderBookResponse> data = new LinkedHashMap<>();
        addProductToDatabase(data, "banica", 2000000, 2);//pumpkin:450,milk:400,crusts:700,sauce:997725=998775*2=1997550
        addProductToDatabase(data, "pumpkin", 1.5, 300);
        addProductToDatabase(data, "milk", 2, 200);
        addProductToDatabase(data, "crusts", 3.9, 200);//water:0,5,eggs:3=3.5 * 200 = 700
        addProductToDatabase(data, "water", 0.01, 300);
        addProductToDatabase(data, "eggs", 0.25, 12);
        addProductToDatabase(data, "sauce", 7000, 150);//ketchup:6550,water:1.5,sugar:100 =6651,5 * 150=997725
        addProductToDatabase(data, "sugar", 2, 50);
        addProductToDatabase(data, "ketchup", 132, 50);//tomatoes:130,water:1 = 131 * 50 = 6550
        addProductToDatabase(data, "tomatoes", 2, 65);
        return data;
    }

    public Map<String, ItemOrderBookResponse> getTestData1() {
        Map<String, ItemOrderBookResponse> data = new LinkedHashMap<>();
        addProductToDatabase(data, "banica", 1000000, 2);//pumpkin:450,milk:400,crusts:700,sauce:997725=998775*2=1997550
        addProductToDatabase(data, "pumpkin", 1.5, 300);
        addProductToDatabase(data, "milk", 1.5, 100);
        addProductToDatabase(data, "milk", 2, 100);
        addProductToDatabase(data, "crusts", 3.9, 200);//water:0,5,eggs:3=3.5 * 200 = 700
        addProductToDatabase(data, "water", 0.01, 100);
        addProductToDatabase(data, "water", 0.02, 150);
        addProductToDatabase(data, "water", 0.03, 50);
        addProductToDatabase(data, "eggs", 0.15, 4);
        addProductToDatabase(data, "eggs", 0.25, 8);
        addProductToDatabase(data, "sauce", 7000, 150);//ketchup:6550,water:1.5,sugar:100 =6651,5 * 150=997725
        addProductToDatabase(data, "sugar", 2, 50);
        addProductToDatabase(data, "ketchup", 130, 50);//tomatoes:130,water:1 = 131 * 50 = 6550
        addProductToDatabase(data, "tomatoes", 1.5, 1);
        addProductToDatabase(data, "tomatoes", 2, 64);
        return data;
    }

    private void addProductToDatabase(Map<String, ItemOrderBookResponse> data, String productName, double price, int quantity) {

        ItemOrderBookResponse item = generateItoOrderBookResponse(productName, price, quantity);

        data.merge(productName, item, (a, b) -> {

           OrderBookLayer  orderBookLayerB = b.getOrderbookLayersList().get(0);

            return ItemOrderBookResponse.newBuilder()
                    .setItemName(productName)
                    .addAllOrderbookLayers(a.getOrderbookLayersList())
                    .addOrderbookLayers(orderBookLayerB)
                    .build();
        });

    }

    private ItemOrderBookResponse generateItoOrderBookResponse(String productName, double price, int quantity) {

        List<OrderBookLayer> list = new ArrayList<>();
        list.add(generateOrderBookLayer(productName, price, quantity));

        return ItemOrderBookResponse
                .newBuilder()
                .setItemName(productName)
                .addAllOrderbookLayers(list)
                .build();

    }

    private OrderBookLayer generateOrderBookLayer(String productName, double price, long quantity) {

        return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();

    }

    private Origin getRandomOrigin() {
        return Origin.forNumber(new Random().nextInt(4));
    }

}
