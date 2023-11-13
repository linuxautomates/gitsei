package io.levelops.internal_api.services;

import io.levelops.commons.databases.models.database.MessageTemplate;
import io.levelops.commons.databases.models.database.questionnaire.Answer.Comment;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface MentionsService {

    static final Pattern mentionPattern = Pattern
        .compile("\\@\\[(?=[a-zA-Z0-9@._%+-]{6,254}\\b)([a-zA-Z0-9._%+-]{1,64}@(?:[a-zA-Z0-9-]{1,63}\\.){1,8}[a-zA-Z]{2,63}\\b)\\]");

    void notifyMentionsInText(final String company, final String commentator, final String text, final MessageTemplate template, final Map<String, Object> values) throws IOException;

    void notifyMentionsInText(final String company, final String commentator, final Comment comment, final MessageTemplate template, final Map<String, Object> values) throws IOException;

    void notifyMentionsInText(final String company, final String commentator, final Map<String, List<Comment>> comments, final MessageTemplate template, final Map<String, Object> values) throws IOException;

    void notifyMentionsInText(final String company, final String commentator, final Map<String, List<Comment>> currentComments, final Map<String, List<Comment>> previousComments, final MessageTemplate template, final Map<String, Object> values) throws IOException;

    /**
     * Extracts any mention in the message with the pattern @[someone@some.company].
     * 
     * @param message text to analyze[]
     * @param commentator email of the person who submitted the message.
     * @return a set of email addresses of people mentioned in the message.
     */
    default Set<String> extractMentionsFromText(final String message, final String commentator) {
        return mentionPattern.matcher(message).results()
            .map(r -> r.group(1))
            .filter(recipient -> !commentator.equalsIgnoreCase(recipient))
            .collect(Collectors.toSet());
    }
}