package de.kaffeekrone.maildrop;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="maildrop")
public class MailDropConfiguration {

    
    @Getter
    @Value("${exchange:mail-drop}")
    private String exchange;
    
    @Getter
    @Value("${routingKey:mail-drop}")
    private String routingKey; 
    
    @Getter
    @Value("${queue:mail-drop}")
    private String queue;
    
    
    @Getter
    @Value("${retryExchange:retry-mail-drop}")
    private String retryExchange;

    @Getter
    @Value("${retryRoutingKey:retry-mail-drop}")
    private String retryRoutingKey;  
    
    @Getter
    @Value("${retryQueue:retry-mail-drop}")
    private String retryQueue;


    @Getter
    @Value("${callbackExchange:callback-mail-drop}")
    private String callbackExchange;

    @Getter
    @Value("${callbackRoutingKey:callback-mail-drop}")
    private String callbackRoutingKey;

    @Getter
    @Value("${callbackQueue:callback-mail-drop}")
    private String callbackQueue;


    @Getter
    @Value("${retryAttempts:5}")
    private int retryAttempts;

    @Getter
    @Value("${retryDelay:5}")
    private int retryDelay;

    @Getter
    @Value("${defaultFromAddress:root@localhost}")
    private String defaultFromAddress;


}
