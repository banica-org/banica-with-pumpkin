package com.market.banica.order.book;


import com.market.banica.order.book.exception.TrackingException;
import com.market.banica.order.book.service.grpc.AuroraClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Scanner;

@EnableMBeanExport
@ManagedResource
@Service
@RequiredArgsConstructor
public class Jmx {
    private final AuroraClient auroraClient;


    @ManagedOperation
    public void startSubscribe(String itemName) throws TrackingException {
        this.auroraClient.startSubscription(itemName,"orderbook");
    }

    @ManagedOperation
    public void stopSubscribe(String itemName) throws TrackingException {
        this.auroraClient.stopSubscription(itemName,"orderbook");
    }

}
