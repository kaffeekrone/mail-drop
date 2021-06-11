package de.kaffeekrone.maildrop.conf;

import de.kaffeekrone.maildrop.MailDropConfiguration;
import de.kaffeekrone.maildrop.MailDropMailProperties;
import de.kaffeekrone.maildrop.SendMailService;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SendMailServiceConfiguration {


    @Bean
    MailDropMailProperties mailDropMailProperties(MailProperties mailProperties) {
        return new MailDropMailProperties(mailProperties);
    }

    @Bean
    SendMailService sendMailService(MailDropConfiguration mailDropConfiguration, MailDropMailProperties mailProperties) {
        return new SendMailService(mailDropConfiguration, mailProperties);
    }


}
