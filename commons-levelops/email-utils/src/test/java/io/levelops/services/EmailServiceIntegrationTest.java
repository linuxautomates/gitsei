package io.levelops.services;

import com.sendgrid.helpers.mail.objects.Attachments;
import io.levelops.exceptions.EmailException;
import io.levelops.models.Email;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.util.List;

public class EmailServiceIntegrationTest {

    @Test
    public void send() throws EmailException {
        EmailService emailService = new EmailService(System.getenv("SENDGRID_API_KEY"));

        emailService.send(Email.builder()
                .from("notification@levelops.io")
                .recipient("sid@levelops.io")
                .subject("Hello")
                .content("Wassup")
                .attachments(List.of(new Attachments.Builder("test.csv",
                                IOUtils.toInputStream("sample,attachment,csv", "UTF-8")).build()))
                .build());
    }
}