
package de.kaffeekrone.maildrop.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
public class MailAttachmentDto {


    @Setter
    @Getter
    private DataSourceDto dataSource;

    @Setter
    @Getter
    private String name;

    @Setter
    @Getter
    private String description;

    @Setter
    @Getter
    private String contentDisposition;

    @Setter
    @Getter
    private String contentId;

}
