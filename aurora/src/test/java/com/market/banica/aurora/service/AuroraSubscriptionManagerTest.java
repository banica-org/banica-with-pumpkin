package com.market.banica.aurora.service;

import com.aurora.Aurora;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class AuroraSubscriptionManagerTest {
    private static final Aurora.AuroraRequest AURORA_REQUEST = Aurora.AuroraRequest.newBuilder().setClientId("clientId").setTopic("*/Eggs").build();


}