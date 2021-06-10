package de.kaffeekrone.maildrop.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.util.List;

@Builder
public class MailDto {

    @Setter
    @Getter
    private String htmlContent;
    @Setter
    @Getter
    private String plainTextContent;
    @Setter
    @Getter
    private String subject;
    @Singular
    @Setter
    @Getter
    private List<MailAttachmentDto> mailAttachments;


}
