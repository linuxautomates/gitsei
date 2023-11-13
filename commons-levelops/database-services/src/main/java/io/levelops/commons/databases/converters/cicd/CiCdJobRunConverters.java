package io.levelops.commons.databases.converters.cicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class CiCdJobRunConverters {
    public static RowMapper<CICDJobRun> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> buildCiCdJobRun(objectMapper, rs);
    }

    public static CICDJobRun buildCiCdJobRun(ObjectMapper mapper, ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id");
        UUID cicdJobId = (UUID) rs.getObject("cicd_job_id");
        Long jobRunNumber = rs.getLong("job_run_number");
        String status = rs.getString("status");
        Instant startTime = DateUtils.toInstant(rs.getTimestamp("start_time"));
        Integer duration = rs.getInt("duration");
        Instant endTime = DateUtils.toInstant(rs.getTimestamp("end_time"));
        String cicdUserId = rs.getString("cicd_user_id");
        String[] scmCommitIds = rs.getArray("scm_commit_ids") != null
                ? (String[]) rs.getArray("scm_commit_ids").getArray()
                : new String[0];
        CICDJobRun.Source source = CICDJobRun.Source.fromString(rs.getString("source"));
        String referenceId = rs.getString("reference_id");
        Boolean ci = rs.getBoolean("ci");
        Boolean cd = rs.getBoolean("cd");
        Map<String, Object> metadata = MapUtils.emptyIfNull(
                ParsingUtils.parseJsonObject(mapper, "metadata", rs.getString("metadata")));
        Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
        Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));
        CICDJobRun aggregationRecord = CICDJobRun.builder()
                .id(id)
                .cicdJobId(cicdJobId)
                .jobRunNumber(jobRunNumber)
                .status(status)
                .startTime(startTime)
                .duration(duration)
                .endTime(endTime)
                .cicdUserId(cicdUserId)
                .source(source)
                .referenceId(referenceId)
                .logGcspath(rs.getString("log_gcspath"))
                .scmCommitIds(scmCommitIds.length > 0 ? Arrays.asList(scmCommitIds) : Collections.emptyList())
                .ci(ci)
                .cd(cd)
                .metadata(metadata)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
        return aggregationRecord;
    }
}
