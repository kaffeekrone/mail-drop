package de.kaffeekrone.maildrop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "maildrop.retry-delay=1", "maildrop.retry-attempts=3", "maildrop.enable-callback=true"
})
@Import(MailDeliveryIT.Services.class)
class MailDeliveryIT {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private MailDropConfiguration configuration;
    @Autowired
    private GreenMail greenMail;
    @MockitoSpyBean
    private SendMailService sendMailService;

    @BeforeEach
    void clearMail() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }

    @Test
    void deliversMailAndPublishesSuccessCallback() throws Exception {
        String id = publishMail();

        JsonObject callback = receiveCallback(id, true);
        assertThat(ZonedDateTime.parse(callback.get("sentDate").getAsString())).isNotNull();
        assertDeliveredMail();
        verify(sendMailService).send(any());
    }

    @Test
    void deliversMailWithoutCallbackWhenDisabled() throws Exception {
        configuration.setEnableCallback(false);
        try {
            publishMail();
            assertDeliveredMail();
            assertThat(rabbitTemplate.receive(configuration.getCallbackQueue(), 1500)).isNull();
            verify(sendMailService).send(any());
        } finally {
            configuration.setEnableCallback(true);
        }
    }

    @Test
    void retriesThroughTheBrokerThenDeliversMail() throws Exception {
        doThrow(new SendMailException("temporary SMTP failure", null))
                .doCallRealMethod().when(sendMailService).send(any());

        String id = publishMail();

        receiveCallback(id, true);
        assertDeliveredMail();
        verify(sendMailService, times(2)).send(any());
    }

    @Test
    void stopsAtTheAttemptLimitAndPublishesFailureCallback() {
        doThrow(new SendMailException("persistent SMTP failure", null))
                .when(sendMailService).send(any());

        String id = publishMail();

        JsonObject callback = receiveCallback(id, false);
        assertThat(callback.has("sentDate")).isFalse();
        verify(sendMailService, times(3)).send(any());
        // Longer than the retry TTL: an accidentally republished message must not retry again.
        assertThat(rabbitTemplate.receive(configuration.getCallbackQueue(), 2500)).isNull();
        verify(sendMailService, times(3)).send(any());
        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    private String publishMail() {
        String id = UUID.randomUUID().toString();
        String body = """
                {"id":"%s","from":"sender@example.org","recipients":["recipient@example.org"],
                 "mail":{"subject":"Integration mail","plainTextContent":"Hello from RabbitMQ"}}
                """.formatted(id);
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setHeader("X-correlation-id", id);
        properties.setHeader("internal-header", "must not reach the callback");
        rabbitTemplate.send(configuration.getExchange(), configuration.getRoutingKey(),
                new Message(body.getBytes(StandardCharsets.UTF_8), properties));
        return id;
    }

    private JsonObject receiveCallback(String id, boolean success) {
        Message message = rabbitTemplate.receive(configuration.getCallbackQueue(), 15000);
        assertThat(message).as("callback for mail %s", id).isNotNull();
        assertThat(message.getMessageProperties().getHeaders())
                .containsEntry("X-correlation-id", id)
                .doesNotContainKeys("internal-header", "requeueCount", "x-death");
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        JsonObject callback = JsonParser.parseString(new String(message.getBody(), StandardCharsets.UTF_8))
                .getAsJsonObject();
        assertThat(callback.get("id").getAsString()).isEqualTo(id);
        assertThat(callback.get("success").getAsBoolean()).isEqualTo(success);
        return callback;
    }

    private void assertDeliveredMail() throws Exception {
        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        var mail = greenMail.getReceivedMessages()[0];
        assertThat(mail.getSubject()).isEqualTo("Integration mail");
        assertThat(mail.getAllRecipients()[0].toString()).isEqualTo("recipient@example.org");
        assertThat(mail.getContent().toString()).contains("Hello from RabbitMQ");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Services {
        @Bean
        @ServiceConnection
        RabbitMQContainer rabbitMQ() {
            return new RabbitMQContainer("rabbitmq:4.1.0-management");
        }

        @Bean(initMethod = "start", destroyMethod = "stop")
        GreenMail greenMail() {
            return new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP))
                    .withConfiguration(GreenMailConfiguration.aConfig().withUser("fancy", "ycnaf"));
        }

        @Bean
        DynamicPropertyRegistrar smtpProperties(GreenMail greenMail) {
            return registry -> {
                registry.add("spring.mail.host", () -> "127.0.0.1");
                registry.add("spring.mail.properties.mail.smtp.port", () -> greenMail.getSmtp().getPort());
            };
        }
    }
}
