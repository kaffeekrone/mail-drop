package de.kaffeekrone.maildrop;

import de.kaffeekrone.maildrop.dto.Attachment;
import de.kaffeekrone.maildrop.dto.Mail;
import de.kaffeekrone.maildrop.dto.MailWithAddresses;
import org.apache.commons.mail.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

public class SendMailService {

    private static final Logger logger = LoggerFactory.getLogger(SendMailService.class);

    private final MailDropConfiguration mailDropConfiguration;
    private final MailDropMailProperties mailProperties;
    private Session session;

    public SendMailService(MailDropConfiguration mailDropConfiguration, MailDropMailProperties mailProperties) {
        this.mailDropConfiguration = mailDropConfiguration;
        this.mailProperties = mailProperties;
    }

    private synchronized Session getSession() {
        if (this.session == null) {
            Authenticator authenticator = null;
            if (hasText(mailProperties.getUsername()) && hasText(mailProperties.getPassword())) {
                authenticator = new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(mailProperties.getUsername(), mailProperties.getPassword());
                    }
                };
            }

            this.session = Session.getInstance(mailProperties.asProperties(), authenticator);
        }

        // a way to enable debug ont he mail session might be nice but this is performance intensive ..

        return this.session;
    }

    private String toAsteriskMail(Set<String> mailAddresses) {
        return mailAddresses.stream().map(SendMailService::toAsteriskMail).collect(Collectors.joining());
    }

    static String toAsteriskMail(String mailAddress) {
        try {
            String address = new InternetAddress(mailAddress).getAddress();
            String[] parts = address.split("@");
            return parts[0].replaceAll("^(.)(.*)?(.)$", "$1*****$3")
                    + "@"
                    + parts[1].replaceAll("^(.)(.*)?(.)(\\..+)$", "$1*****$3$4");
        } catch (AddressException e) {
            throw new SendMailException("invalid mail address: " + mailAddress, e);
        }
    }

    public void send(MailWithAddresses mailWithAddresses) {

        final String from;
        if (hasText(mailWithAddresses.getFrom())) {
            from = mailWithAddresses.getFrom();
        } else {
            from = mailDropConfiguration.getDefaultFromAddress();
        }

        Mail mail = mailWithAddresses.getMail();
        Set<String> recipients = mailWithAddresses.getRecipients();
        Set<String> cc = mailWithAddresses.getCc();
        Set<String> bcc = mailWithAddresses.getBcc();

        if (logger.isInfoEnabled()) {
            logger.info("Trying to send Mail with subject {} to: {} cc: {} bcc: {}", mail.getSubject(),
                    toAsteriskMail(recipients), toAsteriskMail(cc), toAsteriskMail(bcc));
        }

        final String messageId;
        try {
            final Email email = createMail(mail);

            email.setFrom(from);
            email.addTo(recipients.toArray(new String[0]));
            if (!CollectionUtils.isEmpty(cc)) {
                email.addCc(cc.toArray(new String[0]));
            }
            if (!CollectionUtils.isEmpty(bcc)) {
                email.addBcc(bcc.toArray(new String[0]));
            }

            // this is quite implicit but since we know that we received the data via JSON it is always UTF-8 ..
            email.setCharset(StandardCharsets.UTF_8.name());

            email.setMailSession(getSession());
            messageId = email.send();
        } catch (EmailException e) {
            throw new SendMailException("Issue while sending mail!", e);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Send Mail with subject {} successfully to: {} cc: {} bcc: {} with messageId {}", mail.getSubject(),
                    toAsteriskMail(recipients), toAsteriskMail(cc), toAsteriskMail(bcc), messageId);
        }
    }


    private Email createMail(Mail mail) throws EmailException {
        Email email;
        if (hasText(mail.getHtmlContent())) {
            email = createHtmlMail(mail);
        } else if (!CollectionUtils.isEmpty(mail.getAttachments())) {
            email = createMultiPartMail(mail);
        } else if (hasText(mail.getPlainTextContent())) {
            email = createPlainTextMail(mail);
        } else {
            throw new IllegalArgumentException("Neither plain nor html content supplied!");
        }
        email.setSubject(mail.getSubject());
        return email;
    }

    private Email createPlainTextMail(Mail mail) throws EmailException {
        Email email = new SimpleEmail();
        email.setMsg(mail.getPlainTextContent());
        return email;
    }

    private MultiPartEmail createMultiPartMail(Mail mail) throws EmailException {
        MultiPartEmail multiPartMail = new MultiPartEmail();
        attachAttachmentsToMultiPartMail(mail, multiPartMail);

        String content = mail.getPlainTextContent();
        multiPartMail.setMsg(content);
        return multiPartMail;
    }

    private HtmlEmail createHtmlMail(Mail mail) throws EmailException {
        HtmlEmail htmlMail = new HtmlEmail();
        String htmlContent = mail.getHtmlContent();
        htmlMail.setHtmlMsg(htmlContent);

        if (hasText(mail.getPlainTextContent())) {
            htmlMail.setTextMsg(mail.getPlainTextContent());
        }
        for (Attachment attachment : mail.getAttachments()) {
            if (attachment.isInline()) {
                htmlMail.embed(attachment.asDatasource(), attachment.getName(), attachment.getContentId());
            } else {
                htmlMail.attach(attachment.asDatasource(),
                        attachment.getName(),
                        attachment.getDescription(),
                        attachment.getContentDisposition().toLowerCase());
            }
        }
        return htmlMail;
    }

    private void attachAttachmentsToMultiPartMail(Mail mail, MultiPartEmail multiPartMail) throws EmailException {
        for (Attachment mailAttachment : mail.getAttachments()) {
            if (mailAttachment.isInline()) {
                logger.warn("Plain Text and and inline attachment is not possible ignoring attachment {}", mailAttachment.getName());
            }
            multiPartMail.attach(mailAttachment.asDatasource(),
                    mailAttachment.getName(), mailAttachment.getDescription(),
                    mailAttachment.getContentDisposition().toLowerCase());
        }
    }

}
