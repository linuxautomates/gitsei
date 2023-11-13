package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.cicd.CICDJobRunCommits;
import io.levelops.commons.dates.DateUtils;
import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CICDJobRunCommitsConverters {
    public static RowMapper<CICDJobRunCommits> rowMapper() {
        return (rs, rowNumber) -> {
            List<String> scmCommitIds =  (rs.getArray("scm_commit_ids") != null && rs.getArray("scm_commit_ids").getArray() != null)
                    ? Arrays.asList((String[]) rs.getArray("scm_commit_ids").getArray()) : List.of();

            Instant jobRunUpdatedAt = DateUtils.toInstant(rs.getTimestamp("job_run_updated_at"));


            return CICDJobRunCommits.builder()
                    .id(rs.getObject("id", UUID.class))
                    .scmCommitIds(scmCommitIds)
                    .cicdJobId(rs.getObject("job_id", UUID.class))
                    .jobScmUrl(rs.getString("job_scm_url"))
                    .jobRunUpdatedAt(jobRunUpdatedAt)
                    .build();
        };
    }
}
