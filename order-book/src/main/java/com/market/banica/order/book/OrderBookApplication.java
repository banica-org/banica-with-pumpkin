package com.market.banica.order.book;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class OrderBookApplication {

    public static void main(String[] args) {
        System.out.println("Hello Orderbook");
        SpringApplication.run(OrderBookApplication.class, args);
    }
}
