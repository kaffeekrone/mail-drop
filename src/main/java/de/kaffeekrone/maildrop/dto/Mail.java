package de.kaffeekrone.maildrop.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import java.util.List;

@Setter
@Getter
@Builder
public class Mail {

    private String htmlContent;
    private String plainTextContent;
    private String subject;
    @Singular
    private List<Attachment> attachments;


}
