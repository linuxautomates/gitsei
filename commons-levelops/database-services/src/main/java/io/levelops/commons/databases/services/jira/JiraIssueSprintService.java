package io.levelops.commons.databases.services.jira;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.services.jira.conditions.JiraSprintConditionsBuilder;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_SPRINTS;
import static io.levelops.commons.models.DefaultListRequest.DEFAULT_PAGE_SIZE;

@Log4j2
@Service
public class JiraIssueSprintService {

    private final NamedParameterJdbcTemplate template;

    public JiraIssueSprintService(DataSource dataSource) {
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Inserts new sprints into jira_issue_sprint table on conflict doest nothing
     *
     * @param company tenant id
     * @param sprint  {@link DbJiraSprint} object
     * @return return true if insert successfully else false
     */
    public Optional<String> insertJiraSprint(String company, DbJiraSprint sprint) {
        return Optional.ofNullable(template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            //insert jira sprint details
            String sql = "INSERT INTO " + company + "." + JIRA_ISSUE_SPRINTS + " AS sprints " +
                    " (sprint_id, integration_id, name, state, goal, start_date, end_date, completed_at, updated_at) " +
                    " VALUES" +
                    " (?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                    " ON CONFLICT (integration_id, sprint_id) DO UPDATE SET " +
                    " (name, state, goal, start_date, end_date, completed_at, updated_at) = " +
                    " (EXCLUDED.name, EXCLUDED.state, EXCLUDED.goal, EXCLUDED.start_date, EXCLUDED.end_date, EXCLUDED.completed_at, EXCLUDED.updated_at)" +
                    " WHERE " +
                    " (sprints.name, sprints.state, sprints.goal, sprints.start_date, sprints.end_date, sprints.completed_at, sprints.updated_at) " +
                    " IS DISTINCT FROM " +
                    " (EXCLUDED.name, EXCLUDED.state, EXCLUDED.goal, EXCLUDED.start_date, EXCLUDED.end_date, EXCLUDED.completed_at, EXCLUDED.updated_at) " +
                    " RETURNING id";

            try (PreparedStatement insertSprint = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertSprint.setObject(i++, sprint.getSprintId());
                insertSprint.setObject(i++, sprint.getIntegrationId());
                insertSprint.setObject(i++, sprint.getName());
                insertSprint.setObject(i++, StringUtils.upperCase(sprint.getState()));
                insertSprint.setObject(i++, sprint.getGoal());
                insertSprint.setObject(i++, sprint.getStartDate());
                insertSprint.setObject(i++, sprint.getEndDate());
                insertSprint.setObject(i++, sprint.getCompletedDate());
                insertSprint.setObject(i, sprint.getUpdatedAt());

                insertSprint.executeUpdate();
                try (ResultSet rs = insertSprint.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }
                return null;
            }
        })));
    }

    public Stream<DbJiraSprint> streamSprints(String company, JiraSprintFilter filter) {
        return PaginationUtils.stream(0, 1, page -> filterSprints(company, page, null, filter).getRecords());
    }

    public DbListResponse<DbJiraSprint> filterSprints(String company, Integer pageNumber, Integer pageSize, @Nullable JiraSprintFilter filter) {
        int limit = MoreObjects.firstNonNull(pageSize, DEFAULT_PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = filter != null ? filter : JiraSprintFilter.builder().build();

        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        JiraSprintConditionsBuilder.generateSprintsConditions(conditions, null, params, filter);

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + "." + JIRA_ISSUE_SPRINTS +
                where +
                " ORDER BY sprint_id ASC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        String countSql = "SELECT count(*) FROM " + company + "." + JIRA_ISSUE_SPRINTS + where;
        List<DbJiraSprint> results = template.query(sql, params, DbJiraIssueConverters.listSprintsMapper());
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public Optional<DbJiraSprint> getSprint(String company, int integrationId, int sprintId) {
        return IterableUtils.getFirst(filterSprints(company, 0, 1, JiraSprintFilter.builder()
                .integrationIds(List.of(String.valueOf(integrationId)))
                .sprintIds(List.of(String.valueOf(sprintId)))
                .build()).getRecords());
    }


}
