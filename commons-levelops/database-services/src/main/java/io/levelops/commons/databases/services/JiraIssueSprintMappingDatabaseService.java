package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.stream.Collectors;

import static java.util.Map.entry;

@Log4j2
@Service
public class JiraIssueSprintMappingDatabaseService extends FilteredDatabaseService<DbJiraIssueSprintMapping, JiraIssueSprintMappingDatabaseService.JiraIssueSprintMappingFilter> {
    private static final Integer OVERRIDE_DEFAULT_PAGE_SIZE = 500;

    @Autowired
    public JiraIssueSprintMappingDatabaseService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbJiraIssueSprintMapping o) throws SQLException {
        Validate.notBlank(o.getIntegrationId(), "integrationId cannot be null or empty.");
        Validate.notBlank(o.getIssueKey(), "issueKey cannot be null or empty.");
        Validate.notBlank(o.getSprintId(), "sprintId cannot be null or empty.");
        Validate.notNull(o.getAddedAt(), "addedAt cannot be null.");

        String sql = "INSERT INTO " + company + ".jira_issue_sprint_mappings " +
                "(integration_id, issue_key, sprint_id, added_at, planned, delivered, outside_of_sprint, ignorable_issue_type, story_points_planned, story_points_delivered, removed_mid_sprint)" +
                " VALUES " +
                "(:integration_id, :issue_key, :sprint_id, :added_at, :planned, :delivered, :outside_of_sprint, :ignorable_issue_type, :story_points_planned, :story_points_delivered, :removed_mid_sprint)";

        Map<String, Object> params = Map.ofEntries(
                entry("integration_id", Integer.valueOf(o.getIntegrationId())),
                entry("issue_key", o.getIssueKey()),
                entry("sprint_id", Integer.valueOf(o.getSprintId())),
                entry("added_at", o.getAddedAt()),
                entry("planned", MoreObjects.firstNonNull(o.getPlanned(), false)),
                entry("delivered", MoreObjects.firstNonNull(o.getDelivered(), false)),
                entry("outside_of_sprint", MoreObjects.firstNonNull(o.getOutsideOfSprint(), false)),
                entry("ignorable_issue_type", MoreObjects.firstNonNull(o.getIgnorableIssueType(), false)),
                entry("story_points_planned", MoreObjects.firstNonNull(o.getStoryPointsPlanned(), 0)),
                entry("story_points_delivered", MoreObjects.firstNonNull(o.getStoryPointsDelivered(), 0)),
                entry("removed_mid_sprint", MoreObjects.firstNonNull(o.getRemovedMidSprint(), false))
        );
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    public String upsert(String company, DbJiraIssueSprintMapping o) throws SQLException {
        Validate.notBlank(o.getIntegrationId(), "integrationId cannot be null or empty.");
        Validate.notBlank(o.getIssueKey(), "issueKey cannot be null or empty.");
        Validate.notBlank(o.getSprintId(), "sprintId cannot be null or empty.");
        Validate.notNull(o.getAddedAt(), "addedAt cannot be null.");

        String sql = "INSERT INTO " + company + ".jira_issue_sprint_mappings AS mappings " +
                " (integration_id, issue_key, sprint_id, added_at, planned, delivered, outside_of_sprint, ignorable_issue_type, story_points_planned, story_points_delivered, removed_mid_sprint)" +
                " VALUES " +
                " (:integration_id, :issue_key, :sprint_id, :added_at, :planned, :delivered, :outside_of_sprint, :ignorable_issue_type, :story_points_planned, :story_points_delivered, :removed_mid_sprint)" +
                " ON CONFLICT (integration_id, issue_key, sprint_id) " +
                " DO UPDATE SET" +
                "   added_at = GREATEST(mappings.added_at, EXCLUDED.added_at), " +
                "   planned = EXCLUDED.planned, " +
                "   delivered = EXCLUDED.delivered, " +
                "   outside_of_sprint = EXCLUDED.outside_of_sprint, " +
                "   ignorable_issue_type = EXCLUDED.ignorable_issue_type, " +
                "   story_points_planned = EXCLUDED.story_points_planned, " +
                "   story_points_delivered = EXCLUDED.story_points_delivered, " +
                "   removed_mid_sprint = mappings.removed_mid_sprint" +
                " WHERE " +
                " (mappings.added_at, mappings.planned, mappings.delivered, mappings.outside_of_sprint, mappings.ignorable_issue_type, mappings.story_points_planned, mappings.story_points_delivered, mappings.removed_mid_sprint) " +
                " IS DISTINCT FROM " +
                " (EXCLUDED.added_at, EXCLUDED.planned, EXCLUDED.delivered, EXCLUDED.outside_of_sprint, EXCLUDED.ignorable_issue_type, EXCLUDED.story_points_planned, EXCLUDED.story_points_delivered, EXCLUDED.removed_mid_sprint) " +
                " RETURNING id";

        Map<String, Object> params = Map.ofEntries(
                entry("integration_id", Integer.valueOf(o.getIntegrationId())),
                entry("issue_key", o.getIssueKey()),
                entry("sprint_id", Integer.valueOf(o.getSprintId())),
                entry("added_at", o.getAddedAt()),
                entry("planned", MoreObjects.firstNonNull(o.getPlanned(), false)),
                entry("delivered", MoreObjects.firstNonNull(o.getDelivered(), false)),
                entry("outside_of_sprint", MoreObjects.firstNonNull(o.getOutsideOfSprint(), false)),
                entry("ignorable_issue_type", MoreObjects.firstNonNull(o.getIgnorableIssueType(), false)),
                entry("story_points_planned", MoreObjects.firstNonNull(o.getStoryPointsPlanned(), 0)),
                entry("story_points_delivered", MoreObjects.firstNonNull(o.getStoryPointsDelivered(), 0)),
                entry("removed_mid_sprint", MoreObjects.firstNonNull(o.getRemovedMidSprint(), false))
        );
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    @Override
    public Boolean update(String company, DbJiraIssueSprintMapping t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbJiraIssueSprintMapping> get(String company, String id) throws SQLException {
        String sql = "SELECT * FROM " + company + ".jira_issue_sprint_mappings " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<DbJiraIssueSprintMapping> results = template.query(sql, Map.of("id", id),
                    DbJiraIssueConverters.sprintMappingRowMapper());
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get jira status metadata for id={}", id, e);
            return Optional.empty();
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = DbJiraIssueSprintMapping.DbJiraIssueSprintMappingBuilder.class)
    public static class JiraIssueSprintMappingFilter {
        List<String> integrationIds;
        String issueKey;
        List<String> sprintIds;
        Boolean planned;
        Boolean delivered;
        Boolean outsideOfSprint;
        Boolean ignorableIssueType;
        Boolean removedMidSprint;
    }

    @Override
    public DbListResponse<DbJiraIssueSprintMapping> filter(Integer pageNumber, Integer pageSize, String company, @Nullable JiraIssueSprintMappingFilter filter) {
        int limit = MoreObjects.firstNonNull(pageSize, OVERRIDE_DEFAULT_PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = filter != null ? filter : JiraIssueSprintMappingFilter.builder().build();

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- integration ids
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            conditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids", filter.getIntegrationIds().stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        }
        // -- issue key
        if (StringUtils.isNotEmpty(filter.getIssueKey())) {
            conditions.add("issue_key = :issue_key");
            params.put("issue_key", filter.getIssueKey());
        }
        // -- sprint ids
        if (CollectionUtils.isNotEmpty(filter.getSprintIds())) {
            conditions.add("sprint_id IN (:sprint_ids)");
            params.put("sprint_ids", filter.getSprintIds().stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        }
        // -- planned
        if (filter.getPlanned() != null) {
            conditions.add("planned = :planned");
            params.put("planned", filter.getPlanned());
        }
        // -- delivered
        if (filter.getDelivered() != null) {
            conditions.add("delivered = :delivered");
            params.put("delivered", filter.getDelivered());
        }
        // -- outside_of_sprint
        if (filter.getOutsideOfSprint() != null) {
            conditions.add("outside_of_sprint = :outside_of_sprint");
            params.put("outside_of_sprint", filter.getOutsideOfSprint());
        }
        // -- ignorable_issue_type
        if (filter.getIgnorableIssueType() != null) {
            conditions.add("ignorable_issue_type = :ignorable_issue_type");
            params.put("ignorable_issue_type", filter.getIgnorableIssueType());
        }
        // -- removed from the mid sprint
        if (filter.getRemovedMidSprint() != null) {
            conditions.add("removed_mid_sprint = :removed_mid_sprint");
            params.put("removed_mid_sprint", filter.getRemovedMidSprint());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".jira_issue_sprint_mappings " +
                where +
                " ORDER BY added_at ASC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<DbJiraIssueSprintMapping> results = template.query(sql, params, DbJiraIssueConverters.sprintMappingRowMapper());
        String countSql = "SELECT count(*) FROM " + company + ".jira_issue_sprint_mappings " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE FROM " + company + ".jira_issue_sprint_mappings " +
                " WHERE id = :id::uuid";
        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".jira_issue_sprint_mappings (" +
                        "    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "    integration_id         INTEGER NOT NULL" +
                        "                              REFERENCES " + company + ".integrations(id)" +
                        "                              ON DELETE CASCADE," +
                        "    issue_key              VARCHAR NOT NULL," +
                        "    sprint_id              INTEGER NOT NULL," +
                        "    added_at               BIGINT NOT NULL," +
                        "    planned                BOOLEAN NOT NULL," +
                        "    delivered              BOOLEAN NOT NULL," +
                        "    outside_of_sprint      BOOLEAN NOT NULL," +
                        "    ignorable_issue_type   BOOLEAN NOT NULL," +
                        "    story_points_planned   INTEGER NOT NULL," +
                        "    story_points_delivered INTEGER NOT NULL," +
                        "    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "    removed_mid_sprint     BOOLEAN NOT NULL DEFAULT FALSE," +
                        "    UNIQUE (integration_id, issue_key, sprint_id)" +
                        ")"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

}
