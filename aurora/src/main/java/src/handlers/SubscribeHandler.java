package src.handlers;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import src.config.ChannelManager;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
        Optional<ManagedChannel> channelByKey = channels.getChannelByKey(request.getTopic().split("/")[0]);
        if (channelByKey.isPresent()) {
            AuroraServiceGrpc.AuroraServiceStub stub = generateAuroraStub(channelByKey.get());

            startStream(request, stub, responseObserver);
        } else {
            LOGGER.warn("Unsupported message have reached aurora.");
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(request.getTopic().split("/")[0] + " channel not available at the moment.")
                    .asException());
        }
    }

    private void startStream(Aurora.AuroraRequest request, AuroraServiceGrpc.AuroraServiceStub asynchronousStub,
                             StreamObserver<Aurora.AuroraResponse> responseObserver) {
        asynchronousStub.subscribe(request, new StreamObserver<Aurora.AuroraResponse>() {
            @Override
            public void onNext(Aurora.AuroraResponse response) {
                LOGGER.debug("Forwarding response to client {}", request.getClientId());
                responseObserver.onNext(response);
            }

            @Override
            public void onError(final Throwable throwable) {
                LOGGER.warn("Unable to forward.");
                LOGGER.error(throwable.toString());
                responseObserver.onError(throwable);

            }

            @Override
            public void onCompleted() {
                LOGGER.info("Completing stream request for client {}", request.getClientId());
                responseObserver.onCompleted();
            }

        });
    }

    private AuroraServiceGrpc.AuroraServiceStub generateAuroraStub(ManagedChannel channelByKey) {
        LOGGER.debug("Generating stub.");
        return AuroraServiceGrpc.newStub(channelByKey);
    }
}