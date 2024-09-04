
package de.kaffeekrone.maildrop.dto;


import jakarta.activation.DataSource;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Base64;
import java.util.Locale;

@Builder
public class Attachment {

    public enum ContentDisposition {
        ATTACHMENT, INLINE;

        public String toLowerCase() {
            return name().toLowerCase(Locale.US);
        }
    }

    @Setter
    @Getter
    private String name;

    @Setter
    @Getter
    private String description;

    @Setter
    @Getter
    private ContentDisposition contentDisposition;

    @Setter
    @Getter
    private String contentId;

    @Getter
    @Setter
    private String base64Content;

    @Getter
    @Setter
    private String mimeType;

    public boolean isInline() {
        return getContentDisposition() != null
                && getContentDisposition() == ContentDisposition.INLINE;
    }

    public DataSource asDatasource() {
        byte[] bytes = Base64.getDecoder().decode(getBase64Content());
        ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(bytes, getMimeType());
        byteArrayDataSource.setName(getName());
        return byteArrayDataSource;
    }

}
