package de.kaffeekrone.maildrop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MailConsumerTest {

    private static final String RETRY_EXCHANGE = "retry-exchange";
    private static final String RETRY_ROUTING_KEY = "retry-routing-key";
    private static final byte[] BODY = "{}".getBytes(StandardCharsets.UTF_8);

    private RabbitTemplate rabbitTemplate;
    private MailConsumer mailConsumer;

    @BeforeEach
    void setUp() {
        MailDropConfiguration configuration = new MailDropConfiguration();
        configuration.setRetryAttempts(5);
        configuration.setRetryExchange(RETRY_EXCHANGE);
        configuration.setRetryRoutingKey(RETRY_ROUTING_KEY);
        configuration.setEnableCallback(true);

        SendMailService sendMailService = mock(SendMailService.class);
        doThrow(new IllegalArgumentException("deterministic failure"))
                .when(sendMailService).send(any());

        rabbitTemplate = mock(RabbitTemplate.class);
        mailConsumer = new MailConsumer(configuration, sendMailService, rabbitTemplate);
    }

    @Test
    void failedMessageUsesTheConfiguredRetryBudget() {
        Message currentMessage = failedMessageWithoutRetryCount();

        for (int expectedRetryCount = 1; expectedRetryCount < 5; expectedRetryCount++) {
            clearInvocations(rabbitTemplate);
            mailConsumer.onMessage(currentMessage, null);

            org.mockito.ArgumentCaptor<Message> messageCaptor = org.mockito.ArgumentCaptor.forClass(Message.class);
            verify(rabbitTemplate).send(eq(RETRY_EXCHANGE), eq(RETRY_ROUTING_KEY), messageCaptor.capture());

            currentMessage = messageCaptor.getValue();
            assertThat(currentMessage.getBody()).containsExactly(BODY);
            assertThat((Object) currentMessage.getMessageProperties().getHeader("requeueCount"))
                    .isInstanceOf(Integer.class)
                    .isEqualTo(expectedRetryCount);
            assertThat((Object) currentMessage.getMessageProperties().getHeader("X-correlation-id"))
                    .isEqualTo("correlation-id");
            assertThat(currentMessage.getMessageProperties().getContentType()).isEqualTo("application/json");
            assertThat(currentMessage.getMessageProperties().getContentEncoding()).isEqualTo(StandardCharsets.UTF_8.name());
        }

        clearInvocations(rabbitTemplate);
        mailConsumer.onMessage(currentMessage, null);

        verify(rabbitTemplate, never()).send(eq(RETRY_EXCHANGE), eq(RETRY_ROUTING_KEY), any(Message.class));
    }

    @Test
    void invalidOrExhaustedRetryCountsAreNotRepublished() {
        List<Object> retryCounts = Arrays.asList(
                -1,
                Integer.MIN_VALUE,
                4,
                5,
                Integer.MAX_VALUE,
                1L,
                (short) 1,
                (byte) 1,
                "1",
                BigDecimal.ONE,
                1.0,
                true,
                List.of(1),
                Map.of("count", 1),
                new byte[]{1},
                null
        );

        for (Object retryCount : retryCounts) {
            clearInvocations(rabbitTemplate);
            Message message = failedMessageWithRetryCount(retryCount);

            assertThatCode(() -> mailConsumer.onMessage(message, null))
                    .as("retryCount %s", retryCount)
                    .doesNotThrowAnyException();
            verify(rabbitTemplate, never()).send(eq(RETRY_EXCHANGE), eq(RETRY_ROUTING_KEY), any(Message.class));
        }
    }

    private static Message failedMessageWithoutRetryCount() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("X-correlation-id", "correlation-id");
        return new Message(BODY, properties);
    }

    private static Message failedMessageWithRetryCount(Object retryCount) {
        MessageProperties properties = new MessageProperties();
        properties.getHeaders().put("requeueCount", retryCount);
        return new Message(BODY, properties);
    }
}
