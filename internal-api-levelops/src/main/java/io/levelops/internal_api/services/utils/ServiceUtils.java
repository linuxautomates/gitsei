package io.levelops.internal_api.services.utils;

import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.MessageTemplate;
import io.levelops.commons.databases.models.database.TicketTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServiceUtils {

    @NotNull
    public static List<MessageTemplate> getMessageTemplates(List<MessageTemplate> records, List<String> comparableNotifications,
                                                            EventType eventType, TicketTemplate ticketTemplate) {
        List<MessageTemplate> messageTemplates = records.stream()
                .filter(template -> ticketTemplate.getMessageTemplateIds().contains(template.getId()))
                .filter(template -> template.getEventType() == eventType)
                .filter(template -> comparableNotifications.contains(template.getType().toString().toLowerCase()))
                .collect(Collectors.toList());

        List<MessageTemplate.TemplateType> templateTypes = messageTemplates.stream()
                .map(MessageTemplate::getType)
                .collect(Collectors.toList());

        if (comparableNotifications.contains(MessageTemplate.TemplateType.EMAIL.toString().toLowerCase()) &&
                !templateTypes.contains(MessageTemplate.TemplateType.EMAIL)) {
            Optional<MessageTemplate> messageTemplateOptional = getDefaultMessageTemplate(records, eventType, MessageTemplate.TemplateType.EMAIL);
            messageTemplateOptional.ifPresent(messageTemplates::add);
        }

        if (comparableNotifications.contains(MessageTemplate.TemplateType.SLACK.toString().toLowerCase()) &&
                !templateTypes.contains(MessageTemplate.TemplateType.SLACK)) {
            Optional<MessageTemplate> messageTemplateOptional = getDefaultMessageTemplate(records, eventType, MessageTemplate.TemplateType.SLACK);
            messageTemplateOptional.ifPresent(messageTemplates::add);
        }
        return messageTemplates;
    }

    private static Optional<MessageTemplate> getDefaultMessageTemplate(List<MessageTemplate> records, EventType eventType,
                                                                       MessageTemplate.TemplateType templateType) {
        return records.stream()
                .filter(MessageTemplate::isDefaultTemplate)
                .filter(template -> template.getEventType() == eventType)
                .filter(template -> template.getType().equals(templateType))
                .findFirst();
    }

    @NotNull
    public static List<MessageTemplate> getMessageTemplates(List<MessageTemplate> records, List<String> comparableNotifications,
                                                            EventType eventType) {
        return records.stream()
                .filter(MessageTemplate::isDefaultTemplate)
                .filter(template -> template.getEventType() == eventType)
                .filter(template -> comparableNotifications.contains(template.getType().toString().toLowerCase()))
                .collect(Collectors.toList());
    }
}
