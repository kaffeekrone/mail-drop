package de.kaffeekrone.maildrop;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import de.kaffeekrone.maildrop.dto.Attachment;
import de.kaffeekrone.maildrop.dto.Mail;
import de.kaffeekrone.maildrop.dto.MailWithAddresses;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;

import static de.kaffeekrone.maildrop.SendMailService.toAsteriskMail;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SendMailServiceTest {

    private static final String DEFAULT_FROM_ADDRESS = "defaultfromaddress@fancydomain.de";
    private static final String TEXT_PLAIN_CONTENT_TYPE = "text/plain; charset=UTF-8";
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";
    private static final String IMAGE_CONTENT_TYPE = "image/png";
    private static final String DEFAULT_PLAIN_TEXT_CONTENT = "content\uD83D\uDE49";
    private static final String DEFAULT_SUBJECT = "huhu";

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("fancy", "ycnaf"));

    @Autowired
    private SendMailService mailService;
    private static final String DEFAULT_RECIPIENT = "fun@bla.de";

    @Test
    void toAsteriskMailTest() {
        assertThat(toAsteriskMail("123456789@funny.de")).isEqualTo("1*****9@f*****y.de");
        assertThat(toAsteriskMail("g@l.com")).isEqualTo("g@l.com");
    }

    @Test
    public void brokenMailAddress() {
        MailWithAddresses mail = MailWithAddresses.builder()
                .from("trfvzgbhjnkml")
                .recipient(DEFAULT_RECIPIENT)
                .mail(Mail.builder()
                        .subject(DEFAULT_SUBJECT)
                        .plainTextContent(DEFAULT_PLAIN_TEXT_CONTENT)
                        .build())
                .build();

        Assertions.assertThatThrownBy(() -> mailService.send(mail))
                .isInstanceOf(SendMailException.class)
                .hasMessage("Issue while sending mail!");
    }


    @Test
    public void sendMailWithoutContent() {
        MailWithAddresses mail = MailWithAddresses.builder()
                .from(DEFAULT_FROM_ADDRESS)
                .recipient(DEFAULT_RECIPIENT)
                .mail(Mail.builder()
                        .subject(DEFAULT_SUBJECT)
                        .build())
                .build();

        Assertions.assertThatThrownBy(() -> mailService.send(mail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Neither plain nor html content supplied!");
    }

    @Test
    public void plainTextMail() throws MessagingException, IOException {
        MailWithAddresses mail = MailWithAddresses.builder()
                .from(DEFAULT_FROM_ADDRESS)
                .recipient(DEFAULT_RECIPIENT)
                .mail(Mail.builder()
                        .subject(DEFAULT_SUBJECT)
                        .plainTextContent(DEFAULT_PLAIN_TEXT_CONTENT)
                        .build())
                .build();


        mailService.send(mail);

        MimeMessage message = getReceivedMessage();
        assertThat(message.getSubject()).isEqualTo(DEFAULT_SUBJECT);

        assertThat(message.getContentType()).startsWith(TEXT_PLAIN_CONTENT_TYPE);
        assertThat((String) message.getContent()).startsWith(DEFAULT_PLAIN_TEXT_CONTENT);
    }


    @Test
    public void plainTextWithAttachmentsMail() throws MessagingException, IOException {

        String imageNameFileName = "imageName";
        byte[] image = createRandomImage();

        MailWithAddresses mail = MailWithAddresses.builder()
                .from(DEFAULT_FROM_ADDRESS)
                .recipient(DEFAULT_RECIPIENT)
                .mail(Mail.builder()
                        .subject(DEFAULT_SUBJECT)
                        .plainTextContent(DEFAULT_PLAIN_TEXT_CONTENT)
                        .attachment(Attachment.builder()
                                .name(imageNameFileName)
                                .contentDisposition(Attachment.ContentDisposition.ATTACHMENT)
                                .base64Content(toBase64(image))
                                .mimeType(IMAGE_CONTENT_TYPE)
                                .build())
                        .build())
                .build();


        mailService.send(mail);

        MimeMessage message = getReceivedMessage();

        assertThat(message.getContentType()).startsWith("multipart/mixed;");
        MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
        assertThat(mimeMultipart.getCount()).isEqualTo(2);
        MimeBodyPart bodyPart = (MimeBodyPart) mimeMultipart.getBodyPart(0);

        assertPlainTextContent(bodyPart);

        MimeBodyPart attachmentPart = (MimeBodyPart) mimeMultipart.getBodyPart(1);

        assertImageBodyPart(imageNameFileName, image, attachmentPart, Attachment.ContentDisposition.ATTACHMENT);
        assertThat(attachmentPart.getDescription()).isNull();
    }

    @Test
    public void htmlMailWithAlternativeText() throws MessagingException, IOException {

        String htmlContent = "<html><body>" +
                "<a href=\"https://www.kaffeekrone.de\">\uD83D\uDE20<img src=\"https://i.imgur.com/LK2VJvS.jpg\"/></a>" +
                "</body></html>";

        MailWithAddresses mail = MailWithAddresses.builder()
                .from(DEFAULT_FROM_ADDRESS)
                .recipient(DEFAULT_RECIPIENT)
                .mail(Mail.builder()
                        .subject(DEFAULT_SUBJECT)
                        .htmlContent(htmlContent)
                        .plainTextContent(DEFAULT_PLAIN_TEXT_CONTENT)
                        .build())
                .build();


        mailService.send(mail);

        MimeMessage firstReceivedMessage = getReceivedMessage();
        assertThat(firstReceivedMessage.getSubject()).isEqualTo(DEFAULT_SUBJECT);

        assertThat(firstReceivedMessage.getContentType()).startsWith("multipart/alternative;");
        MimeMultipart mimeMultipart = (MimeMultipart) firstReceivedMessage.getContent();

        assertThat(mimeMultipart.getCount()).isEqualTo(2);
        BodyPart htmlBodyPart = mimeMultipart.getBodyPart(1);

        MimeBodyPart plainTextPart = (MimeBodyPart) mimeMultipart.getBodyPart(0);

        assertPlainTextContent(plainTextPart);

        assertThat(htmlBodyPart.getContent()).isEqualTo(htmlContent);
        assertThat(htmlBodyPart.getContentType()).isEqualTo(HTML_CONTENT_TYPE);
    }


    @Test
    public void htmlMailWithAlternativeTextInlineAndAdditionalAttachment() throws MessagingException, IOException {
        // - multipart/mixed
        //     - multipart/related
        //         - multipart/alternative
        //            - text/plain
        //            - text/html
        //         - image/png (with cid)
        // - image/jpeg


        String contentId = "fancyCid";
        String additionalAttachmentFileName = "additionalAttachmentFileName";

        String htmlMailContent = "<html><body>A<br><br>" +
                "<img src=\"cid:" + contentId + "\"/></a><br>" +
                "B</body></html>";

        byte[] image = createRandomImage();
        byte[] additionalImage = createRandomImage();

        String imageFileName = "imageFileName";

        MailWithAddresses mail = MailWithAddresses.builder()
                .from(DEFAULT_FROM_ADDRESS)
                .recipient(DEFAULT_RECIPIENT)
                .mail(Mail.builder()
                        .subject(DEFAULT_SUBJECT)
                        .htmlContent(htmlMailContent)
                        .plainTextContent(DEFAULT_PLAIN_TEXT_CONTENT)
                        .attachment(Attachment.builder()
                                .contentDisposition(Attachment.ContentDisposition.INLINE)
                                .contentId(contentId)
                                .name(imageFileName)
                                .base64Content(toBase64(image))
                                .mimeType(IMAGE_CONTENT_TYPE)
                                .build())
                        .attachment(Attachment.builder()
                                .contentDisposition(Attachment.ContentDisposition.ATTACHMENT)
                                .name("additionalAttachmentFileName")
                                .base64Content(toBase64(additionalImage))
                                .mimeType(IMAGE_CONTENT_TYPE)
                                .build())
                        .build())
                .build();

        mailService.send(mail);

        MimeMessage firstReceivedMessage = getReceivedMessage();
        assertThat(firstReceivedMessage.getSubject()).isEqualTo(DEFAULT_SUBJECT);

        assertThat(firstReceivedMessage.getContentType()).startsWith("multipart/mixed;");
        MimeMultipart mimeMultipartMixed = (MimeMultipart) firstReceivedMessage.getContent();

        assertThat(mimeMultipartMixed.getCount()).isEqualTo(2);

        MimeMultipart multipartRelated = (MimeMultipart) mimeMultipartMixed.getBodyPart(0).getContent();
        assertThat(multipartRelated.getCount()).isEqualTo(2);
        assertThat(multipartRelated.getContentType()).startsWith("multipart/related;");

        MimeMultipart multiPartAlternative = (MimeMultipart) multipartRelated.getBodyPart(0).getContent();
        assertThat(multiPartAlternative.getCount()).isEqualTo(2);
        assertThat(multiPartAlternative.getContentType()).startsWith("multipart/alternative;");

        BodyPart plainBodyPart = multiPartAlternative.getBodyPart(0);
        assertPlainTextContent(plainBodyPart);


        BodyPart htmlBodyPart = multiPartAlternative.getBodyPart(1);
        assertThat(htmlBodyPart.getContent()).isEqualTo(htmlMailContent);
        assertThat(htmlBodyPart.getContentType()).isEqualTo(HTML_CONTENT_TYPE);

        MimeBodyPart imagePart = (MimeBodyPart) multipartRelated.getBodyPart(1);
        assertImageBodyPart(imageFileName, image, imagePart, Attachment.ContentDisposition.INLINE);
        assertThat(imagePart.getContentID()).isEqualTo("<" + contentId + ">");

        BodyPart additionalImageBodyPart = mimeMultipartMixed.getBodyPart(1);
        assertImageBodyPart(additionalAttachmentFileName, additionalImage, additionalImageBodyPart, Attachment.ContentDisposition.ATTACHMENT);
    }

    private MimeMessage getReceivedMessage() {
        return greenMail.getReceivedMessages()[0];
    }


    private void assertImageBodyPart(String fileName, byte[] image, Part attachmentPart, Attachment.ContentDisposition contentDisposition) throws MessagingException, IOException {
        assertThat(attachmentPart.getFileName()).isEqualTo(fileName);

        assertThat(attachmentPart.getInputStream().readAllBytes()).isEqualTo(image);
        assertThat(attachmentPart.getContentType()).startsWith(IMAGE_CONTENT_TYPE);
        assertThat(attachmentPart.getDisposition()).startsWith(contentDisposition.toLowerCase());
    }

    private void assertPlainTextContent(Part bodyPart) throws IOException, MessagingException {
        assertThat(bodyPart.getContentType()).isEqualTo(TEXT_PLAIN_CONTENT_TYPE);
        assertThat(bodyPart.getContent()).isEqualTo(SendMailServiceTest.DEFAULT_PLAIN_TEXT_CONTENT);
    }

    private String toBase64(byte[] byteArray) {
        return new String(Base64.getEncoder().encode(byteArray));
    }

    private static byte[] createRandomImage() {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            int width = 16;
            int height = 16;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int a = (int) (Math.random() * 256); //alpha
                    int r = (int) (Math.random() * 256); //red
                    int g = (int) (Math.random() * 256); //green
                    int b = (int) (Math.random() * 256); //blue

                    int p = (a << 24) | (r << 16) | (g << 8) | b; //pixel

                    img.setRGB(x, y, p);
                }
            }

            ImageIO.write(img, "png", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}