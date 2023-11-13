package io.levelops.commons.databases.converters.cicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifactMapping;
import io.levelops.commons.dates.DateUtils;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

public class CiCdJobRunArtifactMappingConverters {

    public static RowMapper<CiCdJobRunArtifactMapping> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> CiCdJobRunArtifactMapping.builder()
                .id(rs.getString("id"))
                .cicdJobRunId1(UUID.fromString(rs.getString("cicd_job_run_id1")))
                .cicdJobRunId2(UUID.fromString(rs.getString("cicd_job_run_id2")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

}
