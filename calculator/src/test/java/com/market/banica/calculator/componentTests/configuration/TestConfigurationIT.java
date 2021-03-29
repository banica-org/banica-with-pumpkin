package com.market.banica.calculator.componentTests.configuration;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

@Configuration
@Profile(value = "testIT")
public class TestConfigurationIT {

    private final String serverName = InProcessServerBuilder.generateName();

    public String getServerName() {

        return serverName;
    }

    public AuroraServiceGrpc.AuroraServiceBlockingStub getBlockingStub() {

        return AuroraServiceGrpc.newBlockingStub(getChannel());
    }

    public ManagedChannel getChannel() {

        return InProcessChannelBuilder.forName(serverName).build();
    }

    public Server startInProcessService(AuroraServiceGrpc.AuroraServiceImplBase service) throws IOException {

        return InProcessServerBuilder.forName(serverName).directExecutor()
                .addService(service).build().start();
    }

    public AuroraServiceGrpc.AuroraServiceImplBase getMockGrpcService(Aurora.AuroraResponse auroraResponse) {

        return mock(AuroraServiceGrpc.AuroraServiceImplBase.class, delegatesTo(new AuroraServiceGrpc.AuroraServiceImplBase() {
            @Override
            public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

                responseObserver.onNext(auroraResponse);

                responseObserver.onCompleted();
            }
        }));
    }

    public AuroraServiceGrpc.AuroraServiceImplBase getGrpcService(Aurora.AuroraResponse response) {

        return new AuroraServiceGrpc.AuroraServiceImplBase() {
            @Override
            public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {

                responseObserver.onNext(response);

                responseObserver.onCompleted();
            }
        };
    }

    public AuroraServiceGrpc.AuroraServiceImplBase getEmptyGrpcService() {

        return new AuroraServiceGrpc.AuroraServiceImplBase() {
        };
    }
}