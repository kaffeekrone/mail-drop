package de.kaffeekrone.maildrop;

import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

@Component("consumer")
public class MailConsumerContainerHealthIndicator extends AbstractHealthIndicator {

    private final SimpleMessageListenerContainer simpleMessageListenerContainer;

    public MailConsumerContainerHealthIndicator(@Qualifier("mailConsumerContainer") SimpleMessageListenerContainer simpleMessageListenerContainer) {
        this.simpleMessageListenerContainer = simpleMessageListenerContainer;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean isRunning = simpleMessageListenerContainer.isRunning();
        if (isRunning) {
            builder.up()
                    .withDetail("isRunning", isRunning)
                    .withDetail("isActive", simpleMessageListenerContainer.isActive())
                    .withDetail("activeConsumerCount", simpleMessageListenerContainer.getActiveConsumerCount());
        } else {
            builder.down()
                    .withDetail("isRunning", isRunning)
                    .withDetail("isActive", simpleMessageListenerContainer.isActive());
        }
    }
}
