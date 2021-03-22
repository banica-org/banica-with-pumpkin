package com.market.banica.aurora;

import com.aurora.Aurora;
import com.market.banica.aurora.service.AuroraService;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class Main implements CommandLineRunner {
    private final AuroraService auroraService;

    @Autowired
    public Main(AuroraService auroraService) {
        this.auroraService = auroraService;
    }


    @Override
    public void run(String... args) throws Exception {
        Aurora.AuroraRequest asiaBuilder = Aurora.AuroraRequest.newBuilder().setClientId("1").setTopic("asia/eggs").build();
        Aurora.AuroraRequest europeBuilder = Aurora.AuroraRequest.newBuilder().setClientId("1").setTopic("europe/eggs").build();
        Aurora.AuroraRequest americaBuilder = Aurora.AuroraRequest.newBuilder().setClientId("1").setTopic("america/eggs").build();
        Aurora.AuroraRequest test = Aurora.AuroraRequest.newBuilder().setClientId("1").setTopic("*/*").build();

        Scanner scanner = new Scanner(System.in);
        int line = Integer.parseInt(scanner.nextLine());
        while (line != 0) {
            int[] count = new int[1];
//            if (line == 1) {
//                auroraService.subscribe(asiaBuilder, new StreamObserver<Aurora.AuroraResponse>() {
//                    @Override
//                    public void onNext(Aurora.AuroraResponse auroraResponse) {
//                        System.out.println(auroraResponse.toString());
//                        count[0]++;
//                    }
//
//                    @Override
//                    public void onError(Throwable throwable) {
//
//                    }
//
//                    @Override
//                    public void onCompleted() {
//
//                    }
//                });
//            } else if (line == 2) {
//                auroraService.subscribe(europeBuilder, new StreamObserver<Aurora.AuroraResponse>() {
//                    @Override
//                    public void onNext(Aurora.AuroraResponse auroraResponse) {
//                        System.out.println(auroraResponse.toString());
//                        count[0]++;
//                    }
//
//                    @Override
//                    public void onError(Throwable throwable) {
//
//                    }
//
//                    @Override
//                    public void onCompleted() {
//
//                    }
//                });
//            } else if (line == 3) {
//                auroraService.subscribe(americaBuilder, new StreamObserver<Aurora.AuroraResponse>() {
//                    @Override
//                    public void onNext(Aurora.AuroraResponse auroraResponse) {
//                        System.out.println(auroraResponse.toString());
//                        count[0]++;
//                    }
//
//                    @Override
//                    public void onError(Throwable throwable) {
//
//                    }
//
//                    @Override
//                    public void onCompleted() {
//
//                    }
//                });
//            }

            auroraService.subscribe(test, new StreamObserver<Aurora.AuroraResponse>() {
                @Override
                public void onNext(Aurora.AuroraResponse auroraResponse) {
                    System.out.println(auroraResponse.toString());
                    count[0]++;
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onCompleted() {

                }
            });



            try {
                Thread.sleep(900);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(count[0]);
            line = Integer.parseInt(scanner.nextLine());
        }
    }
}
