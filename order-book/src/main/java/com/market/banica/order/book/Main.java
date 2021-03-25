package com.market.banica.order.book;

import com.market.banica.order.book.client.testAurora;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class Main implements CommandLineRunner {
    private final testAurora testAurora;

    @Autowired
    public Main(testAurora testAurora) {
        this.testAurora = testAurora;
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        int i = Integer.parseInt(scanner.nextLine());
        while (i != 0) {
            if (i == 1) {
                testAurora.subscribe("asia", "eggs");
            } else if (i == 2) {
                testAurora.subscribe("europe", "eggs");
            } else if (i == 3) {
                testAurora.subscribe("america", "eggs");
            }
            i = Integer.parseInt(scanner.nextLine());
        }
    }
}
