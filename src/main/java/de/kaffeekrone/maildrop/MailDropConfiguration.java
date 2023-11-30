package de.kaffeekrone.maildrop;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "maildrop")
public class MailDropConfiguration {


    @Getter
    @Setter
    @Value("${exchange:mail-drop}")
    private String exchange;

    @Getter
    @Setter
    @Value("${routingKey:mail-drop}")
    private String routingKey;

    @Getter
    @Setter
    @Value("${queue:mail-drop}")
    private String queue;


    @Getter
    @Setter
    @Value("${retryExchange:retry-mail-drop}")
    private String retryExchange;

    @Getter
    @Setter
    @Value("${retryRoutingKey:retry-mail-drop}")
    private String retryRoutingKey;

    @Getter
    @Setter
    @Value("${retryQueue:retry-mail-drop}")
    private String retryQueue;


    @Getter
    @Setter
    @Value("${callbackExchange:callback-mail-drop}")
    private String callbackExchange;

    @Getter
    @Setter
    @Value("${callbackRoutingKey:callback-mail-drop}")
    private String callbackRoutingKey;

    @Getter
    @Setter
    @Value("${callbackQueue:callback-mail-drop}")
    private String callbackQueue;


    @Getter
    @Setter
    @Value("${retryAttempts:5}")
    private int retryAttempts;

    @Getter
    @Setter
    @Value("${retryDelay:5}")
    private int retryDelay;

    @Getter
    @Setter
    @Value("${defaultFromAddress:root@localhost}")
    private String defaultFromAddress;

    @Getter
    @Setter
    @Value("${enableCallback:true}")
    private boolean enableCallback;


}
