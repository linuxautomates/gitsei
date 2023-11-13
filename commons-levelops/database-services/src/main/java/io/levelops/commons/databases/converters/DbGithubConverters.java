package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.models.database.github.DbGithubCardTransition;
import io.levelops.commons.databases.models.database.github.DbGithubProject;
import io.levelops.commons.databases.models.database.github.DbGithubProjectCard;
import io.levelops.commons.databases.models.database.github.DbGithubProjectCardWithIssue;
import io.levelops.commons.databases.models.database.github.DbGithubProjectColumn;
import io.levelops.commons.databases.models.filters.GithubCardFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.sql.SQLException;

@Log4j2
public class DbGithubConverters {

    public static RowMapper<DbGithubProject> projectRowMapper() {
        return (rs, rowNumber) -> DbGithubProject.builder()
                .id(rs.getString("id"))
                .projectId(rs.getString("project_id"))
                .project(rs.getString("project"))
                .integrationId(rs.getString("integration_id"))
                .organization(rs.getString("organization"))
                .description(rs.getString("description"))
                .state(rs.getString("state"))
                .creator(rs.getString("creator"))
                .isPrivate(rs.getBoolean("private"))
                .projectCreatedAt(rs.getLong("project_created_at"))
                .projectUpdatedAt(rs.getLong("project_updated_at"))
                .createdAt(rs.getLong("created_at"))
                .updatedAt(rs.getLong("updated_at"))
                .build();
    }

    public static RowMapper<DbGithubProjectColumn> columnRowMapper() {
        return (rs, rowNumber) -> DbGithubProjectColumn.builder()
                .id(rs.getString("id"))
                .projectId(rs.getString("project_id"))
                .columnId(rs.getString("column_id"))
                .name(rs.getString("name"))
                .columnCreatedAt(rs.getLong("column_created_at"))
                .columnUpdatedAt(rs.getLong("column_updated_at"))
                .createdAt(rs.getLong("created_at"))
                .updatedAt(rs.getLong("updated_at"))
                .build();
    }

    public static RowMapper<DbGithubProjectCard> cardRowMapper() {
        return (rs, rowNumber) -> DbGithubProjectCard.builder()
                .id(rs.getString("id"))
                .currentColumnId(rs.getString("current_column_id"))
                .cardId(rs.getString("card_id"))
                .archived(rs.getBoolean("archived"))
                .creator(rs.getString("creator"))
                .contentUrl(rs.getString("content_url"))
                .issueId(rs.getString("issue_id"))
                .cardCreatedAt(rs.getLong("card_created_at"))
                .cardUpdatedAt(rs.getLong("card_updated_at"))
                .createdAt(rs.getLong("created_at"))
                .updatedAt(rs.getLong("updated_at"))
                .build();
    }

    public static RowMapper<DbGithubProjectCard> issueCardRowMapper() {
        return (rs, rowNumber) -> DbGithubProjectCard.builder()
                .id(rs.getString("id"))
                .currentColumnId(rs.getString("current_column_id"))
                .cardId(rs.getString("card_id"))
                .archived(rs.getBoolean("archived"))
                .creator(rs.getString("creator"))
                .cardCreatedAt(rs.getLong("card_created_at"))
                .cardUpdatedAt(rs.getLong("card_updated_at"))
                .createdAt(rs.getLong("created_at"))
                .updatedAt(rs.getLong("updated_at"))
                .issueId(rs.getString("issues_issue_id"))
                .repoId(rs.getString("issues_repo_id"))
                .number(rs.getString("issues_number"))
                .build();
    }

    public static RowMapper<DbGithubProjectCardWithIssue> issueCardWIthIssueRowMapper() {
        return (rs, rowNumber) -> DbGithubProjectCardWithIssue.builder()
                .currentColumnId(rs.getString("current_column_id"))
                .cardId(rs.getString("card_id"))
                .archived(rs.getBoolean("archived"))
                .creator(rs.getString("creator"))
                .contentUrl(rs.getString("content_url"))
                .cardCreatedAt(rs.getLong("card_created_at"))
                .cardUpdatedAt(rs.getLong("card_updated_at"))
                .createdAt(rs.getLong("created_at"))
                .updatedAt(rs.getLong("updated_at"))
                .issueId(rs.getString("issues_issue_id"))
                .repoId(rs.getString("issues_repo_id"))
                .number(rs.getString("issues_number"))
                .title(rs.getString("issue_title"))
                .state(rs.getString("issue_state"))
                .assignees((rs.getArray("issues_assignees") != null &&
                        rs.getArray("issues_assignees").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("issues_assignees").getArray()) : List.of())
                .labels((rs.getArray("issues_labels") != null &&
                        rs.getArray("issues_labels").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("issues_labels").getArray()) : List.of())
                .issueCreatedAt(rs.getTimestamp("issue_created_at").toInstant().getEpochSecond())
                .issueClosedAt(rs.getTimestamp("issue_closed_at") != null ?
                        rs.getTimestamp("issue_closed_at").toInstant().getEpochSecond() : null)
                .build();
    }

    public static RowMapper<DbGithubCardTransition> cardTransitionRowMapper() {
        return (rs, rowNumber) -> DbGithubCardTransition.builder()
                .id(rs.getString("id"))
                .projectId(rs.getString("project_id"))
                .cardId(rs.getString("card_id"))
                .columnId(rs.getString("column_id"))
                .integrationId(rs.getString("integration_id"))
                .updater(rs.getString("updater"))
                .endTime(rs.getLong("end_time"))
                .startTime(rs.getLong("start_time"))
                .createdAt(rs.getLong("created_at"))
                .build();
    }

    public static RowMapper<DbAggregationResult> distinctCardRowMapper(String key,
                                                                       GithubCardFilter.CALCULATION calc,
                                                                       Optional<String> additionalKey) {
        return (rs, rowNumber) -> {
            if (calc == GithubCardFilter.CALCULATION.count) {
                return DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .count(rs.getLong("ct"))
                        .build();
            } else if (calc == GithubCardFilter.CALCULATION.resolution_time) {
                return DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .count(rs.getLong("ct"))
                        .median(rs.getLong("md"))
                        .build();
            } else if (calc == GithubCardFilter.CALCULATION.stage_times_report) {
                return DbAggregationResult.builder()
                        .key(rs.getString(key))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .count(rs.getLong("ct"))
                        .mean(rs.getDouble("mean_time"))
                        .median(rs.getLong("md"))
                        .build();
            }
            throw new SQLException("Unsupported query.");
        };
    }
}