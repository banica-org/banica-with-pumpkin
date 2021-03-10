package com.market.banica.calculator.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Configuration
@ConfigurationProperties(prefix = "banica.pumpkin")
@PropertySource("classpath:banica.properties")
@EnableMBeanExport
@ManagedResource
@Getter
@Setter(onMethod_ ={@ManagedOperation} )
public class BanicaPumpkinProps {

    @Min(value = 5)
    @Max(value = 20)
    private int eggCount;

    @Min(value = 1)
    @Max(value = 3)
    private int crustsCount;

    @Min(value = 100)
    @Max(value = 300)
    private volatile double pumpkinGrams;
}
