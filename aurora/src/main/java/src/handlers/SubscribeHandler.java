package src.handlers;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import src.config.ChannelManager;
import src.observer.AuroraObserver;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@Service
public class SubscribeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscribeHandler.class);

    private final ChannelManager channels;

    @Autowired
    public SubscribeHandler(ChannelManager channelManager) {
        channels = channelManager;
    }

    public void handleSubscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
        LOGGER.info("Handling subscribe from client {}", request.getClientId());
        List<ManagedChannel> channelsWithPrefix = this.channels.getAllChannelsContainingPrefix(request.getTopic().split("/")[0]);
        if (channelsWithPrefix.isEmpty()) {
            LOGGER.warn("Unsupported message have reached aurora.");
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(request.getTopic().split("/")[0] + " channel not available at the moment.")
                    .asException());
            return;
        }
        CountDownLatch latch = new CountDownLatch(channelsWithPrefix.size());

        channelsWithPrefix.forEach(channel -> this.generateAuroraStub(channel)
                .subscribe(request, new AuroraObserver(request, responseObserver, latch)));

        try {
            latch.await();
            LOGGER.info("Completing streams for client {}", request.getClientId());
            responseObserver.onCompleted();
        } catch (InterruptedException e) {
            LOGGER.warn("Latch have been interrupted completing streams for client {}", request.getClientId());
            LOGGER.error(e.getMessage());
            responseObserver.onError(e);
        }
    }

    private AuroraServiceGrpc.AuroraServiceStub generateAuroraStub(ManagedChannel channelByKey) {
        LOGGER.debug("Generating stub.");
        return AuroraServiceGrpc.newStub(channelByKey);
    }
}