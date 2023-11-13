package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Log4j2
@Service
public class JiraIssueStoryPointsDatabaseService extends FilteredDatabaseService<DbJiraStoryPoints, JiraIssueStoryPointsDatabaseService.JiraStoryPointsFilter> {

    @Autowired
    public JiraIssueStoryPointsDatabaseService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbJiraStoryPoints o) throws SQLException {
        Validate.notBlank(o.getIntegrationId(), "o.getIntegrationId() cannot be null or empty.");
        Validate.notBlank(o.getIssueKey(), "o.getIssueKey() cannot be null or empty.");

        String sql = "INSERT INTO " + company + ".jira_issue_story_points " +
                " (integration_id, issue_key, start_time, end_time, story_points)" +
                " VALUES " +
                " (:integration_id, :issue_key, :start_time, :end_time, :story_points)";

        Map<String, Object> params = Map.of(
                "integration_id", Integer.valueOf(o.getIntegrationId()),
                "issue_key", o.getIssueKey(),
                "start_time", o.getStartTime(),
                "end_time", o.getEndTime(),
                "story_points", o.getStoryPoints()
        );
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    public String upsert(String company, DbJiraStoryPoints o) throws SQLException {
        String sql = "INSERT INTO " + company + ".jira_issue_story_points AS sp " +
                " (integration_id, issue_key, start_time, end_time, story_points)" +
                " VALUES " +
                " (:integration_id, :issue_key, :start_time, :end_time, :story_points)" +
                " ON CONFLICT (integration_id, issue_key, start_time) " +
                " DO UPDATE SET" +
                "   end_time = EXCLUDED.end_time, " +
                "   story_points = EXCLUDED.story_points " +
                " WHERE" +
                " (sp.end_time, sp.story_points) " +
                " IS DISTINCT FROM " +
                " (EXCLUDED.end_time, EXCLUDED.story_points) " +
                " RETURNING id";

        Map<String, Object> params = Map.of(
                "integration_id", Integer.valueOf(o.getIntegrationId()),
                "issue_key", o.getIssueKey(),
                "start_time", o.getStartTime(),
                "end_time", o.getEndTime(),
                "story_points", o.getStoryPoints()
        );
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    @Override
    public Boolean update(String company, DbJiraStoryPoints t) throws SQLException {
       throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbJiraStoryPoints> get(String company, String id) throws SQLException {
        String sql = "SELECT * FROM " + company + ".jira_issue_story_points " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<DbJiraStoryPoints> results = template.query(sql, Map.of("id", id),
                    DbJiraIssueConverters.storyPointsRowMapper());
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get jira story points for id={}", id, e);
            return Optional.empty();
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraStoryPointsFilter.JiraStoryPointsFilterBuilder.class)
    public static class JiraStoryPointsFilter {
        String integrationId;
        String issueKey;
        Long startTime;
        // TODO
//        Long startTimeBefore;
//        Long startTimeAfter;
//        Long endTimeBefore;
//        Long endTimeAfter;
//        Long storyPoints;
    }

    @Override
    public DbListResponse<DbJiraStoryPoints> filter(Integer pageNumber, Integer pageSize, String company, @Nullable JiraStoryPointsFilter filter) {
        int limit = MoreObjects.firstNonNull(pageSize, DEFAULT_PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = filter != null ? filter : JiraStoryPointsFilter.builder().build();

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- integration id
        if (StringUtils.isNotEmpty(filter.getIntegrationId())) {
            conditions.add("integration_id = :integration_id");
            params.put("integration_id", Integer.valueOf(filter.getIntegrationId()));
        }
        // -- issue key
        if (StringUtils.isNotEmpty(filter.getIssueKey())) {
            conditions.add("issue_key = :issue_key");
            params.put("issue_key", StringUtils.upperCase(filter.getIssueKey()));
        }
        // -- start time
        if (filter.getStartTime() != null) {
            conditions.add("start_time = :start_time");
            params.put("start_time", filter.getStartTime());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".jira_issue_story_points " +
                where +
                " ORDER BY start_time ASC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<DbJiraStoryPoints> results = template.query(sql, params, DbJiraIssueConverters.storyPointsRowMapper());
        String countSql = "SELECT count(*) FROM " + company + ".jira_issue_story_points " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE FROM " + company + ".jira_issue_story_points " +
                " WHERE id = :id::uuid";
        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".jira_issue_story_points (" +
                        "    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "    integration_id  INTEGER NOT NULL" +
                        "                       REFERENCES " + company + ".integrations(id)" +
                        "                       ON DELETE CASCADE," +
                        "    issue_key       VARCHAR NOT NULL," +
                        "    story_points    INTEGER NOT NULL," +
                        "    start_time      BIGINT NOT NULL," +
                        "    end_time        BIGINT NOT NULL," +
                        "    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "    UNIQUE (integration_id, issue_key, start_time)" +
                        ")"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

}
