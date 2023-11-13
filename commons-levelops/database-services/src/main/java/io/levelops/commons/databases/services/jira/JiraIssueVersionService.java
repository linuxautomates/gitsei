package io.levelops.commons.databases.services.jira;

import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.utils.TransactionCallback;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_VERSIONS;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_PROJECTS;

@Log4j2
@Service
public class JiraIssueVersionService {

    private final NamedParameterJdbcTemplate template;

    public JiraIssueVersionService(DataSource dataSource) {
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    public boolean insertJiraVersion(String company, DbJiraVersion version) {
        return BooleanUtils.isTrue(template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String sql = "INSERT INTO " + company + "." + JIRA_ISSUE_VERSIONS + " AS versions " +
                    " (version_id, project_id, name, description, integration_id, archived, released, overdue, start_date, end_date, fix_version_updated_at) " +
                    " VALUES" +
                    " (?,?,?,?,?,?,?,?,?,?,?) " +
                    " ON CONFLICT (integration_id, version_id) DO UPDATE SET " +
                    " (project_id, name, description, archived, released, overdue, start_date, end_date, fix_version_updated_at) =" +
                    " (EXCLUDED.project_id, EXCLUDED.name, EXCLUDED.description, EXCLUDED.archived, EXCLUDED.released, EXCLUDED.overdue, EXCLUDED.start_date, EXCLUDED.end_date, EXCLUDED.fix_version_updated_at) " +
                    " WHERE " +
                    " (versions.project_id, versions.name, versions.description, versions.archived, versions.released, versions.overdue, versions.start_date, versions.end_date, versions.fix_version_updated_at) " +
                    " IS DISTINCT FROM " +
                    " (EXCLUDED.project_id, EXCLUDED.name, EXCLUDED.description, EXCLUDED.archived, EXCLUDED.released, EXCLUDED.overdue, EXCLUDED.start_date, EXCLUDED.end_date, EXCLUDED.fix_version_updated_at) ";
            try (PreparedStatement insertVersion = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertVersion.setObject(i++, version.getVersionId());
                insertVersion.setObject(i++, version.getProjectId());
                insertVersion.setObject(i++, version.getName());
                insertVersion.setObject(i++, version.getDescription());
                insertVersion.setObject(i++, version.getIntegrationId());
                insertVersion.setObject(i++, version.getArchived());
                insertVersion.setObject(i++, version.getReleased());
                insertVersion.setObject(i++, version.getOverdue());
                insertVersion.setObject(i++, version.getStartDate() != null
                        ? LocalDateTime.ofEpochSecond(version.getStartDate().getEpochSecond(), 0, ZoneOffset.UTC)
                        : null);
                insertVersion.setObject(i++, version.getEndDate() != null
                        ? LocalDateTime.ofEpochSecond(version.getEndDate().getEpochSecond(), 0, ZoneOffset.UTC)
                        : null);
                insertVersion.setObject(i, version.getFixVersionUpdatedAt() != null
                        ? version.getFixVersionUpdatedAt()
                        : 0L);
                return insertVersion.executeUpdate() > 0;
            }
        })));
    }

    public Optional<DbJiraVersion> getJiraVersion(String company, Integer integrationId, Integer versionId) {
        if (Objects.isNull(integrationId) || Objects.isNull(versionId)) {
            log.debug("Invalid integrationId or versionId passed. Exiting getJiraVersion");
            return Optional.empty();
        }
        String sql = "SELECT * FROM " + company + "." + JIRA_ISSUE_VERSIONS
                + " WHERE version_id = :versionId AND integration_id = :integrationId";
        Map<String, Object> params = Map.of("versionId", versionId, "integrationId", integrationId);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbJiraVersion> data = template.query(sql, params, DbJiraIssueConverters.versionRowMapper());
        return data.stream().findFirst();
    }

    /**
     * @param company        tenant id
     * @param versionNames   list of versionNames
     * @param integrationIds list of integrationIds
     * @return list of {@link DbJiraVersion}
     */
    public List<DbJiraVersion> getVersionsForIssues(String company, List<String> versionNames, List<Integer> integrationIds,
                                                    List<String> projectKeys) {
        if (CollectionUtils.isEmpty(versionNames) || CollectionUtils.isEmpty(integrationIds) || CollectionUtils.isEmpty(projectKeys)) {
            return List.of();
        }
        return template.query(
                "SELECT * FROM " + company + "." + JIRA_ISSUE_VERSIONS +
                        " LEFT JOIN ( SELECT key as project_key,integrationid AS project_integration_id, cloudid " +
                        " FROM " + company + "." + JIRA_PROJECTS + ") as projects" +
                        " ON integration_id = projects.project_integration_id" +
                        " AND project_id::text = projects.cloudid" +
                        " WHERE name IN (:versions)" +
                        " AND integration_id IN (:integration_ids)" +
                        " AND project_key IN (:keys)",
                Map.of("versions", versionNames,
                        "integration_ids", integrationIds,
                        "keys", projectKeys),
                DbJiraIssueConverters.versionListRowMapper());
    }

}
