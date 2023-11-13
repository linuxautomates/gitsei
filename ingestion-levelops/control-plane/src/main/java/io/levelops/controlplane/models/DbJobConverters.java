package io.levelops.controlplane.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.functional.IngestionFailure;
import io.levelops.controlplane.models.jsonb.JsonUtils;
import io.levelops.controlplane.utils.DatabaseUtils;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.controlplane.JobStatus;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class DbJobConverters {

    public static CreateJobRequest convertDbJobToRequest(DbJob job) {
        return CreateJobRequest.builder()
                .jobId(job.getId())
                .controllerName(job.getControllerName())
                .query(job.getQuery())
                .callbackUrl(job.getCallbackUrl())
                .tags(job.getTags())
                .intermediateState(job.getIntermediateState())
                .attemptCount(job.getAttemptCount())
                .tenantId(job.getTenantId())
                .integrationId(job.getIntegrationId())
                .build();
    }

    public static RowMapper<DbJob> jobRowMapper(ObjectMapper objectMapper) {
        return jobRowMapper(objectMapper, true, true);
    }

    public static RowMapper<DbJob> jobRowMapper(ObjectMapper objectMapper, boolean includeQuery, boolean includeResult) {
        return (rs, rowNum) -> {
            List<IngestionFailure> ingestionFailures = null;
            if(includeResult) {
                String ingestionFailureStr = rs.getString("failures");
                if(StringUtils.isNotEmpty(ingestionFailureStr)) {
                    try {
                        ingestionFailures = objectMapper.readValue(ingestionFailureStr, new TypeReference<>() {});
                    } catch (JsonProcessingException e) {
                        log.error("Error deserializing ingestion failures! payload = {}", ingestionFailureStr, e);
                    }
                }
            }

            return DbJob.builder()
                    .id(rs.getString("id"))
                    .agentId(rs.getString("agent_id"))
                    .status(JobStatus.fromString(rs.getString("status")))
                    .tenantId(rs.getString("tenant_id"))
                    .integrationId(rs.getString("integration_id"))
                    .tags(DatabaseUtils.fromSqlArray(rs.getArray("tags"), String.class).collect(Collectors.toSet()))
                    .reserved(rs.getBoolean("reserved"))
                    .level(rs.getInt("level"))
                    .parentId(rs.getString("parent_id"))
                    .attemptCount(rs.getInt("attempt_count"))
                    .attemptMax(rs.getInt("attempt_max"))
                    .query(includeQuery ? JsonUtils.parseJsonObjectField(objectMapper, rs, "query") : null)
                    .callbackUrl(rs.getString("callback_url"))
                    .controllerName(rs.getString("controller_name"))
                    .createdAt(rs.getLong("created_at"))
                    .statusChangedAt(rs.getLong("status_changed_at"))
                    .result(includeResult ? JsonUtils.parseJsonObjectField(objectMapper, rs, "result") : null)
                    .intermediateState(includeResult ? JsonUtils.parseJsonObjectField(objectMapper, rs, "intermediate_state") : null)
                    .error(includeResult ? JsonUtils.parseJsonObjectField(objectMapper, rs, "error") : null)
                    .ingestionFailures(ingestionFailures)
                    .build();
        };
    }

}
