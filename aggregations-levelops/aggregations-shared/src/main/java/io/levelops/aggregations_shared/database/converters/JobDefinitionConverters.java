package io.levelops.aggregations_shared.database.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.jackson.ParsingUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class JobDefinitionConverters {
    public static RowMapper<DbJobDefinition> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> {
            return buildJobDefinition(rs, objectMapper);
        };
    }

    private static DbJobDefinition buildJobDefinition(ResultSet rs, ObjectMapper objectMapper) throws SQLException {
        return DbJobDefinition.builder()
                .id((UUID) rs.getObject("id"))
                .tenantId(rs.getString("tenant_id"))
                .integrationId(rs.getString("integration_id"))
                .integrationType(rs.getString("integration_type"))
                .ingestionTriggerId(rs.getString("ingestion_trigger_id"))
                .jobType(JobType.valueOf(rs.getString("job_type")))
                .isActive(rs.getBoolean("is_active"))
                .defaultPriority(JobPriority.fromInteger(rs.getInt("default_priority")))
                .attemptMax(rs.getInt("attempt_max"))
                .retryWaitTimeInMinutes(rs.getInt("retry_wait_time_minutes"))
                .timeoutInMinutes(rs.getLong("timeout_in_minutes"))
                .frequencyInMinutes(rs.getInt("frequency_in_minutes"))
                .fullFrequencyInMinutes(rs.getInt("full_frequency_in_minutes"))
                .aggProcessorName(rs.getString("agg_processor_name"))
                .lastIterationTs(DateUtils.toInstant(rs.getTimestamp("last_iteration_ts")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .metadata(ParsingUtils.parseJsonObject(objectMapper, "metadata", rs.getString("metadata")))
                .build();
    }
}
