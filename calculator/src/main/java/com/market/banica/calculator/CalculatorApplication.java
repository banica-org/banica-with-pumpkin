package com.market.banica.calculator;

import com.market.banica.calculator.configuration.BanicaPumpkinProps;
import com.market.banica.calculator.service.BanicaPumpkinPropsConfigImpl;
import com.market.banica.calculator.service.contract.BanicaPumpkinPropsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class CalculatorApplication {


    public static void main(String[] args) {
        ApplicationContext applicationContext = SpringApplication.run(CalculatorApplication.class, args);

        BanicaPumpkinProps test = (BanicaPumpkinProps) applicationContext.getBean("banicaPumpkinProps");

        System.out.println(test.getEggCount());

        BanicaPumpkinPropsConfig test1 = new BanicaPumpkinPropsConfigImpl(test);
        BanicaPumpkinProps newTest = new BanicaPumpkinProps();
        newTest.setEggCount(1);
        test1.createReceipt(newTest);

        System.out.println(test.getEggCount());

        try{
            Thread.sleep(30000);
        }catch (Exception ignored){}

        System.out.println(test.getEggCount());
    }
}
