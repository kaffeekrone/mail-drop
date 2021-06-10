
package de.kaffeekrone.maildrop.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
public class DataSourceDto {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String base64Content;

    @Getter
    @Setter
    private String mimeType;

}
