package src.observer;

import com.aurora.Aurora;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;


public class AuroraObserver implements StreamObserver<Aurora.AuroraResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraObserver.class);

    private final CountDownLatch latch;

    private final Aurora.AuroraRequest request;
    private final StreamObserver<Aurora.AuroraResponse> forwardResponse;

    public AuroraObserver(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> forwardResponse, CountDownLatch latch) {
        this.request = request;
        this.latch = latch;
        this.forwardResponse = forwardResponse;
    }

    @Override
    public void onNext(Aurora.AuroraResponse response) {
        LOGGER.debug("Forwarding response to client {}", request.getClientId());
        forwardResponse.onNext(response);
    }


    @Override
    public void onError(Throwable throwable) {
        LOGGER.warn("Unable to forward.");
        LOGGER.error(throwable.toString());
        latch.countDown();
    }

    @Override
    public void onCompleted() {
        LOGGER.info("Completing stream request for client {}", request.getClientId());
        latch.countDown();
    }
}
