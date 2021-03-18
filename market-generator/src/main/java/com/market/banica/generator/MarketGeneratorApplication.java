package com.market.banica.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableMBeanExport;

@SpringBootApplication
@EnableMBeanExport
public class MarketGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketGeneratorApplication.class, args);
    }
}
