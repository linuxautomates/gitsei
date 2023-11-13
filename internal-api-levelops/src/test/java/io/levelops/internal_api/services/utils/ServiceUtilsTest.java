package io.levelops.internal_api.services.utils;

import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.MessageTemplate;
import io.levelops.commons.databases.models.database.TicketTemplate;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServiceUtilsTest {

    @Test
    public void test() {
        MessageTemplate messageTemplate1 = MessageTemplate.builder()
                .id("1")
                .name("Email Template")
                .type(MessageTemplate.TemplateType.EMAIL)
                .emailSubject("my_test")
                .message("Please_fill_out")
                .eventType(EventType.SMART_TICKET_CREATED)
                .defaultTemplate(false)
                .system(false)
                .botName("")
                .build();

        MessageTemplate messageTemplate2 = MessageTemplate.builder()
                .id("2")
                .name("Slack Template")
                .type(MessageTemplate.TemplateType.SLACK)
                .emailSubject("my_test")
                .message("Please_fill_out")
                .eventType(EventType.SMART_TICKET_CREATED)
                .defaultTemplate(false)
                .system(false)
                .botName("")
                .build();

        TicketTemplate ticketTemplate1 = TicketTemplate.builder()
                .notifyBy(Map.of(EventType.ALL, List.of("EMAIL")))
                .messageTemplateIds(List.of("1"))
                .build();

        TicketTemplate ticketTemplate2 = TicketTemplate.builder()
                .notifyBy(Map.of(EventType.ALL, List.of("SLACK")))
                .messageTemplateIds(List.of("2"))
                .build();

        TicketTemplate ticketTemplate3 = TicketTemplate.builder()
                .notifyBy(Map.of(EventType.ALL, List.of("EMAIL", "SLACK")))
                .messageTemplateIds(List.of("3", "4"))
                .build();

        EventType eventType = EventType.SMART_TICKET_CREATED;

        var notifications = ticketTemplate1.getNotifyBy().getOrDefault(eventType, ticketTemplate1.getNotifyBy().getOrDefault(EventType.ALL, null));
        var comparableNotifications = notifications.stream().map(String::toLowerCase).collect(Collectors.toList());

        List<MessageTemplate> messageTemplates = ServiceUtils.getMessageTemplates(List.of(messageTemplate1), comparableNotifications, eventType, ticketTemplate1);
        Assertions.assertTrue(CollectionUtils.isNotEmpty(messageTemplates));

        notifications = ticketTemplate1.getNotifyBy().getOrDefault(eventType, ticketTemplate2.getNotifyBy().getOrDefault(EventType.ALL, null));
        comparableNotifications = notifications.stream().map(String::toLowerCase).collect(Collectors.toList());

        messageTemplates = ServiceUtils.getMessageTemplates(List.of(messageTemplate2), comparableNotifications, eventType, ticketTemplate2);
        Assertions.assertTrue(CollectionUtils.isNotEmpty(messageTemplates));

        notifications = ticketTemplate3.getNotifyBy().getOrDefault(eventType, ticketTemplate2.getNotifyBy().getOrDefault(EventType.ALL, null));
        comparableNotifications = notifications.stream().map(String::toLowerCase).collect(Collectors.toList());

        messageTemplates = ServiceUtils.getMessageTemplates(List.of(messageTemplate2), comparableNotifications, eventType, ticketTemplate3);
        Assertions.assertTrue(CollectionUtils.isEmpty(messageTemplates));

    }
}
