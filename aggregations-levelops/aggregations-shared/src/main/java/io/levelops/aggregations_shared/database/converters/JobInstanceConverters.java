package io.levelops.aggregations_shared.database.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.utils.CompressionUtils;
import io.levelops.aggregations_shared.utils.DatabaseUtils;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobInstancePayload;
import io.levelops.commons.etl.models.JobMetadata;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.job_progress.StageProgressDetail;
import io.levelops.commons.jackson.ParsingUtils;
import org.springframework.jdbc.core.RowMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class JobInstanceConverters {
    public static RowMapper<DbJobInstance> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> {
            try {
                return buildJobDefinition(rs, objectMapper);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static DbJobInstance buildJobDefinition(ResultSet rs, ObjectMapper objectMapper) throws SQLException, IOException {
        JobInstancePayload payload = null;
        // If the column payload exists but it's empty this will set it to an empty list
        // However if the column payload does not exist then it will set it to null
        if (DatabaseUtils.doesColumnExist("payload", rs)) {
            String payloadStr = CompressionUtils.decompress(rs.getBytes("payload"));
            payload = ParsingUtils.parseObject(objectMapper, "payload", JobInstancePayload.class, payloadStr);
        }
        return DbJobInstance.builder()
                .id((UUID) rs.getObject("id"))
                .jobDefinitionId((UUID) rs.getObject("job_definition_id"))
                .instanceId(rs.getInt("instance_id"))
                .workerId(rs.getString("worker_id"))
                .status(JobStatus.fromString(rs.getString("status")))
                .scheduledStartTime(DateUtils.toInstant(rs.getTimestamp("scheduled_start_time")))
                .startTime(DateUtils.toInstant(rs.getTimestamp("start_time")))
                .priority(JobPriority.fromInteger(rs.getInt("priority")))
                .attemptMax(rs.getInt("attempt_max"))
                .attemptCount(rs.getInt("attempt_count"))
                .timeoutInMinutes(rs.getLong("timeout_in_minutes"))
                .aggProcessorName(rs.getString("agg_processor_name"))
                .lastHeartbeat(DateUtils.toInstant(rs.getTimestamp("last_heartbeat")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .statusChangedAt(DateUtils.toInstant(rs.getTimestamp("status_changed_at")))
                .metadata(ParsingUtils.parseObject(objectMapper, "metadata", JobMetadata.class, rs.getString("metadata")))
                .progress(ParsingUtils.parseMap(objectMapper, "progress", String.class, Integer.class, rs.getString("progress")))
                .progressDetails(ParsingUtils.parseMap(objectMapper, "progress_details", String.class, StageProgressDetail.class, rs.getString("progress_details")))
                .payload(payload)
                .payloadGcsFilename(rs.getString("payload_gcs_filename"))
                .isFull(rs.getBoolean("is_full"))
                .isReprocessing(rs.getBoolean("is_reprocessing"))
                .tags(Set.copyOf(DatabaseUtils.fromSqlArray(rs.getArray("tags"), String.class).collect(Collectors.toList())))
                .build();
    }
}
