package de.kaffeekrone.maildrop.conf;

import de.kaffeekrone.maildrop.MailDropConfiguration;
import de.kaffeekrone.maildrop.MailConsumer;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
public class RabbitMQConfiguration {

    @Bean("mailExchange")
    DirectExchange mailDropExchange(MailDropConfiguration mailDropConfiguration) {
        return new DirectExchange(mailDropConfiguration.getExchange(), true, false);
    }

    @Bean("mailQueue")
    Queue mailQueue(MailDropConfiguration mailDropConfiguration) {
        return new Queue(mailDropConfiguration.getQueue(), true);
    }

    @Bean
    Binding binding(MailDropConfiguration mailDropConfiguration, @Qualifier("mailQueue") Queue queue, @Qualifier("mailExchange") DirectExchange directExchange) {
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

    @Bean("mailConsumerContainer")
    SimpleMessageListenerContainer container(MailDropConfiguration mailDropConfiguration, ConnectionFactory connectionFactory,
                                             MailConsumer mailConsumer) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(mailDropConfiguration.getQueue());
        container.setMessageListener(mailConsumer);
        return container;
    }
}
