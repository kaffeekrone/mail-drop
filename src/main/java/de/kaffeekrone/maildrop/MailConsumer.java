package de.kaffeekrone.maildrop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.rabbitmq.client.Channel;
import de.kaffeekrone.maildrop.dto.Callback;
import de.kaffeekrone.maildrop.dto.MailWithAddresses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.HashMap;

@Component
public class MailConsumer implements ChannelAwareMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MailConsumer.class);

    private final MailDropConfiguration mailDropConfiguration;
    private final SendMailService sendMailService;
    private final RabbitTemplate rabbitTemplate;
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class,
                    (JsonSerializer<ZonedDateTime>) (date, type, context) -> new JsonPrimitive(date.toString()))
            .create();

    @Autowired
    public MailConsumer(MailDropConfiguration mailDropConfiguration, SendMailService sendMailService, RabbitTemplate rabbitTemplate) {
        this.mailDropConfiguration = mailDropConfiguration;
        this.sendMailService = sendMailService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void onMessage(Message message, Channel channel) {
        String messageStr = new String(message.getBody(), StandardCharsets.UTF_8);
        logger.debug("Received '{}'", messageStr);

        String mailId = null;
        try {
            MailWithAddresses mailWithAddresses = gson.fromJson(messageStr, MailWithAddresses.class);
            mailId = mailWithAddresses.getId();

            sendMailService.send(mailWithAddresses);

            notifyCallback(message, mailId, true);

        } catch (Exception e) {
            logger.error("Unable to handle message {}", messageStr, e);
            pushToRetryQueue(message, mailId);
        }
    }

    private void pushToRetryQueue(Message originalMessage, String mailId) {
        try {
            MessageProperties properties = originalMessage.getMessageProperties();
            Object requeueCountHeader = properties.getHeaders().getOrDefault("requeueCount", 0);
            if (!(requeueCountHeader instanceof Integer requeueCount) || requeueCount < 0) {
                logger.warn("Discarding invalid requeueCount header: {}", requeueCountHeader);
                notifyCallback(originalMessage, mailId, false);
                return;
            }

            long nextRequeueCount = (long) requeueCount + 1;
            if (nextRequeueCount >= mailDropConfiguration.getRetryAttempts()) {
                notifyCallback(originalMessage, mailId, false);
            } else {
                HashMap<String, Object> headers = new HashMap<>(properties.getHeaders());
                headers.put("requeueCount", (int) nextRequeueCount);

                MessageProperties messageProperties = new MessageProperties();
                messageProperties.setContentType("application/json");
                messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());

                headers.forEach(messageProperties::setHeader);

                rabbitTemplate.send(mailDropConfiguration.getRetryExchange(),
                        mailDropConfiguration.getRetryRoutingKey(),
                        new Message(originalMessage.getBody(), messageProperties));

            }

        } catch (IOException e) {
            logger.error("Retry Queue failed", e);
        }
    }


    private void notifyCallback(Message originalMessage, String mailId, boolean success) throws IOException {
        if (!mailDropConfiguration.isEnableCallback()) {
            logger.debug("Skipped notifying callback queue, it is disabled");
            return;
        }
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/json");
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());

        originalMessage.getMessageProperties().getHeaders().entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith("X-"))
                .forEach(e -> messageProperties.setHeader(e.getKey(), e.getValue()));

        Callback callback = new Callback();
        callback.setSuccess(success);
        callback.setId(mailId);
        if (success) {
            callback.setSentDate(ZonedDateTime.now());
        }

        byte[] bytes = gson.toJson(callback).getBytes(StandardCharsets.UTF_8);
        rabbitTemplate.send(mailDropConfiguration.getCallbackExchange(),
                mailDropConfiguration.getCallbackRoutingKey(),
                new Message(bytes, messageProperties));
    }
}
