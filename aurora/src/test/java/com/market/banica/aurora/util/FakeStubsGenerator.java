package com.market.banica.aurora.util;

import com.aurora.Aurora;
import com.aurora.AuroraServiceGrpc;
import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;


import java.io.IOException;

public class FakeStubsGenerator {

    public void createFakeAuroraReplyingServer(String receiverServerName, GrpcCleanupRule grpcCleanup, ManagedChannel auroraReceiverChannel) throws IOException {
        grpcCleanup.register(InProcessServerBuilder
                .forName(receiverServerName)
                .directExecutor()
                .addService(this.fakeReceivingAuroraService())
                .build()
                .start());

        grpcCleanup.register(auroraReceiverChannel);
        auroraReceiverChannel.getState(true);
    }

    private AuroraServiceGrpc.AuroraServiceImplBase fakeReceivingAuroraService() {
        return new AuroraServiceGrpc.AuroraServiceImplBase() {
            @Override
            public void request(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                Aurora.AuroraResponse response = Aurora.AuroraResponse
                        .newBuilder()
                        .setMessage(Any.pack(request))
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }

            @Override
            public void subscribe(Aurora.AuroraRequest request, StreamObserver<Aurora.AuroraResponse> responseObserver) {
                for (int i = 0; i < 3; i++) {
                    Aurora.AuroraResponse response = Aurora.AuroraResponse
                            .newBuilder()
                            .setMessage(Any.pack(request))
                            .build();

                    responseObserver.onNext(response);
                }

                responseObserver.onCompleted();
            }
        };
    }
}
