package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.models.filters.ZendeskTicketsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class DbZendeskTicketConverters {

    public static RowMapper<DbZendeskTicket> listRowMapper() {
        return ((rs, rowNum) -> buildTicket(rs));
    }

    public static ResultSetExtractor<DbZendeskTicket> rowMapper() {
        return (rs -> {
            if (!rs.next())
                return null;
            return buildTicket(rs);
        });
    }

    public static ResultSetExtractor<String> idMapper() {
        return (rs -> {
            if (!rs.next())
                return null;
            return rs.getString("id");
        });
    }

    public static RowMapper<DbAggregationResult> aggRowMapper(String key, ZendeskTicketsFilter.CALCULATION CALCULATION,
                                                              Optional<String> additionalKey) {
        return ((rs, rowNum) -> {
            if (ZendeskTicketsFilter.CALCULATION.ticket_count == CALCULATION) {
                return DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .totalTickets(rs.getLong("ct"))
                        .build();
            } else {
                return DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .totalTickets(rs.getLong("ct"))
                        .max(rs.getLong("mx"))
                        .min(rs.getLong("mn"))
                        .median(rs.getLong("percentile_disc"))
                        .build();
            }
        });
    }

    private static DbZendeskTicket buildTicket(ResultSet rs) throws SQLException {
        return DbZendeskTicket.builder()
                .id(rs.getString("id"))
                .ticketId(rs.getLong("ticket_id"))
                .integrationId(String.valueOf(rs.getInt("integration_id")))
                .brand(rs.getString("brand"))
                .type(rs.getString("type"))
                .subject(rs.getString("subject"))
                .priority(rs.getString("priority"))
                .status(rs.getString("status"))
                .recipientEmail(rs.getString("recipient"))
                .requesterEmail(rs.getString("requester"))
                .submitterEmail(rs.getString("submitter"))
                .assigneeEmail(rs.getString("assignee"))
                .organizationName(rs.getString("organization"))
                .hops(rs.getInt("hops"))
                .bounces(rs.getInt("bounces"))
                .customFields(ParsingUtils.parseJsonObject(DefaultObjectMapper.get(),
                        "zendesk_ticket",
                        rs.getString("custom_fields")))
                .ingestedAt(rs.getTimestamp("ingested_at"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .ticketCreatedAt(rs.getTimestamp("ticket_created_at"))
                .ticketUpdatedAt(rs.getTimestamp("ticket_updated_at"))
                .dueAt(rs.getTimestamp("due_at"))
                .reopens(rs.getInt("reopens"))
                .replies(rs.getInt("replies"))
                .assignedAt(rs.getTimestamp("assigned_at"))
                .solvedAt(rs.getTimestamp("solved_at"))
                .firstResolutionTime(rs.getLong("first_resolution_time"))
                .firstReplyTime(rs.getLong("first_reply_time"))
                .fullResolutionTime(rs.getLong("full_resolution_time"))
                .agentWaitTime(rs.getLong("agent_wait_time"))
                .requesterWaitTime(rs.getLong("requester_wait_time"))
                .assigneeUpdatedAt(rs.getTimestamp("assignee_updated_at"))
                .jiraIssueKeys(List.of(ArrayUtils.nullToEmpty(StringUtils.split(
                        rs.getString("jira_issues"), ","))))
                .externalId(rs.getString("external_id"))
                .build();
    }
}
