package com.market.banica.aurora.config;


import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractStub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

@Configuration
public class StubManager {

    private GrpcClassProvider grpcClassProvider;

    @Autowired
    public StubManager(GrpcClassProvider grpcClassProvider) {
        this.grpcClassProvider = grpcClassProvider;
    }

    public AbstractBlockingStub<? extends AbstractBlockingStub<?>> getBlockingStub(Channel channel, String prefix) {

        Optional<Class<?>> grpcServiceClass = grpcClassProvider.getClass(prefix);

        if (grpcServiceClass.isPresent()) {
            try {
                Class<?> grpsClass = grpcServiceClass.get();
                Method stubMethod = grpsClass.getDeclaredMethod("newBlockingStub", Channel.class);
                Object stub = stubMethod.invoke(grpsClass, channel);

                return (AbstractBlockingStub<? extends AbstractBlockingStub<?>>) stub;

            } catch (NoSuchMethodException e) {
                //log it and rethrow it
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        } else {
            throw new RuntimeException("No supported stub. prefix of stub : " + prefix);
        }

        throw new RuntimeException("No supported stub.");
    }

    public AbstractStub<? extends AbstractStub<?>> getStub(Channel channel, String prefix) {

        Optional<Class<?>> grpcServiceClass = grpcClassProvider.getClass(prefix);

        if (grpcServiceClass.isPresent()) {
            try {
                Class<?> grpsClass = grpcServiceClass.get();
                Method stubMethod = grpsClass.getDeclaredMethod("newStub", Channel.class);
                Object stub = stubMethod.invoke(grpsClass, channel);

                return (AbstractStub<? extends AbstractStub<?>>) stub;

            } catch (NoSuchMethodException e) {
                //log it and rethrow it
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        } else {
            throw new RuntimeException("No supported stub. prefix of stub : " + prefix);
        }


        throw new RuntimeException("No supported stub.");
    }
}
