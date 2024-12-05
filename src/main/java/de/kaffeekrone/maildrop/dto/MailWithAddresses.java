
package de.kaffeekrone.maildrop.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.util.Map;
import java.util.Set;

@Setter
@Getter
@Builder
public class MailWithAddresses {

    private Mail mail;
    @Singular
    private Set<String> recipients;
    private String from;
    @Singular("cc")
    private Set<String> cc;
    @Singular("bcc")
    private Set<String> bcc;

    private Set<String> replyTo;

    private Map<String, String> customHeaders;

    private String id;
}
