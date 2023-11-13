package io.levelops.internal_api.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.MessageTemplate;
import io.levelops.commons.databases.models.database.questionnaire.Answer.Comment;
import io.levelops.notification.services.NotificationService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

@Log4j2
@Service
public class LocalMentionsService implements MentionsService {
    private final NotificationService notificationService;

    public LocalMentionsService(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void notifyMentionsInText(
        @Nonnull final String company,
        @Nonnull final String commentator,
        @Nonnull final String text,
        @Nonnull final MessageTemplate template,
        final Map<String, Object> values) throws IOException  {
        extractMentionsFromText(text, commentator)
            .forEach(recipientEmail -> {
                try {
                    Map<String, Object> mValues = new HashMap<>();
                    if (values != null) {
                        mValues.putAll(values);
                    }
                    mValues.put("text", text);
                    log.info("Sending mention notification to {}", recipientEmail);
                    notificationService.sendNotification(company, template, recipientEmail, mValues);
                } catch (IOException e) {
                    log.error("Unable to notify mention for {}", recipientEmail, e);
                }
            });
    }

    @Override
    public void notifyMentionsInText(
        @Nonnull final String company,
        @Nonnull final String commentator,
        @Nonnull final Comment comment,
        @Nonnull final MessageTemplate template,
        final Map<String, Object> values) throws IOException {
        notifyMentionsInText(company, MoreObjects.firstNonNull(comment.getUser(), commentator), comment.getMessage(), template, values);
    }

    @Override
    public void notifyMentionsInText(
        @Nonnull final String company,
        @Nonnull final String commentator,
        @Nonnull final Map<String, List<Comment>> currentComments, 
        @Nonnull final MessageTemplate template,
        final Map<String, Object> values) throws IOException  {
        notifyMentionsInText(company, commentator, currentComments, null, template, values);
    }

    @Override
    public void notifyMentionsInText(
        @Nonnull final String company,
        @Nonnull final String commentator,
        @Nonnull final Map<String, List<Comment>> currentComments,
        final Map<String, List<Comment>> previousComments,
        @Nonnull final MessageTemplate template,
        final Map<String, Object> values) throws IOException  {
        // compare current with previous
        // get new comments
        // get previous comments
        currentComments.entrySet().stream()
            .filter(entry -> entry.getValue() != null && entry.getValue().stream()
                .filter(comment -> {
                    return comment != null && StringUtils.isNotBlank(comment.getMessage());
                })
                .findAny().isPresent())
            .flatMap(entry -> {
                // Process all cases
                // if there are no previous comments
                if (previousComments == null) {
                    return entry.getValue().stream();
                }
                // - new comments in a section previously without comments
                if (previousComments.get(entry.getKey()) == null) { 
                    // send all mentions
                    return entry.getValue().stream()
                        .filter(comment -> comment != null && StringUtils.isNotBlank(comment.getMessage()));
                }
                // sections with previous comments
                else {
                    // get new or changed comments
                    return entry.getValue().stream()
                        .filter(comment -> {
                            if (StringUtils.isBlank(comment.getMessage())) {
                                return false;
                            }
                            // skip existing
                            return !previousComments.get(entry.getKey()).stream()
                                // if the previous comment is equals to any of the current comments then skip
                                .filter(previous -> comment.getMessage().equalsIgnoreCase(previous.getMessage()))
                                // if the comment doesn't match any of the previous ones then is a new or a changed comment
                                .findAny()
                                .isPresent();
                        });
                }
            })
            // send notifications (at this time we are not allowing comments edition so only new comments are sent, 
            // when we edit comments we need to be able to identify changes/new mentions in the same comment... which is not happening now)
            .forEach(comment -> {
                try {
                    notifyMentionsInText(company, commentator, comment, template, values);
                } catch (IOException e) {
                    log.error("Unable to send notification for the comment: {}", comment, e);
                }
            });
    }
    
}