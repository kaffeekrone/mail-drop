package de.kaffeekrone.maildrop;

import org.apache.commons.mail2.core.EmailConstants;
import org.springframework.boot.autoconfigure.mail.MailProperties;

import java.util.Properties;

public class MailDropMailProperties {

    private final MailProperties originalProperties;

    public MailDropMailProperties(MailProperties originalProperties) {
        this.originalProperties = originalProperties;
    }

    public Properties asProperties() {
        Properties out = new Properties();
        if (originalProperties.getProperties() != null) {
            out.putAll(originalProperties.getProperties());
        }

        putIfNonNull(out, EmailConstants.MAIL_HOST, originalProperties.getHost());
        putIfNonNull(out, EmailConstants.MAIL_PORT, originalProperties.getPort());
        putIfNonNull(out, EmailConstants.MAIL_SMTP_USER, originalProperties.getUsername());
        putIfNonNull(out, EmailConstants.MAIL_SMTP_PASSWORD, originalProperties.getPassword());

        putIfNonNull(out, EmailConstants.MAIL_TRANSPORT_PROTOCOL, originalProperties.getProtocol());

        return out;
    }

    private void putIfNonNull(Properties properties, String constant, Object value) {
        if (value != null) {
            properties.put(constant, value);
        }
    }

    public String getUsername() {
        return asProperties().getProperty(EmailConstants.MAIL_SMTP_USER);
    }

    public String getPassword() {
        return asProperties().getProperty(EmailConstants.MAIL_SMTP_PASSWORD);
    }
}
