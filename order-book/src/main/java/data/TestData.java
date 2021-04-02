package data;

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

        orderBookLayers.add(generateOrderBookLayer(price, quantity));

        return orderBookLayers;
    }

    private OrderBookLayer generateOrderBookLayer(double price, long quantity) {
        return OrderBookLayer.newBuilder().setPrice(price).setQuantity(quantity).setOrigin(getRandomOrigin()).build();
    }

    private Origin getRandomOrigin() {
        return Origin.forNumber(new Random().nextInt(3) + 1);
    }


}
