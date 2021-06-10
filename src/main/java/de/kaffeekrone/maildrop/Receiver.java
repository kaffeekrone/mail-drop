package de.kaffeekrone.maildrop;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import de.kaffeekrone.maildrop.dto.CallbackDto;
import de.kaffeekrone.maildrop.dto.MailDto;
import de.kaffeekrone.maildrop.dto.MailWithAddressesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.HashMap;

@Component
public class Receiver implements ChannelAwareMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(Receiver.class);

    private final MailDropConfiguration mailDropConfiguration;
    private final JavaMailSender javaMailSender;
    private final RabbitTemplate rabbitTemplate;
    private final Gson gson;

    @Autowired
    public Receiver(MailDropConfiguration mailDropConfiguration, JavaMailSender javaMailSender, RabbitTemplate rabbitTemplate, Gson gson) {
        this.mailDropConfiguration = mailDropConfiguration;
        this.javaMailSender = javaMailSender;
        this.rabbitTemplate = rabbitTemplate;
        this.gson = gson;
    }

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        String messageStr = new String(message.getBody(), StandardCharsets.UTF_8);
        logger.debug("Received '{}'", messageStr);

        try {
            MailWithAddressesDto mailWithAddresses = gson.fromJson(messageStr, MailWithAddressesDto.class);

            sendMail(mailWithAddresses);

            notifyCallback(message, true);

        } catch (Exception e) {
            logger.error("Unable to handle message" + messageStr, e);
            pushToRetryQueue(message);
        }
    }

    private void sendMail(MailWithAddressesDto mailWithAddresses) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

        helper.setTo(mailWithAddresses.getRecipients().toArray(new String[0]));
        helper.setCc(mailWithAddresses.getCc().toArray(new String[0]));
        helper.setBcc(mailWithAddresses.getBcc().toArray(new String[0]));

        MailDto mail = mailWithAddresses.getMail();
        helper.setSubject(mail.getSubject());

        boolean hasPlainContent = StringUtils.hasText(mail.getPlainTextContent());
        boolean hasHtmlContent = StringUtils.hasText(mail.getHtmlContent());
        if (hasPlainContent && hasHtmlContent) {
            helper.setText(mail.getPlainTextContent(), mail.getHtmlContent());
        } else if (hasPlainContent) {
            helper.setText(mail.getPlainTextContent(), false);
        } else if (hasHtmlContent) {
            helper.setText(mail.getHtmlContent(), true);
        } else {
            throw new IllegalArgumentException("Mail has no content");
        }


//        for (MailAttachmentDto mailAttachment : mailWithAddresses.getMail().getMailAttachments()) {
//            helper.addAttachment(mailAttachment.getName(), mailAttachment.);
//        }
//        also look for
//     message.addInline("myLogo", new ClassPathResource("img/mylogo.gif"));
//     message.addAttachment("myDocument.pdf", new ClassPathResource("doc/myDocument.pdf"));

        javaMailSender.send(mimeMessage);
    }

    private void pushToRetryQueue(Message originalMessage) {
        try {
            MessageProperties properties = originalMessage.getMessageProperties();
            int requeueCount = (int) properties.getHeaders().getOrDefault("requeueCount", 0);
            if (requeueCount + 1 >= mailDropConfiguration.getRetryAttempts()) {
                notifyCallback(originalMessage, false);
            } else {
                HashMap<String, Object> headers = new HashMap<>(properties.getHeaders());
                headers.put("requeueCount", ++requeueCount);

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


    private void notifyCallback(Message originalMessage, boolean success) throws IOException {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/json");
        messageProperties.setContentEncoding(StandardCharsets.UTF_8.name());

        originalMessage.getMessageProperties().getHeaders().entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith("X-"))
                .forEach(e -> messageProperties.setHeader(e.getKey(), e.getValue()));

        CallbackDto callbackDto = new CallbackDto();
        callbackDto.setSuccess(success);
        if (success) {
            callbackDto.setSentDate(ZonedDateTime.now());
        }

        byte[] bytes = gson.toJson(callbackDto).getBytes(StandardCharsets.UTF_8);
        rabbitTemplate.send(mailDropConfiguration.getCallbackExchange(),
                mailDropConfiguration.getCallbackRoutingKey(),
                new Message(bytes, messageProperties));
    }
}