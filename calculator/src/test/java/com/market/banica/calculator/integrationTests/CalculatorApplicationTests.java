package com.market.banica.calculator.integrationTests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("testIT")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CalculatorApplicationTests {

    @Test
    void contextLoads() {
    }
}
