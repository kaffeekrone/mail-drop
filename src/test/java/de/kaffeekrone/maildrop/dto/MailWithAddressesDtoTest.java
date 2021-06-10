package de.kaffeekrone.maildrop.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MailWithAddressesDtoTest {

    @Test
    public void testSmth() {
        MailWithAddressesDto build = MailWithAddressesDto.builder()
                .recipient("huhu@bla.de")
                .recipient("huhu2@bla.de")
                .mail(MailDto.builder()
                        .mailAttachment(MailAttachmentDto.builder()
                                .name("someAttachment")
                                .dataSource(DataSourceDto.builder()
                                        .base64Content("aHVodQ==")
                                        .build())
                                .build())
                        .build())
                .build();

        System.out.println(build.getRecipients());
    }

    @Test
    public void testGson() {
        MailWithAddressesDto mail = MailWithAddressesDto.builder()
                .recipient("huhu@bla.de")
//                .recipient("huhu2@bla.de")
                .from("from@funny.de")
                .mail(MailDto.builder()
                        .subject("Gurkenfeld")
                        .plainTextContent("somePlainText")
//                        .mailAttachment(MailAttachmentDto.builder()
//                                .name("someAttachment")
//                                .dataSource(DataSourceDto.builder()
//                                        .base64Content("aHVodQ==")
//                                        .build())
//                                .build())
                        .build())
                .build();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String s = gson.toJson(mail);
        System.out.println(s);

    }

}