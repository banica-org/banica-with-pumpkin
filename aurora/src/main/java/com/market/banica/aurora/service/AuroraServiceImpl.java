package com.market.banica.aurora.service;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.market.AvailabilityResponse;
import com.market.BuySellProductResponse;
import com.market.MarketServiceGrpc;
import com.market.ProductBuyRequest;
import com.market.ProductSellRequest;
import com.market.banica.aurora.config.ChannelManager;
import com.market.banica.aurora.handlers.SubscribeHandler;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.market.banica.aurora.handlers.RequestHandler;

import java.util.Locale;
import java.util.Optional;


@Service
public class AuroraServiceImpl extends AuroraServiceGrpc.AuroraServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraServiceImpl.class);

    private final RequestHandler requestHandler;

    private final SubscribeHandler subscribeHandler;
    private final ChannelManager channelManager;

    @Autowired
    public AuroraServiceImpl(RequestHandler requestHandler, SubscribeHandler subscribeHandler, ChannelManager channelManager) {
        this.requestHandler = requestHandler;
        this.subscribeHandler = subscribeHandler;
        this.channelManager = channelManager;
    }

    @Override
    public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.info("Accepted request from client {}", request.getClientId());
        requestHandler.handleRequest(request, responseObserver);
    }

    @Override
    public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.info("Accepted subscribe from client {}", request.getClientId());
        subscribeHandler.handleSubscribe(request, responseObserver);
    }

    @Override
    public void buyProduct(ProductBuyRequest request, StreamObserver<BuySellProductResponse> responseObserver) {
        Optional<ManagedChannel> channelByKey = channelManager.getChannelByKey("market-" + request.getOrigin().toString().toLowerCase(Locale.ROOT));
        MarketServiceGrpc.MarketServiceBlockingStub stub = MarketServiceGrpc.newBlockingStub(channelByKey.get());
        BuySellProductResponse buySellProductResponse = stub.buyProduct(request);
        responseObserver.onNext(buySellProductResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void checkAvailability(ProductBuyRequest request, StreamObserver<AvailabilityResponse> responseObserver) {
        Optional<ManagedChannel> channelByKey = channelManager.getChannelByKey("market-" + request.getOrigin().toString().toLowerCase(Locale.ROOT));
        MarketServiceGrpc.MarketServiceBlockingStub stub = MarketServiceGrpc.newBlockingStub(channelByKey.get());
        AvailabilityResponse availabilityResponse = stub.checkAvailability(request);
        responseObserver.onNext(availabilityResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void sellProduct(ProductSellRequest request, StreamObserver<BuySellProductResponse> responseObserver) {
        Optional<ManagedChannel> channelByKey = channelManager.getChannelByKey("market-" + request.getMarketName().toLowerCase(Locale.ROOT));
        MarketServiceGrpc.MarketServiceBlockingStub stub = MarketServiceGrpc.newBlockingStub(channelByKey.get());
        BuySellProductResponse availabilityResponse = stub.sellProduct(request);
        responseObserver.onNext(availabilityResponse);
        responseObserver.onCompleted();
    }
}
