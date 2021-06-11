package de.kaffeekrone.maildrop.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;


class MailWithAddressesTest {

    @Test
    public void testGson() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (JsonReader is = new JsonReader(new InputStreamReader(ClassUtils.getDefaultClassLoader().getResourceAsStream("mail.json")))) {
            MailWithAddresses mailWithAddresses = gson.fromJson(is, MailWithAddresses.class);

            assertThat(mailWithAddresses.getFrom()).isEqualTo("defaultfromaddress@fancydomain.de");
            assertThat(mailWithAddresses.getRecipients()).containsExactlyInAnyOrder("fun@bla.de");
            assertThat(mailWithAddresses.getCc()).isEmpty();
            assertThat(mailWithAddresses.getBcc()).isEmpty();

            Mail mail = mailWithAddresses.getMail();

            assertThat(mail.getSubject()).isEqualTo("huhu");
            assertThat(mail.getPlainTextContent()).isEqualTo("content🙉");
            assertThat(mail.getHtmlContent()).isEqualTo("<html><body>A<br><img src=\"cid:fancyCid\"/></a><br>B</body></html>");

            assertThat(mail.getAttachments()).extracting(
                    Attachment::getName,
                    Attachment::getDescription,
                    Attachment::getContentDisposition,
                    Attachment::getContentId,
                    Attachment::getMimeType
            ).containsExactlyInAnyOrder(
                    tuple("imageFileName", null, Attachment.ContentDisposition.INLINE, "fancyCid", "image/png"),
                    tuple("additionalAttachmentFileName", null, Attachment.ContentDisposition.ATTACHMENT, null, "image/png"));


        }
    }

}