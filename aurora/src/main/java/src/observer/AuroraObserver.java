package src.observer;

import com.aurora.Aurora;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuroraObserver implements StreamObserver<Aurora.AuroraResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuroraObserver.class);

    private Aurora.AuroraRequest request;
    private StreamObserver<Aurora.AuroraResponse> forwardResponse;

    public AuroraObserver(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> forwardResponse) {
        this.request = request;
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
        forwardResponse.onError(throwable);
    }

    @Override
    public void onCompleted() {
        LOGGER.info("Completing stream request for client {}", request.getClientId());
        forwardResponse.onCompleted();
    }
}
