package com.market.banica.calculator.service.grpc;

import com.aurora.Aurora;
import com.google.protobuf.Any;
import com.market.AvailabilityResponse;
import com.market.BuySellProductResponse;
import com.market.banica.calculator.componentTests.configuration.TestConfigurationIT;
import com.market.banica.common.exception.IncorrectResponseException;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuroraClientSideServiceTest {

    TestConfigurationIT configurationIT = new TestConfigurationIT();
    private AuroraClientSideService auroraClientSideService;

    @Before
    public void setUp() {
        this.auroraClientSideService = new AuroraClientSideService(configurationIT.getBlockingStub());
    }

    @Test
    public void unpackAndValidateResponseUnpacksAndReturnsValidResponseWithValidArguments() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //Arrange
        Method unpackAndValidateResponse = auroraClientSideService.getClass()
                .getDeclaredMethod("unpackAndValidateResponse", Aurora.AuroraResponse.class, Class.class);
        unpackAndValidateResponse.setAccessible(true);
        Aurora.AuroraResponse auroraResponse = Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(AvailabilityResponse.newBuilder().build()))
                .build();
        AvailabilityResponse availabilityResponse = AvailabilityResponse.newBuilder().build();

        //Act
        Object actualResponse = unpackAndValidateResponse
                .invoke(auroraClientSideService, auroraResponse, AvailabilityResponse.class);

        //Assert
        assertEquals(availabilityResponse, actualResponse);
    }

    @Test
    public void unpackAndValidateResponseInternallyThrowsIncorrectResponseExceptionWhenGivenAndExpectedReturnTypeDontMatch() throws NoSuchMethodException {
        //Arrange
        Method unpackAndValidateResponse = auroraClientSideService.getClass().getDeclaredMethod("unpackAndValidateResponse",
                Aurora.AuroraResponse.class, Class.class);
        unpackAndValidateResponse.setAccessible(true);
        Aurora.AuroraResponse auroraResponse = Aurora.AuroraResponse.newBuilder()
                .setMessage(Any.pack(AvailabilityResponse.newBuilder().build()))
                .build();
        try {

            //Act
            unpackAndValidateResponse.invoke(auroraClientSideService, auroraResponse, BuySellProductResponse.class);
        } catch (IllegalAccessException | InvocationTargetException e) {

            //Assert
            assertThat(e.getCause(), instanceOf(IncorrectResponseException.class));
        }
    }
}