package io.levelops.commons.databases.converters.cicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

public class CiCdJobRunArtifactConverters {

    public static RowMapper<CiCdJobRunArtifact> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> CiCdJobRunArtifact.builder()
                .id(rs.getString("id"))
                .cicdJobRunId(UUID.fromString(rs.getString("cicd_job_run_id")))
                .input(rs.getBoolean("input"))
                .output(rs.getBoolean("output"))
                .type(rs.getString("type"))
                .location(rs.getString("location"))
                .name(rs.getString("name"))
                .qualifier(rs.getString("qualifier"))
                .hash(rs.getString("hash"))
                .metadata(ParsingUtils.parseJsonObject(objectMapper, "metadata", rs.getString("metadata")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

}
