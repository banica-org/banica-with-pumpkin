package com.market.banica.aurora.service;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.banica.common.ChannelRPCConfig;
import com.orderbook.ItemOrderBookRequest;
import com.orderbook.ItemOrderBookResponse;
import com.orderbook.OrderBookServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class AuroraServiceImpl extends AuroraServiceGrpc.AuroraServiceImplBase {

    private final ManagedChannel managedChannel;
    private static final Logger LOGGER = LogManager.getLogger(AuroraServiceImpl.class);
    ManagedChannel orderBookChannel;

    @Autowired
    AuroraServiceImpl(@Value("${aurora.server.host}") final String host,
                      @Value("${orderbook.server.port}") final int port) {

        LOGGER.info("Setting up connection with Orderbook.");

        managedChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .defaultServiceConfig(ChannelRPCConfig.getInstance().getServiceConfig())
                .enableRetry()
                .build();

    }

    @Override
    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.info("Received request from client {}.",request.getClientId());

        if (request.getTopic().contains("order-book")){
            OrderBookServiceGrpc.OrderBookServiceBlockingStub stub = OrderBookServiceGrpc.newBlockingStub(this.managedChannel);

            Pair<String, Long> pair = this.harvestData(request);

            ItemOrderBookResponse orderBookResponse = stub.getOrderBookItemLayers(ItemOrderBookRequest.newBuilder()
                    .setClientId(request.getClientId())
                    .setItemName(pair.getKey())
                    .setQuantity(pair.getValue())
                    .build());

            responseObserver.onNext(Aurora.AuroraResponse.newBuilder().
                    setItemOrderBookResponse(orderBookResponse).
                    build());

            responseObserver.onCompleted();
        }

        super.request(request, responseObserver);
    }

    private Pair<String,Long> harvestData(Aurora.AuroraRequest request){
        String topic = request.getTopic();

        String[] split = topic.split("/");

        String productName = split[1];

        Long quantity = Long.parseLong(split[2]);

        return new Pair<>(productName,quantity);
    }

    @Override
    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        super.subscribe(request, responseObserver);
    }
}
