package com.market.banica.calculator.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

/**
 * Date: 3/10/2021 Time: 7:58 AM
 * <p>
 * <p>
 * Config class for swagger2
 *
 * @author Vladislav_Zlatanov
 */

@EnableSwagger2
@Configuration
public class SwaggerConfig {

    @Bean
    public Docket swaggerDocketConfig() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.market.banica.calculator"))
                .build()
                .apiInfo(this.apiDetails());
    }

    private ApiInfo apiDetails() {
        return new ApiInfo("Banica-with-pumpkin API",
                "API for recipes",
                "1-0-SNAPSHOT",
                "Training purpose only",
                new springfox.documentation.service.Contact("Epam", "https://www.epam.com/", ""),
                "API license",
                "https://www.epam.com/",
                Collections.emptyList());
    }
}
