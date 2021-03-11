package com.market.banica.calculator.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Properties;

@Component
@Getter
@Setter
public class ReceiptsBase {

    private Properties database = new Properties();

    @PostConstruct
    public void readBackUp(){
        try(InputStream input = this.getClass().getClassLoader().getResourceAsStream(getClass().getSimpleName() + ".properties")) {

            if(input == null){
                return;
            }
            Properties props = new Properties();
            props.load(input);
            setDatabase(props);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void writeBackUp(){
        try(OutputStream output = new FileOutputStream("c:/user/"+ getClass().getSimpleName() + LocalDateTime.now() +".properties")) {

            database.store(output, "back-up for " + LocalDateTime.now());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
