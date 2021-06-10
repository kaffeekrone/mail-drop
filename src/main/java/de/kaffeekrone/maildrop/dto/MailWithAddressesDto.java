
package de.kaffeekrone.maildrop.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

@Builder
public class MailWithAddressesDto {

    @Setter
    @Getter
    private MailDto mail;
    @Singular
    @Setter
    @Getter
    private List<String> recipients;
    @Getter
    @Setter
    private String from;
    @Singular("cc")
    @Setter
    @Getter
    private List<String> cc;
    @Singular("bcc")
    @Setter
    @Getter
    private List<String> bcc;

}
