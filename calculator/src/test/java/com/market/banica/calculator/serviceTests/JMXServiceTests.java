package com.market.banica.calculator.serviceTests;

import com.market.banica.calculator.service.JMXServiceImpl;
import com.market.banica.calculator.service.contract.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class JMXServiceTests {

    @Mock
    private ProductService productService;

    @InjectMocks
    private JMXServiceImpl jmxService;

    @Test
    public void createProduct_Should_createProduct_When_parametersAreValidAndProductIsNotComposite() {
        //given
        doNothing().when(productService).createProduct("name", "unit","");

        //when
        jmxService.createProduct("name", "unit","");

        //then
        verify(productService, times(1))
                .createProduct("name", "unit","");
        verifyNoMoreInteractions(productService);

    }
}
