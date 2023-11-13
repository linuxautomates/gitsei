package io.levelops.commons.databases.converters.cicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.cicd.DbCiCdPushedArtifact;
import io.levelops.commons.databases.models.database.cicd.DbCiCdPushedJobRunParam;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.RowMapper;

import java.util.stream.Collectors;

public class CiCdPushedConverters {

    public static RowMapper<DbCiCdPushedArtifact> pushedArtifactsRowMapper(ObjectMapper mapper) {
        return (rs, rowNumber) -> DbCiCdPushedArtifact.builder()
                .id(rs.getString("id"))
                .integrationId(String.valueOf(rs.getInt("integration_id")))
                .repository(rs.getString("repository"))
                .jobName(rs.getString("job_name"))
                .jobRunNumber(rs.getLong("job_run_number"))
                .artifacts(CollectionUtils.emptyIfNull(ParsingUtils.parseJsonList(mapper, "artifacts", rs.getString("artifacts")))
                        .stream().map(artifactMap ->
                                DbCiCdPushedArtifact.Artifact.builder()
                                        .name(artifactMap.containsKey("name") ? artifactMap.get("name").toString() : null)
                                        .location(artifactMap.containsKey("location") ? artifactMap.get("location").toString() : null)
                                        .digest(artifactMap.containsKey("digest") ? artifactMap.get("digest").toString() : null)
                                        .tag(artifactMap.containsKey("tag") ? artifactMap.get("tag").toString() : null)
                                        .artifactCreatedAt(artifactMap.containsKey("artifactCreatedAt") ? DateUtils.parseDateTimeToDate(artifactMap.get("artifactCreatedAt").toString()) : null)
                                        .type(artifactMap.containsKey("type") ? artifactMap.get("type").toString() : null)
                                        .build()).collect(Collectors.toList())
                )
                .build();
    }

    public static RowMapper<DbCiCdPushedJobRunParam> pushedParamsRowMapper(ObjectMapper mapper) {
        return (rs, rowNumber) -> DbCiCdPushedJobRunParam.builder()
                .id(rs.getString("id"))
                .integrationId(String.valueOf(rs.getInt("integration_id")))
                .repository(rs.getString("repository"))
                .jobName(rs.getString("job_name"))
                .jobRunNumber(rs.getLong("job_run_number"))
                .jobRunParams(CollectionUtils.emptyIfNull(ParsingUtils.parseJsonList(mapper, "job_run_params", rs.getString("job_run_params")))
                        .stream().map(jobRunMap ->
                                DbCiCdPushedJobRunParam.JobRunParam.builder()
                                        .name(jobRunMap.containsKey("name") ? jobRunMap.get("name").toString() : null)
                                        .type(jobRunMap.containsKey("type") ? jobRunMap.get("type").toString() : null)
                                        .value(jobRunMap.containsKey("value") ? jobRunMap.get("value").toString() : null)
                                        .build()).collect(Collectors.toList())
                )
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }
}
