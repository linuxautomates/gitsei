package io.levelops.services;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import io.levelops.exceptions.EmailException;
import io.levelops.models.Email;
import io.levelops.utils.SendGridUtils;

import java.io.IOException;

public class EmailService {

    private final SendGrid sendGrid;

    public EmailService(String sendGridApiKey) {
        sendGrid = new SendGrid(sendGridApiKey);
    }

    public void send(Email email) throws EmailException {
        Mail mail = SendGridUtils.toSengGridMail(email);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                return;
            }

            throw new EmailException(String.format("Failed to send email. status=%d, response='%s'",
                    response.getStatusCode(), response.getBody()));
        } catch (IOException ex) {
            throw new EmailException("Could not send email", ex);
        }
    }


}
