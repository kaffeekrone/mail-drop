
package de.kaffeekrone.maildrop.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.util.Map;
import java.util.Set;

@Builder
public class MailWithAddresses {

    @Setter
    @Getter
    private Mail mail;
    @Singular
    @Setter
    @Getter
    private Set<String> recipients;
    @Getter
    @Setter
    private String from;
    @Singular("cc")
    @Setter
    @Getter
    private Set<String> cc;
    @Singular("bcc")
    @Setter
    @Getter
    private Set<String> bcc;

    @Getter
    @Setter
    private Set<String> replyTo;

    @Getter
    @Setter
    private Map<String, String> customHeaders;

    @Getter
    @Setter
    private String id;
}
