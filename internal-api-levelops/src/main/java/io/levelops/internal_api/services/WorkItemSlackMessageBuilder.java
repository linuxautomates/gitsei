package io.levelops.internal_api.services;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.internal_api.services.handlers.LevelOpsLinkUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class WorkItemSlackMessageBuilder {
    private static final String WI_LINK_FORMAT = "{\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"<%s|%s>\"}}";
    private static final String CHANGE_TICKET_STATUS_OPTION = "{ \"text\": { \"type\": \"plain_text\", \"text\": \"%s\", \"emoji\": true }, \"value\": \"%s\" }";
    private static final String CHANGE_TICKET_STATUS_COMPLETE = "{ \"type\": \"section\", \"text\": { \"type\": \"mrkdwn\", \"text\": \"Change ticket status\" }, \"accessory\": { \"type\": \"static_select\", \"placeholder\": { \"type\": \"plain_text\", \"text\": \"Select an item\", \"emoji\": true }, \"options\": [ %s ] } }";
    private static final String FORWARD_TICKET = "{\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"Forward this ticket\"},\"accessory\": {\"type\": \"multi_conversations_select\",\"placeholder\": {\"type\": \"plain_text\",\"text\": \"Select conversations\",\"emoji\": true}}}";
    private static final String ASSIGN_TICKET = "{\"block_id\":\"assign_snippet_ticket\",\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"`Assign To :`\"},\"accessory\":{\"action_id\":\"assignee_select_snippet_ticket\",\"type\":\"users_select\",\"placeholder\":{\"type\":\"plain_text\",\"text\":\"Select assignee\",\"emoji\":true}}}";

    private String formattedStateName(String state) {
        if(StringUtils.isBlank(state)) {
            return state;
        }
        if("CLOSED".equals(state)) {
            return "Closed";
        } else if("OPEN".equals(state)) {
            return "Open";
        } else if("IN_REVIEW".equals(state)) {
            return "In Review";
        } else if("NEW".equals(state)) {
            return "New";
        } else {
            return state;
        }
    }
    private String buildChangeTicketStatusBlock(List<String> states) {
        List<String> options = states.stream().map(x -> String.format(CHANGE_TICKET_STATUS_OPTION, formattedStateName(x), x)).collect(Collectors.toList());
        String changeTicketStatusBlock = String.format(CHANGE_TICKET_STATUS_COMPLETE, String.join(",", options));
        log.debug("changeTicketStatusBlock = {}", changeTicketStatusBlock);
        return changeTicketStatusBlock;
    }

    private String buildWIAndAssessmentLinkBlock(final String company, final String appBaseUrl, String vanityId, String ticketTitle) {
        String wiLink = LevelOpsLinkUtils.buildWorkItemLink(appBaseUrl, vanityId);
        if(StringUtils.isBlank(wiLink)) {
            return null;
        }
        String ticketTitleSanitized = StringUtils.isNotBlank(ticketTitle) ? ticketTitle : "View Ticket";
        return String.format(WI_LINK_FORMAT, wiLink, ticketTitleSanitized);
    }

    public WorkItemSlackMessages buildInteractiveMessage(final String company, final String appBaseUrl, final WorkItem workItem, final List<String> states) {
        List<String> blocks = new ArrayList<>();
        //Add WI Link Text
        blocks.add(buildWIAndAssessmentLinkBlock(company, appBaseUrl, workItem.getVanityId(), workItem.getTitle()));
        //Add Change Ticket Status Action
        blocks.add(buildChangeTicketStatusBlock(states));
        //Add Forward Ticket Action
        blocks.add(FORWARD_TICKET);
        //Add Assign Ticket Action
        blocks.add(ASSIGN_TICKET);
        String message = "[" + blocks.stream().filter(Objects::nonNull).collect(Collectors.joining(",")) + "]";

        return WorkItemSlackMessages.builder().message(message).build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = WorkItemSlackMessages.WorkItemSlackMessagesBuilder.class)
    public static class WorkItemSlackMessages {
        private final String message;
        private final List<ImmutablePair<UUID,String>> modalMessages;
    }
}
