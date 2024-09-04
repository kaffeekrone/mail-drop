package de.kaffeekrone.maildrop;

import de.kaffeekrone.maildrop.dto.Attachment;
import de.kaffeekrone.maildrop.dto.Mail;
import de.kaffeekrone.maildrop.dto.MailWithAddresses;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.apache.commons.mail2.core.EmailException;
import org.apache.commons.mail2.jakarta.Email;
import org.apache.commons.mail2.jakarta.HtmlEmail;
import org.apache.commons.mail2.jakarta.MultiPartEmail;
import org.apache.commons.mail2.jakarta.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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
        if (!CollectionUtils.isEmpty(mailAddresses)) {
            return mailAddresses.stream()
                    .map(SendMailService::toAsteriskMail)
                    .collect(Collectors.joining());
        } else {
            return "";
        }
    }

    static String toAsteriskMail(String mailAddress) {
        try {
            InternetAddress internetAddress = new InternetAddress(mailAddress);
            String address = internetAddress.getAddress();
            if (address.contains("@")) {
                String[] parts = address.split("@");
                return parts[0].replaceAll("^(.)(.*)?(.)$", "$1*****$3")
                        + "@"
                        + parts[1].replaceAll("^(.)(.*)?(.)(\\..+)$", "$1*****$3$4");
            } else {
                return "";
            }
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
        Set<String> replyTo = mailWithAddresses.getReplyTo();

        if (logger.isInfoEnabled()) {
            logger.info("Trying to send Mail with subject {} and id {} to: {} cc: {} bcc: {}", mail.getSubject(), mailWithAddresses.getId(),
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

            if (!CollectionUtils.isEmpty(bcc)) {
                email.addBcc(bcc.toArray(new String[0]));
            }

            if (!CollectionUtils.isEmpty(replyTo)) {
                Set<InternetAddress> set = new HashSet<>();
                for (String s : replyTo) {
                    InternetAddress e = new InternetAddress(s);
                    e.validate();
                    set.add(e);
                }
                email.setReplyTo(set);
            }

            if (!CollectionUtils.isEmpty(mailWithAddresses.getCustomHeaders())) {
                email.setHeaders(mailWithAddresses.getCustomHeaders());
            }

            // this is quite implicit but since we know that we received the data via JSON it is always UTF-8 ..
            email.setCharset(StandardCharsets.UTF_8.name());

            email.setMailSession(getSession());
            messageId = email.send();
        } catch (EmailException | AddressException e) {
            throw new SendMailException("Issue while sending mail!", e);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Send Mail with subject {} and id {} successfully to: {} cc: {} bcc: {} with messageId {}", mail.getSubject(),
                    mailWithAddresses.getId(), toAsteriskMail(recipients), toAsteriskMail(cc), toAsteriskMail(bcc), messageId);
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
