package io.levelops.commons.databases.services.jira;

import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.jira.DbJiraPrioritySla;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.JiraIssueService.PRIORITIES_SLA_TABLE;

@Log4j2
@Service
public class JiraIssuePrioritySlaService {

    private final NamedParameterJdbcTemplate template;

    public JiraIssuePrioritySlaService(DataSource dataSource) {
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    public boolean updatePrioritySla(String company, DbJiraPrioritySla prioritySla) {
        return bulkUpdatePrioritySla(company,
                List.of(prioritySla.getId()),
                null,
                null,
                null,
                null,
                prioritySla.getRespSla(),
                prioritySla.getSolveSla()) == 1;
    }

    public int bulkUpdatePrioritySla(String company,
                                         List<String> ids,
                                         List<String> integrationIds,
                                         List<String> projects,
                                         List<String> issueTypes,
                                         List<String> priorities,
                                         Long respSla,
                                         Long solveSla) {
        List<String> updates = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (respSla != null) {
            updates.add("resp_sla = :resp_sla");
            params.put("resp_sla", respSla);
        }
        if (solveSla != null) {
            updates.add("solve_sla = :solve_sla");
            params.put("solve_sla", solveSla);
        }
        if (updates.isEmpty()) {
            return 0;
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            conditions.add("id IN (:ids)");
            params.put("ids",
                    ids.stream()
                            .map(UUID::fromString)
                            .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            conditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids",
                    integrationIds.stream()
                            .map(NumberUtils::toInt)
                            .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            conditions.add("project IN (:projects)");
            params.put("projects", projects);
        }
        if (CollectionUtils.isNotEmpty(issueTypes)) {
            conditions.add("task_type IN (:task_types)");
            params.put("task_types", issueTypes);
        }
        if (CollectionUtils.isNotEmpty(priorities)) {
            conditions.add("priority IN (:priorities)");
            params.put("priorities", priorities);
        }
        String where = "";
        if (!conditions.isEmpty()) {
            where = " WHERE " + String.join(" AND ", conditions);
        }
        String sql = "UPDATE " + company + "." + PRIORITIES_SLA_TABLE + " SET "
                + String.join(", ", updates) + where;
        return template.update(sql, params);
    }

    public DbListResponse<DbJiraPrioritySla> listPrioritiesSla(String company,
                                                               List<String> integrationIds,
                                                               List<String> projects,
                                                               List<String> issueTypes,
                                                               List<String> priorities,
                                                               Integer pageNumber,
                                                               Integer pageSize) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            conditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids",
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            conditions.add("project IN (:projects)");
            params.put("projects", projects);
        }
        if (CollectionUtils.isNotEmpty(issueTypes)) {
            conditions.add("task_type IN (:task_types)");
            params.put("task_types", issueTypes);
        }
        if (CollectionUtils.isNotEmpty(priorities)) {
            conditions.add("priority IN (:priorities)");
            params.put("priorities", priorities);
        }
        String where = "";
        if (!conditions.isEmpty()) {
            where = " WHERE " + String.join(" AND ", conditions);
        }

        String sortLimit = " ORDER BY resp_sla DESC, solve_sla DESC OFFSET :skip LIMIT :limit";
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);

        String sql = "SELECT * FROM " + company + "." + PRIORITIES_SLA_TABLE + where;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbJiraPrioritySla> results = template.query(
                sql + sortLimit, params, DbJiraIssueConverters.priorityRowMapper());
        String countSql = "SELECT COUNT(*) FROM ( " + sql + " ) as i";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }
}
