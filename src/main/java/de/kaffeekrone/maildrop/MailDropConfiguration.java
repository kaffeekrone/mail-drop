package de.kaffeekrone.maildrop;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "maildrop")
public class MailDropConfiguration {

    @Value("${exchange:mail-drop}")
    private String exchange;

    @Value("${routingKey:mail-drop}")
    private String routingKey;

    @Value("${queue:mail-drop}")
    private String queue;


    @Value("${retryExchange:retry-mail-drop}")
    private String retryExchange;

    @Value("${retryRoutingKey:retry-mail-drop}")
    private String retryRoutingKey;

    @Value("${retryQueue:retry-mail-drop}")
    private String retryQueue;

    @Value("${callbackExchange:callback-mail-drop}")
    private String callbackExchange;

    @Value("${callbackRoutingKey:callback-mail-drop}")
    private String callbackRoutingKey;

    @Value("${callbackQueue:callback-mail-drop}")
    private String callbackQueue;

    @Value("${retryAttempts:5}")
    private int retryAttempts;

    @Value("${retryDelay:5}")
    private int retryDelay;

    @Value("${defaultFromAddress:root@localhost}")
    private String defaultFromAddress;

    @Value("${enableCallback:true}")
    private boolean enableCallback;


}
