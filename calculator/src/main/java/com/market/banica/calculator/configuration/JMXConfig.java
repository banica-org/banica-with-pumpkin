package com.market.banica.calculator.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.ResourceBundle;

@Configuration
@EnableMBeanExport
@ManagedResource
public class JMXConfig {

    @ManagedOperation
    public void setValue(String fileName, String ingredientName, String newValue) {

        try(InputStream input = this.getClass().getClassLoader().getResourceAsStream(fileName + ".properties");
                OutputStream output = new FileOutputStream("src/main/resources/" + fileName + ".properties")) {

            Properties props = new Properties();
            props.load(input);
            props.setProperty(ingredientName,newValue);
            props.store(output, "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ManagedOperation
    public String getValue(String fileName ,String ingredientName) {

       ResourceBundle resource = ResourceBundle.getBundle(fileName);
        return resource.getString(ingredientName);

    }


}
