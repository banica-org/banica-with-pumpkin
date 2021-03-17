package com.market.banica.common;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.InputStreamReader;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChannelRPCConfig {

    private ChannelRPCConfig() {
    }

    public static Map<String, ?> getRetryingServiceConfig() {
        return new Gson().fromJson(
                new JsonReader(
                        new InputStreamReader(
                                ChannelRPCConfig.class.getResourceAsStream(
                                        "retrying_service_config.json"),
                                UTF_8)),
                Map.class);
    }


}
