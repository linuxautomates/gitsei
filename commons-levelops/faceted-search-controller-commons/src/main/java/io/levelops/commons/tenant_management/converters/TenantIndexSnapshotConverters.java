package io.levelops.commons.tenant_management.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.faceted_search.models.IndexType;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.tenant_management.models.JobStatus;
import io.levelops.commons.tenant_management.models.Offsets;
import io.levelops.commons.tenant_management.models.TenantIndexSnapshot;
import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;
import java.util.UUID;

public class TenantIndexSnapshotConverters {

    public static RowMapper<TenantIndexSnapshot> mapTenantTenantIndexSnapshot(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> {
            Offsets latestOffsets = ParsingUtils.parseObject(objectMapper, "latest_offsets", Offsets.class, rs.getString("latest_offsets"));
            //status_updated_at
            TenantIndexSnapshot tenantConfig = TenantIndexSnapshot.builder()
                    .id(rs.getObject("id", UUID.class))
                    .indexTypeConfigId(rs.getObject("index_type_config_id", UUID.class))
                    .ingestedAt(rs.getLong("ingested_at"))
                    .indexName(rs.getString("index_name"))
                    .indexExist(rs.getBoolean("index_exist"))
                    .priority(rs.getInt("priority"))
                    .status(JobStatus.fromString(rs.getString("status")))
                    .statusUpdatedAt( DateUtils.toInstant(rs.getTimestamp("status_updated_at")))
                    .failedAttemptsCount(rs.getInt("failed_attempts_count"))
                    .lastRefreshStartedAt( DateUtils.toInstant(rs.getTimestamp("last_refresh_started_at")))
                    .lastRefreshedAt( DateUtils.toInstant(rs.getTimestamp("last_refreshed_at")))
                    .latestOffsets(latestOffsets)
                    .heartbeat( DateUtils.toInstant(rs.getTimestamp("heartbeat")))
                    .markedForDeletion(rs.getBoolean("marked_for_deletion"))
                    .markedForDeletionAt(DateUtils.toInstant(rs.getTimestamp("marked_for_deletion_at")))
                    .createdAt( DateUtils.toInstant(rs.getTimestamp("created_at")))
                    .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                    .tenantId(rs.getString("tenant_id"))
                    .indexType(IndexType.fromString(rs.getString("index_type")))
                    .build();
            return tenantConfig;
        };
    }
}
