package de.kaffeekrone.maildrop.conf;

import de.kaffeekrone.maildrop.MailDropConfiguration;
import de.kaffeekrone.maildrop.Receiver;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
public class RabbitMQConfiguration {

//    private static final String queueName = "mail-drop";
//    private static final String exchangeName = "mail-drop";
//    private static final String routingKey = "mail-drop";

//    private static final String retryQueueName = "retry-mail-drop";
//    private static final String retryExchangeName = "retry-mail-drop";
//    private static final String retryRoutingKey = "retry-mail-drop";

//    private static final String callbackExchange = "mail-drop-result";
//    private static final String callbackQueue = "mail-drop-result";
//    private static final String callbackRoutingKey = "mail-drop-result";

    @Bean("mailExchange")
    DirectExchange mailDropExchange(MailDropConfiguration mailDropConfiguration) {
        return new DirectExchange(mailDropConfiguration.getExchange(), true, false);
    }

    @Bean("mailQueue")
    Queue mailQueue(MailDropConfiguration mailDropConfiguration) {
        return new Queue(mailDropConfiguration.getQueue(), true);
    }

    @Bean
    Binding binding(MailDropConfiguration mailDropConfiguration,  @Qualifier("mailQueue") Queue queue, @Qualifier("mailExchange") DirectExchange directExchange) {
        return BindingBuilder.bind(queue).to(directExchange).with(mailDropConfiguration.getRoutingKey());
    }

    @Bean("retryExchange")
    DirectExchange retryMailDropExchange(MailDropConfiguration mailDropConfiguration) {
        return new DirectExchange(mailDropConfiguration.getRetryExchange(), true, false);
    }


    @Bean("retryQueue")
    Queue retryMailQueue(MailDropConfiguration mailDropConfiguration) {
        return new Queue(mailDropConfiguration.getRetryQueue(), true, false, false, Map.of("x-dead-letter-exchange", mailDropConfiguration.getExchange(),
                "x-dead-letter-routing-key", mailDropConfiguration.getRoutingKey(),
                "x-message-ttl", TimeUnit.SECONDS.toMillis(mailDropConfiguration.getRetryDelay())));
    }

    @Bean
    Binding retryBinding(MailDropConfiguration mailDropConfiguration, @Qualifier("retryQueue") Queue queue, @Qualifier("retryExchange") DirectExchange directExchange) {
        return BindingBuilder.bind(queue).to(directExchange).with(mailDropConfiguration.getRetryRoutingKey());
    }


//    callback stuff
    @Bean("callbackExchange")
    DirectExchange callbackExchange(MailDropConfiguration mailDropConfiguration) {
        return new DirectExchange(mailDropConfiguration.getCallbackExchange(), true, false);
    }

    @Bean("callbackQueue")
    Queue callbackQueue(MailDropConfiguration mailDropConfiguration) {
        return new Queue(mailDropConfiguration.getCallbackQueue(), true);
    }

    @Bean
    Binding callbackBinding(MailDropConfiguration mailDropConfiguration, @Qualifier("callbackQueue") Queue queue, @Qualifier("callbackExchange") DirectExchange directExchange) {
        return BindingBuilder.bind(queue).to(directExchange).with(mailDropConfiguration.getCallbackRoutingKey());
    }

    @Bean
    SimpleMessageListenerContainer container(MailDropConfiguration mailDropConfiguration, ConnectionFactory connectionFactory,
                                             Receiver receiver) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(mailDropConfiguration.getQueue());
        container.setMessageListener(receiver);
        return container;
    }
}
