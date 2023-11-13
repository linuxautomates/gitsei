package io.levelops.commons.databases.models.database.zendesk;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.integrations.zendesk.converters.ZendeskCustomFieldConverter;
import io.levelops.integrations.zendesk.models.*;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbZendeskTicket.DbZendeskTicketBuilder.class)
@Log4j2
public class DbZendeskTicket {

    private static final String UNASSIGNED = "_UNASSIGNED_";
    static final String DTYPE_DATE = "date";
    static final String DTYPE_DATETIME = "datetime";

    @JsonProperty
    String id;

    @JsonProperty("ticket_id")
    Long ticketId;

    @JsonProperty("brand")
    String brand;

    @JsonProperty("ticket_url")
    String ticketUrl;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("custom_fields")
    Map<String, Object> customFields;

    @JsonProperty("external_id")
    String externalId;

    @JsonProperty
    String type;

    @JsonProperty
    String subject;

    @JsonProperty
    String description;

    @JsonProperty
    String priority;

    @JsonProperty
    String status;

    @JsonProperty("recipient_email")
    String recipientEmail;

    @JsonProperty("requester_email")
    String requesterEmail;

    @JsonProperty("submitter_email")
    String submitterEmail;

    @JsonProperty("assignee_email")
    String assigneeEmail;

    @JsonProperty("organization_name")
    String organizationName;

    @JsonProperty("problem_id")
    String problemId;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty("ticket_created_at")
    Date ticketCreatedAt;

    @JsonProperty("ticket_updated_at")
    Date ticketUpdatedAt;

    @JsonProperty
    Integer hops;

    @JsonProperty
    Integer bounces;

    @JsonProperty
    Integer reopens;

    @JsonProperty
    Integer replies;

    @JsonProperty("assignee_updated_at")
    Date assigneeUpdatedAt;

    @JsonProperty("requester_updated_at")
    Date requesterUpdatedAt;

    @JsonProperty("status_updated_at")
    Date statusUpdatedAt;

    @JsonProperty("initially_assigned_at")
    Date initiallyAssignedAt;

    @JsonProperty("assigned_at")
    Date assignedAt;

    @JsonProperty("solved_at")
    Date solvedAt;

    @JsonProperty("due_at")
    Date dueAt;

    @JsonProperty("last_comment_added_at")
    Date lastCommentAddedAt;

    @JsonProperty("first_reply_time")
    Long firstReplyTime;

    @JsonProperty("first_resolution_time")
    Long firstResolutionTime;

    @JsonProperty("full_resolution_time")
    Long fullResolutionTime;

    @JsonProperty("agent_wait_time")
    Long agentWaitTime;

    @JsonProperty("requester_wait_time")
    Long requesterWaitTime;

    @JsonProperty("on_hold_time")
    Long onHoldTime;

    @JsonProperty("jira_issue_keys")
    List<String> jiraIssueKeys;

    @JsonProperty("ingested_at")
    Date ingestedAt;

    public static DbZendeskTicket fromTicket(@Nonnull Ticket ticket,
                                             String integrationId,
                                             Date fetchTime,
                                             List<IntegrationConfig.ConfigEntry> customFieldConfig,
                                             List<DbZendeskField> customFieldProperties) {
        Date truncatedDate = DateUtils.truncate(fetchTime, Calendar.DATE);
        TicketMetric metric = ticket.getTicketMetric();
        Map<String, Object> customFields = new HashMap<>();
        if (CollectionUtils.isNotEmpty(customFieldConfig)
                && ticket.getCustomFields() != null) {
            for (IntegrationConfig.ConfigEntry entry : customFieldConfig) {
                if (entry.getKey() == null) continue;
                Optional<DbZendeskField> field = customFieldProperties
                                                    .stream()
                                                    .filter(c -> c.getFieldId() == Long.parseLong(entry.getKey()))
                                                    .findFirst();

                Object val = null;
                for (Ticket.ZendeskField ticketField: ticket.getCustomFields()) {
                    if (ticketField.getId() == Long.parseLong(entry.getKey())) {
                        val = ticketField.getValue();
                        break;
                    }
                }

                if (field.isPresent()) {
                    if (field.get().getFieldType().equalsIgnoreCase(DTYPE_DATE)){
                        try {
                            customFields.put(entry.getKey(), ZendeskCustomFieldConverter.parseDate(val));
                        } catch (ParseException e) {
                            log.warn("unable to parse date field {}", val, e);
                        }
                    } else if (field.get().getFieldType().equalsIgnoreCase(DTYPE_DATETIME)) {
                        try {
                            customFields.put(entry.getKey(), ZendeskCustomFieldConverter.parseDateTime(val));
                        } catch (ParseException e) {
                            log.warn("unable to parse datetime field {}", val, e);
                        }
                    }

                    if (val instanceof List && !((List<?>) val).isEmpty()) {
                        customFields.put(entry.getKey(), ((List<?>) val).stream()
                                .map(ZendeskCustomFieldConverter::parseValue)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()));
                    } else {
                        String value = ZendeskCustomFieldConverter.parseValue(val);
                        if (StringUtils.isEmpty(value)) continue;
                        if (StringUtils.isNotEmpty(entry.getDelimiter()))
                            customFields.put(entry.getKey(), List.of(value.split(entry.getDelimiter()))
                                    .stream()
                                    .map(ZendeskCustomFieldConverter::parseValue)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList()));
                        else
                            customFields.put(entry.getKey(), value);
                    }
                }
            }
        }
        DbZendeskTicket.DbZendeskTicketBuilder builder = DbZendeskTicket.builder()
                .ticketId(ticket.getId())
                .brand(ticket.getBrand() != null ? ticket.getBrand().getName() : UNASSIGNED)
                .integrationId(integrationId)
                .customFields(customFields)
                .ingestedAt(truncatedDate)
                .externalId(ticket.getExternalId())
                .type(MoreObjects.firstNonNull(ticket.getType(), UNASSIGNED))
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .priority(MoreObjects.firstNonNull(ticket.getPriority(), UNASSIGNED))
                .status(MoreObjects.firstNonNull(ticket.getStatus(), UNASSIGNED))
                .recipientEmail(ticket.getRecipient())
                .requesterEmail(getEmailOrUnassigned(ticket.getRequester()))
                .submitterEmail(getEmailOrUnassigned(ticket.getSubmitter()))
                .assigneeEmail(getEmailOrUnassigned(ticket.getAssignee()))
                .organizationName(ticket.getOrganization() != null ? ticket.getOrganization().getName() : UNASSIGNED)
                .problemId(String.valueOf(ticket.getProblemId()))
                .dueAt(ticket.getDueAt())
                .ticketCreatedAt(ticket.getCreatedAt())
                .ticketUpdatedAt(ticket.getUpdatedAt())
                .ticketUrl(ticket.getUrl())
                .jiraIssueKeys(ListUtils.emptyIfNull(ticket.getJiraLinks())
                        .stream()
                        .map(JiraLink::getIssueKey)
                        .collect(Collectors.toList()))
                .hops(Optional.ofNullable(metric).map(TicketMetric::getAssigneeStations).orElse(0))
                .reopens(Optional.ofNullable(metric).map(TicketMetric::getReopens).orElse(0))
                .replies(Optional.ofNullable(metric).map(TicketMetric::getReplies).orElse(0));
        if (metric != null) {
            builder.assigneeUpdatedAt(metric.getAssigneeUpdatedAt())
                    .requesterUpdatedAt(metric.getRequesterUpdatedAt())
                    .statusUpdatedAt(metric.getStatusUpdatedAt())
                    .initiallyAssignedAt(metric.getInitiallyAssignedAt())
                    .assignedAt(metric.getAssignedAt())
                    .solvedAt(metric.getSolvedAt())
                    .lastCommentAddedAt(metric.getLastCommentAddedAt())
                    .firstReplyTime(getCalendarTime(metric.getReplyTimeInMins()))
                    .firstResolutionTime(getCalendarTime(metric.getFirstResolutionTimeInMins()))
                    .fullResolutionTime(getCalendarTime(metric.getFirstResolutionTimeInMins()))
                    .agentWaitTime(getCalendarTime(metric.getAgentWaitTimeInMins()))
                    .requesterWaitTime(getCalendarTime(metric.getRequesterWaitTimeInMins()))
                    .onHoldTime(getCalendarTime(metric.getOnHoldTimeInMins()));
        }
        return builder.build();
    }

    private static String getEmailOrUnassigned(User user) {
        return user != null && StringUtils.isNotEmpty(user.getEmail()) ? user.getEmail() : UNASSIGNED;
    }

    private static Long getCalendarTime(ZendeskDuration duration) {
        return duration != null && duration.getCalendarTimeInMins() != null ? (long) (duration.getCalendarTimeInMins() * 60) : null;
    }
}
