
package de.kaffeekrone.maildrop.dto;


import jakarta.activation.DataSource;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Base64;
import java.util.Locale;

@Setter
@Getter
@Builder
public class Attachment {

    public enum ContentDisposition {
        ATTACHMENT, INLINE;

        public String toLowerCase() {
            return name().toLowerCase(Locale.US);
        }
    }

    private String name;

    private String description;

    private ContentDisposition contentDisposition;

    private String contentId;

    private String base64Content;

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
