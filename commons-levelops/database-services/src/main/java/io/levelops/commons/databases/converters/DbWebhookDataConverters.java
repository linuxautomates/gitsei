package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.scm.DbWebhookData;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.integrations.github.models.GithubWebhookEvent;
import org.springframework.jdbc.core.RowMapper;

public class DbWebhookDataConverters {

    public static RowMapper<DbWebhookData> webhookDataRowMapper(ObjectMapper mapper) {
        return (rs, rowNumber) -> DbWebhookData.builder()
                .id(rs.getString("id"))
                .webhookId(rs.getString("webhook_id"))
                .jobId(rs.getString("job_id"))
                .integrationId(rs.getInt("integration_id"))
                .status(rs.getString("status"))
                .webhookEvent(ParsingUtils.parseObject(mapper, "webhook_event", GithubWebhookEvent.class, rs.getString("webhook_event")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .build();
    }

    public static RowMapper<String> webhookJobIdRowMapper() {
        return (rs, rowNumber) -> rs.getString("job_id");
    }
}
