package com.market.banica.order.book;

import com.market.banica.order.book.client.AuroraClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class Main implements CommandLineRunner {
    private final AuroraClient auroraClient;

    @Autowired
    public Main(AuroraClient auroraClient) {
        this.auroraClient = auroraClient;
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        int i = Integer.parseInt(scanner.nextLine());
        while (i != 0) {
            if (i == 1) {
                auroraClient.subscribe("asia", "eggs");
            } else if (i == 2) {
                auroraClient.subscribe("europe", "eggs");
            } else if (i == 3) {
                auroraClient.subscribe("america", "eggs");
            }
            i = Integer.parseInt(scanner.nextLine());
        }
    }
}
