package de.kaffeekrone.maildrop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@SpringBootApplication
public class MailDropApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailDropApplication.class, args);
    }

}
