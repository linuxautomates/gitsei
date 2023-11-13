package io.levelops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sendgrid.helpers.mail.objects.Attachments;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@lombok.Value
@lombok.Builder(toBuilder = true)
@JsonDeserialize(builder = Email.EmailBuilder.class)
public class Email {

    @JsonProperty("from")
    EmailContact from;

    @JsonProperty("recipients")
    List<EmailContact> recipients;

    @JsonProperty("subject")
    String subject;

    @JsonProperty("content")
    String content;

    @Builder.Default
    @JsonProperty("content_type")
    String contentType = "text/plain";

    @Builder.Default
    @JsonProperty("attachments")
    List<Attachments> attachments = List.of();

    public static class EmailBuilder {

        public EmailBuilder from(EmailContact from) {
            this.from = from;
            return this;
        }

        public EmailBuilder from(String email) {
            this.from(EmailContact.builder()
                    .email(email)
                    .build());
            return this;
        }

        public EmailBuilder from(String email, String name) {
            this.from(EmailContact.builder()
                    .email(email)
                    .name(name)
                    .build());
            return this;
        }

        private EmailBuilder recipient(EmailContact emailContact) {
            if (recipients == null) {
                recipients = new ArrayList<>();
            }
            recipients.add(emailContact);
            return this;
        }

        public EmailBuilder recipient(String email) {
            return this.recipient(EmailContact.builder()
                    .email(email)
                    .build());
        }

        public EmailBuilder recipient(String email, String name) {
            return this.recipient(EmailContact.builder()
                    .email(email)
                    .name(name)
                    .build());
        }
    }

}
