package io.levelops.utils;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Personalization;
import io.levelops.models.Email;
import io.levelops.models.EmailContact;

public class SendGridUtils {

    public static Mail toSengGridMail(Email email) {
        Personalization personalization = new Personalization();
        email.getRecipients().stream()
                .map(SendGridUtils::toSendGridEmailContact)
                .forEach(personalization::addTo);

        Mail mail = new Mail();
        mail.setFrom(toSendGridEmailContact(email.getFrom()));
        mail.setSubject(email.getSubject());
        mail.addContent(new Content(email.getContentType(), email.getContent()));
        mail.addPersonalization(personalization);
        if(email.getAttachments() != null && email.getAttachments().size() > 0) {
            for (Attachments attachments: email.getAttachments()) {
                mail.addAttachments(attachments);
            }
        }

        return mail;
    }

    public static com.sendgrid.helpers.mail.objects.Email toSendGridEmailContact(EmailContact contact) {
        return new com.sendgrid.helpers.mail.objects.Email(contact.getEmail(), contact.getName());
    }
}
