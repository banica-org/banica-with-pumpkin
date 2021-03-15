package com.market.banica.order.book;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderBookApplication {

    public static void main(String[] args) {
        Logger LOGGER = LogManager.getLogger(MarketDataClient.class);
        SpringApplication.run(OrderBookApplication.class, args);
        LOGGER.info("Products data updated!");
    }

}
