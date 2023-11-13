package io.levelops.auth.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.auth.httpmodels.ResetToken;
import io.levelops.commons.databases.models.database.User;
import io.levelops.exceptions.EmailException;
import io.levelops.models.Email;
import io.levelops.models.EmailContact;
import io.levelops.services.EmailService;
import io.levelops.services.TemplateService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;

@Log4j2
@Service
@SuppressWarnings("unused")
public class SystemNotificationService {

    // \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/
    // TODO    MOVE THIS HTML INTO A RESOURCE FILE
    // /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\

    private static final String CREATION_NOTIFICATION_SUBJECT = "Propelo Account Created.";
    private static final String CREATION_NOTIFICATION_BODY = "<p>Hi $name,</p>\n" +
            "<p>An account has been created for you in Propelo. Please use the forgot password flow if the link expires.</p>\n" +
            "<p>Your login details are:</p>\n" +
            "<p> - company: $company</p>\n" +
            "<p> - username: $user</p>\n" +
            "<p> - login link: <a href=\"$baseurl/#/signin?set-password&name=$name&token=$encodedvalue\">Login Link</a>\n";

    private static final String RESET_NOTIFICATION_SUBJECT = "Propelo Password Reset.";
    private static final String RESET_NOTIFICATION_BODY = "<p>Hi $name,</p>\n" +
            "<p>Here are the details to reset your password. If you did not request this, please ignore this email.</p>\n" +
            "<p>Your login details are</p>\n" +
            "<p> - company: $company</p>\n" +
            "<p> - username: $user</p>\n" +
            "<p> - login link: <a href=\"$baseurl/#/signin?set-password&name=$name&token=$encodedvalue\">Reset Link</a>\n" +
            "<p> - alternate login link: <a href=\"$alternateBaseUrl/#/signin?set-password&name=$name&token=$encodedvalue\">Reset Link</a>\n";

    private static final String FROM_EMAIL = "do-not-reply@levelops.io";

    private final EmailService emailService;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String alternateBaseUrl;

    @Autowired
    public SystemNotificationService(
            /* TODO remove OAUTH_BASE_URL in favor of APP_BASE_URL when both URL have been harmonized */
            @Value("${OAUTH_BASE_URL:https://app.propelo.ai}") String baseUrl,
            @Qualifier("custom") ObjectMapper objectMapper,
            EmailService emailService, TemplateService templateService,
            @Value("${ALTERNATE_OAUTH_BASE_URL:}")String alternateBaseUrl) {
        this.emailService = emailService;
        this.templateService = templateService;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.alternateBaseUrl = alternateBaseUrl;
    }

    private EmailContact getDefaultEmailContact() {
        return EmailContact.builder()
                .name("Propelo")
                .email(FROM_EMAIL)
                .build();
    }

    private String getEncodedToken(String company, User levelOpsUser, String resetToken) throws JsonProcessingException {
        return new String(Base64.getEncoder().encode(
                objectMapper.writeValueAsBytes(
                        ResetToken.builder()
                                .company(company)
                                .token(resetToken)
                                .username(levelOpsUser.getEmail())
                                .build())));
    }

    private String getEmailBody(String template, User levelOpsUser, String company, String encodedEmailToken) {
        return templateService.evaluateTemplate(template, Map.of(
                "name", levelOpsUser.getFirstName(),
                "company", company,
                "user", levelOpsUser.getEmail(),
                "encodedvalue", encodedEmailToken,
                "baseurl", baseUrl,
                "alternateBaseUrl",alternateBaseUrl));
    }

    public void sendPasswordResetMessage(String company, User levelOpsUser, String resetToken)
            throws JsonProcessingException, EmailException {
        String encodedToken = getEncodedToken(company, levelOpsUser, resetToken);
        String content = getEmailBody(RESET_NOTIFICATION_BODY, levelOpsUser, company, encodedToken);
        sendHtmlEmailNotification(RESET_NOTIFICATION_SUBJECT, content, getDefaultEmailContact(),
                levelOpsUser.getEmail());
    }

    public void sendAccountCreationMessage(String company, User levelOpsUser, String resetToken)
            throws JsonProcessingException, EmailException {
        String encodedToken = getEncodedToken(company, levelOpsUser, resetToken);
        String content = getEmailBody(CREATION_NOTIFICATION_BODY, levelOpsUser, company, encodedToken);
        sendHtmlEmailNotification(CREATION_NOTIFICATION_SUBJECT, content, getDefaultEmailContact(),
                levelOpsUser.getEmail());
    }

    private void sendHtmlEmailNotification(String subject, String content, EmailContact from, String userEmail)
            throws EmailException {
        emailService.send(Email.builder()
                .subject(subject)
                .content(content)
                .contentType("text/html")
                .from(from)
                .recipient(userEmail)
                .build());
    }
}
