package com.market.banica.calculator.componentTests.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TestConfigurationIT.class)
public class TestConfigurationBase {
}
