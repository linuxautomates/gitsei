package io.levelops.commons.databases.services;


import io.levelops.commons.databases.converters.SonarQubeIssueConverters;
import io.levelops.commons.databases.converters.SonarQubeProjectConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeIssue;
import io.levelops.commons.databases.models.filters.SonarQubeIssueFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.helper.organization.OrgUnitHelper.newOUConfigForStacks;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * class to perform Database operations like insert, group by, select queries etc on sonarqube_issues table.
 */


@Log4j2
@Service
public class SonarQubeIssueService extends DatabaseService<DbSonarQubeIssue> {

    private static final String SONARQUBE_PROJECT_ISSUES = "sonarqube_project_issues";
    private static final String SONARQUBE_PROJECTS = "sonarqube_projects";
    private static final Set<String> SORTABLE_COLUMNS = Set.of("effort", "debt",
            "issue_creation_date", "issue_updation_date");
    private final NamedParameterJdbcTemplate template;


    public SonarQubeProjectService projectService;
    private static final Set<String> PARTIAL_FILTER_COLUMN = Set.of("project", "type", "severity", "organization", "status", "author");
    @Autowired
    protected SonarQubeIssueService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        projectService = new SonarQubeProjectService(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(SonarQubeProjectService.class);
    }

    @Override
    public String insert(String company, DbSonarQubeIssue issue) throws SQLException {

        String sql = "INSERT INTO " + company + "." + SONARQUBE_PROJECT_ISSUES + " AS t " +
                " (project, integration_id, pull_request, type, organization, key, rule," +
                "  severity, component, status, message, effort, debt, author, tags, ingested_at," +
                "  issue_creation_date, issue_updation_date, creation_date, updation_date) " +
                " VALUES (:project, :integration_id, :pull_request, :type, :organization, :key, :rule, " +
                "  :severity, :component, :status, :message, :effort, :debt, :author, :tags::varchar[], :ingested_at, " +
                "  :issue_creation_date, :issue_updation_date, 'now', 'now') " +
                " ON CONFLICT (key, project, ingested_at, integration_id) " +
                " DO UPDATE SET " +
                "   integration_id = EXCLUDED.integration_id," +
                "   type = EXCLUDED.type," +
                "   organization = EXCLUDED.organization," +
                "   rule = EXCLUDED.rule," +
                "   severity = EXCLUDED.severity," +
                "   component = EXCLUDED.component," +
                "   status = EXCLUDED.status," +
                "   message = EXCLUDED.message, " +
                "   effort = EXCLUDED.debt, " +
                "   author = EXCLUDED.author," +
                "   tags = EXCLUDED.tags," +
                "   issue_creation_date = EXCLUDED.issue_creation_date," +
                "   issue_updation_date = EXCLUDED.issue_updation_date," +
                "   updation_date = EXCLUDED.updation_date" +
                " WHERE (t.pull_request, t.type, t.organization, t.rule," +
                "  t.severity, t.component, t.status, t.message, t.effort, t.debt, t.author, t.tags," +
                "  t.issue_creation_date, t.issue_updation_date)" +
                " IS DISTINCT FROM (EXCLUDED.pull_request, EXCLUDED.type, EXCLUDED.organization, EXCLUDED.rule," +
                "  EXCLUDED.severity, EXCLUDED.component, EXCLUDED.status, EXCLUDED.message, EXCLUDED.effort, EXCLUDED.debt, EXCLUDED.author, EXCLUDED.tags," +
                "  EXCLUDED.issue_creation_date, EXCLUDED.issue_updation_date);";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("project", issue.getProject());
        params.addValue("integration_id", NumberUtils.toInt(issue.getIntegrationId()));
        params.addValue("pull_request", issue.getPullRequest());
        params.addValue("type", issue.getType());
        params.addValue("organization", issue.getOrganization());
        params.addValue("key", issue.getKey());
        params.addValue("rule", issue.getRule());
        params.addValue("severity", issue.getSeverity());
        params.addValue("component", issue.getComponent());
        params.addValue("status", issue.getStatus());
        params.addValue("message", issue.getMessage());
        params.addValue("effort", convertStringTimeToMinutes(issue.getEffort()));
        params.addValue("debt", convertStringTimeToMinutes(issue.getDebt()));
        params.addValue("author", issue.getAuthor());
        params.addValue("tags", DatabaseUtils.toSqlArray(issue.getTags()));
        params.addValue("ingested_at", DateUtils.toTimestamp(issue.getIngestedAt()));
        params.addValue("issue_creation_date", DateUtils.toTimestamp(issue.getCreationDate()));
        params.addValue("issue_updation_date", DateUtils.toTimestamp(issue.getUpdationDate()));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return this.getId(company, issue.getKey(), issue.getProject(), issue.getIntegrationId())
                    .orElseThrow(() -> new SQLException("Failed to get issue id"));
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    @Nullable
    private Integer convertStringTimeToMinutes(@Nullable String effort) {
        if (effort == null) {
            return null;
        }
        String eff = effort.toLowerCase();
        int val = Integer.parseInt(eff.replaceAll("[^0-9]", ""));
        if (eff.contains("min") || eff.contains("m")) {
            return val;
        } else if (eff.contains("sec") || eff.contains("s")) {
            return val / 60;
        } else {
            return val * 60;
        }
    }


    public Optional<String> getId(String company, String key, String project, String integrationId) {
        String query = "SELECT ID FROM " + company + "." + SONARQUBE_PROJECT_ISSUES +
                " where key=:key AND project=:project AND integration_id=:integration_id;";
        Map<String, Object> params = Map.of(
                "key", key,
                "project", project,
                "integration_id", NumberUtils.toInt(integrationId)
        );
        return Optional.ofNullable(this.template.query(query, params, SonarQubeProjectConverters.idMapper()));
    }

    @Override
    public Boolean update(String company, DbSonarQubeIssue t) throws SQLException {
        // Update is taken care of in the insert method itself.
        return null;
    }

    @Override
    public Optional<DbSonarQubeIssue> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException("Not supported");
    }

    public Optional<DbSonarQubeIssue> get(String company, String key, String project, Date ingestedAt,
                                          String integrationId) {
        String query = "SELECT * from " + company + "." + SONARQUBE_PROJECT_ISSUES + " " +
                "where key=:key AND project=:project AND ingested_at=:ingested_at AND integration_id=:integration_id;";
        Map<String, Object> params = Map.of(
                "key", key,
                "project", project,
                "ingested_at", DateUtils.toTimestamp(ingestedAt),
                "integration_id", NumberUtils.toInt(integrationId)
        );
        return Optional.ofNullable(template.query(query, params, SonarQubeIssueConverters.rowMapper()));
    }

    @Override
    public DbListResponse<DbSonarQubeIssue> list(String company,
                                                 Integer pageNumber,
                                                 Integer pageSize) throws SQLException {
        return list(company, SonarQubeIssueFilter.builder().build(), Collections.emptyMap(), pageNumber, pageSize, null);
    }


    public DbListResponse<DbSonarQubeIssue> list(String company, SonarQubeIssueFilter filter,
                                                 Map<String, SortingOrder> sortBy,
                                                 Integer pageNumber,
                                                 Integer pageSize,
                                                 OUConfiguration ouConfig) {
        Map<String, Object> params = new HashMap<>();
        final List<String> conditions = createWhereClauseAndUpdateParams(company, params, filter.getIngestedAt(), filter.getIntegrationIds(),
                filter.getSeverities(), filter.getStatuses(), filter.getProjects(), filter.getOrganizations(),
                filter.getComponents(), filter.getAuthors(), filter.getTypes(), filter.getTags(), filter.getPartialMatch(), ouConfig);
        setPagingParams(pageNumber, pageSize, params);
        String sortByKey = sortBy.entrySet().stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    } else {
                        return "issue_creation_date";
                    }
                })
                .orElse("issue_creation_date");
        SortingOrder sortingOrder = sortBy.getOrDefault("order", SortingOrder.DESC);
        String whereClause = getWhereClause(conditions);
        String limitClause = " OFFSET :offset LIMIT :limit";
        String orderByClause = " ORDER BY " + sortByKey + " " + sortingOrder.name();
        String selectStatement = "SELECT * FROM " + company + "." + SONARQUBE_PROJECT_ISSUES;
        String query = selectStatement + " " + whereClause + " " + orderByClause + " " + limitClause;
        String countQuery = "SELECT COUNT(*) FROM " + company + "." + SONARQUBE_PROJECT_ISSUES + " " + whereClause;
        List<DbSonarQubeIssue> issues = template.query(query, params, SonarQubeIssueConverters.listRowMapper());
        final Integer count = template.queryForObject(countQuery, params, Integer.class);
        return DbListResponse.of(issues, count);
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(String company,
                                                              SonarQubeIssueFilter filter,
                                                              List<SonarQubeIssueFilter.DISTINCT> stacks,
                                                              String configTableKey,
                                                              OUConfiguration ouConfig)
            throws SQLException {
        final DbListResponse<DbAggregationResult> result = groupByAndCalculate(company, filter, configTableKey, ouConfig);
        if (CollectionUtils.isEmpty(stacks)
                || SonarQubeIssueFilter.DISTINCT.trend.equals(stacks.get(0))
                || SonarQubeIssueFilter.DISTINCT.trend.equals(filter.getDistinct())) {
            return result;
        }
        final SonarQubeIssueFilter.DISTINCT stack = stacks.get(0);
        OUConfiguration ouConfigForStacks = ouConfig;
        List<DbAggregationResult> finalList = new ArrayList<>();
        for (DbAggregationResult record : result.getRecords()) {
            log.debug("stack record = {}", record);
            SonarQubeIssueFilter stackFilter = null;
            if (StringUtils.isNotEmpty(configTableKey)) {
                stackFilter = filter.toBuilder().distinct(stack).build();
            } else {
                if (record.getKey() == null) {
                    log.warn("stack record key is null! {}", record);
                    continue;
                }
                switch (filter.getDistinct()) {
                    case project:
                        stackFilter = filter.toBuilder().projects(List.of(record.getKey())).distinct(stack).build();
                        break;
                    case type:
                        stackFilter = filter.toBuilder().types(List.of(record.getKey())).distinct(stack).build();
                        break;
                    case severity:
                        stackFilter = filter.toBuilder().severities(List.of(record.getKey())).distinct(stack).build();
                        break;
                    case status:
                        stackFilter = filter.toBuilder().statuses(List.of(record.getKey())).distinct(stack).build();
                        break;
                    case organization:
                        stackFilter = filter.toBuilder().organizations(List.of(record.getKey())).distinct(stack).build();
                        break;
                    case author:
                        stackFilter = filter.toBuilder().authors(List.of(record.getKey())).distinct(stack).build();
                        ouConfigForStacks = newOUConfigForStacks(ouConfig, "authors");
                        break;
                    case tag:
                        stackFilter = filter.toBuilder().tags(List.of(record.getKey())).distinct(stack).build();
                        break;
                    case component:
                        stackFilter = filter.toBuilder().components(List.of(record.getKey())).distinct(stack).build();
                        break;
                    default:
                        throw new SQLException("This stack is not available for sonarqube issues." + stack);
                }
            }
            finalList.add(record.toBuilder().stacks(groupByAndCalculate(company, stackFilter, null, ouConfigForStacks)
                    .getRecords()).build());
        }
        return DbListResponse.of(finalList, finalList.size());
    }


    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   SonarQubeIssueFilter filter,
                                                                   String configTableKey,
                                                                   OUConfiguration ouConfig) throws SQLException {
        String groupByClause;
        String orderByClause = " ORDER by ct DESC ";
        String key;
        SonarQubeIssueFilter.DISTINCT distinct = filter.getDistinct();
        if (StringUtils.isNotEmpty(configTableKey)) {
            distinct = SonarQubeIssueFilter.DISTINCT.none;
        }
        switch (distinct) {
            case project:
                key = "project";
                groupByClause = " GROUP BY " + key;
                break;
            case type:
                key = "type";
                groupByClause = " GROUP BY " + key;
                break;
            case tag:
                key = "UNNEST(tags) as tag";
                groupByClause = " GROUP BY tag";
                break;
            case severity:
                key = "severity";
                groupByClause = " GROUP BY " + key;
                break;
            case organization:
                key = "organization";
                groupByClause = " GROUP BY " + key;
                break;
            case status:
                key = "status";
                groupByClause = " GROUP BY " + key;
                break;
            case author:
                key = "author";
                groupByClause = " GROUP BY " + key;
                break;
            case component:
                key = "component";
                groupByClause = " GROUP BY " + key;
                break;
            case trend:
                groupByClause = " GROUP BY trend ";
                orderByClause = " ORDER BY trend ASC ";
                key = "EXTRACT(EPOCH FROM ingested_at)::text AS trend";
                break;
            case none:
                groupByClause = "";
                key = "";
                break;
            default:
                throw new SQLException("Unsupported across: " + distinct.toString());
        }

        SonarQubeIssueFilter.CALCULATION calculation = filter.getCalculation();
        String aggSql;
        if (calculation == SonarQubeIssueFilter.CALCULATION.effort) {
            aggSql = String.format("MIN(%s) as mini ,MAX(%s) as maxi,SUM(%s) " +
                    "as sum, COUNT(*) as ct", calculation.toString(), calculation.toString(), calculation.toString());
        } else {
            aggSql = " COUNT(*) as ct ";
        }

        Map<String, Object> params = new HashMap<>();
        Long ingestedDate = distinct != SonarQubeIssueFilter.DISTINCT.trend ? filter.getIngestedAt() : null;
        final List<String> conditions = createWhereClauseAndUpdateParams(company, params, ingestedDate, filter.getIntegrationIds(),
                filter.getSeverities(),
                filter.getStatuses(), filter.getProjects(), filter.getOrganizations(), filter.getComponents(),
                filter.getAuthors(), filter.getTypes(), filter.getTags(), filter.getPartialMatch(), ouConfig);

        String whereClause = getWhereClause(conditions);

        String query;
        List<DbAggregationResult> aggResults;
        if (StringUtils.isNotEmpty(configTableKey)) {
            query = "SELECT '" + configTableKey + "' AS config_key" + "," + aggSql + " FROM " + company + "." +
                    SONARQUBE_PROJECT_ISSUES + " " + whereClause + " " + orderByClause;
            log.info("sql = " + query); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            aggResults = template.query(query, params, SonarQubeIssueConverters.aggRowMapper("config_key", calculation));
        } else {
            query = "SELECT " + key + (StringUtils.isNotEmpty(key) ? "," : "") + aggSql + " from " + company + "." + SONARQUBE_PROJECT_ISSUES + " " +
                    whereClause + " " + (StringUtils.isNotEmpty(groupByClause) ? groupByClause : "") + " " + orderByClause;
            log.info("sql = " + query); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            aggResults = template.query(query, params,
                    SonarQubeIssueConverters.aggRowMapper(distinct.toString(), calculation));
        }
        return DbListResponse.of(aggResults, aggResults.size());
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateForValues(String company,
                                                                   SonarQubeIssueFilter filter,
                                                                   String configTableKey,
                                                                   OUConfiguration ouConfig) throws SQLException {
        String groupByClause;
        String orderByClause = " ORDER by ct DESC ";
        String key;
        SonarQubeIssueFilter.DISTINCT distinct = filter.getDistinct();
        if (StringUtils.isNotEmpty(configTableKey)) {
            distinct = SonarQubeIssueFilter.DISTINCT.none;
        }
        String name = distinct.name();
        switch (distinct) {
            case project:
                key = "key as project";
                groupByClause = " GROUP BY key";
                break;
            case tag:
                key = "UNNEST(tags) as tag";
                groupByClause = " GROUP BY tag";
                break;
            case type:
            case severity:
            case organization:
            case status:
            case author:
            case component:
                key = name;
                groupByClause = " GROUP BY " + key;
                break;
            case none:
                groupByClause = "";
                key = "";
                break;
            default:
                throw new SQLException("Unsupported across: " + distinct.toString());
        }

        SonarQubeIssueFilter.CALCULATION calculation = filter.getCalculation();
        Map<String, Object> params = new HashMap<>();
        Long ingestedDate = !SonarQubeIssueFilter.DISTINCT.project.equals(distinct) ? filter.getIngestedAt() : null;
        final List<String> conditions = createWhereClauseAndUpdateParams(company, params, ingestedDate, filter.getIntegrationIds(),
                filter.getSeverities(),
                filter.getStatuses(), filter.getProjects(), filter.getOrganizations(), filter.getComponents(),
                filter.getAuthors(), filter.getTypes(), filter.getTags(), filter.getPartialMatch(), ouConfig);

        String whereClause = getWhereClause(conditions);
        String table = SonarQubeIssueFilter.DISTINCT.project.equals(distinct) ? SONARQUBE_PROJECTS :  SONARQUBE_PROJECT_ISSUES;
        String query;
        List<DbAggregationResult> aggResults;
        if (StringUtils.isNotEmpty(configTableKey)) {
            query = "SELECT '" + configTableKey + "' AS config_key" + ", COUNT(*) as ct FROM " + company + "." +
                    table + " " + whereClause + " " + orderByClause;
            log.info("sql = " + query); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            aggResults = template.query(query, params, SonarQubeIssueConverters.aggRowMapper("config_key", calculation));
        } else {
            query = "SELECT " + (StringUtils.isNotEmpty(key) ? key + ", " : "") + "COUNT(*) as ct from " + company + "." + table + whereClause + " " + (StringUtils.isNotEmpty(groupByClause) ? groupByClause : "") + " " + orderByClause;
            log.info("sql = " + query); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            aggResults = template.query(query, params,
                    SonarQubeIssueConverters.aggRowMapper(distinct.toString(), calculation));
        }
        return DbListResponse.of(aggResults, aggResults.size());
    }

    private String getWhereClause(List<String> conditions) {
        String whereClause = EMPTY;
        if (!conditions.isEmpty()) {
            whereClause = " WHERE " + String.join(" AND ", conditions);
        }
        return whereClause;
    }

    private void setPagingParams(Integer pageNumber, Integer pageSize, Map<String, Object> params) {
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);
    }

    protected List<String> createWhereClauseAndUpdateParams(String company, Map<String, Object> params, Long ingestedDate,
                                                            List<String> integrationIds, List<String> severities,
                                                            List<String> statuses,
                                                            List<String> projects, List<String> organizations,
                                                            List<String> components, List<String> authors,
                                                            List<String> types, List<String> tags,
                                                            Map<String, Map<String, String>> partialMatch, OUConfiguration ouConfig) {
        List<String> issueConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(severities)) {
            issueConditions.add("severity in (:severities)");
            params.put("severities", severities);
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            issueConditions.add("status in (:statuses)");
            params.put("statuses", statuses);
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            issueConditions.add("project in (:projects)");
            params.put("projects", projects);
        }
        if (CollectionUtils.isNotEmpty(organizations)) {
            issueConditions.add("organization in (:organizations)");
            params.put("organizations", organizations);
        }
        if (CollectionUtils.isNotEmpty(tags)) {
            issueConditions.add("tags && ARRAY[ :tags ]");
            params.put("tags", tags);
        }
        if (CollectionUtils.isNotEmpty(authors) || (OrgUnitHelper.doesOUConfigHaveSonarQubeAuthor(ouConfig))) { // OU: authors
            // OU first since it takes precedence over the original authors
            if (OrgUnitHelper.doesOUConfigHaveSonarQubeAuthor(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.SONARQUBE);
                if (StringUtils.isNotBlank(usersSelect)) {
                    issueConditions.add(MessageFormat.format("{0} IN (SELECT display_name FROM ({1}) l)", "author", usersSelect));
                }
            }
            // only if OU is not configured for authors
            else if (CollectionUtils.isNotEmpty(authors)) {
                issueConditions.add("author in (:authors)");
                params.put("authors", authors);
            }
        }
        if (CollectionUtils.isNotEmpty(types)) {
            issueConditions.add("type in (:types)");
            params.put("types", types);
        }
        if (CollectionUtils.isNotEmpty(components)) {
            StringBuilder componentSql = new StringBuilder();
            componentSql.append("(");
            int n = components.size();
            String component;
            for (int i = 0; i < n - 1; i++) {
                componentSql.append("component LIKE :component").append(i).append(" OR ");
                component = "%" + components.get(i) + "%";
                params.put("component" + i, component);
            }
            componentSql.append("component LIKE :component").append(n - 1).append(")");
            component = "%" + components.get(n - 1) + "%";
            params.put("component" + (n - 1), component);
            issueConditions.add(componentSql.toString());
        }
        if (ingestedDate != null) {
            issueConditions.add("ingested_at = to_timestamp(:ingested_at)");
            params.put("ingested_at", ingestedDate);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            issueConditions.add("integration_id in (:integration_ids)");
            params.put("integration_ids", integrationIds.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
        if (!MapUtils.isEmpty(partialMatch)) {
            for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatch.entrySet()) {
                String keyName = partialMatchEntry.getKey();
                if (!PARTIAL_FILTER_COLUMN.contains(keyName))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid match field " + keyName);

                Map<String, String> value = partialMatchEntry.getValue();

                String begins = value.get("$begins");
                String ends = value.get("$ends");
                String contains = value.get("$contains");

                if (StringUtils.firstNonEmpty(begins, ends, contains) != null) {
                    if (begins != null) {
                        String beingsCondition = keyName + " ILIKE :" + keyName + "_begins";
                        params.put(keyName + "_begins", begins + "%");
                        issueConditions.add(beingsCondition);
                    }

                    if (ends != null) {
                        String endsCondition = keyName + " ILIKE :" + keyName + "_ends";
                        params.put(keyName + "_ends", "%" + ends);
                        issueConditions.add(endsCondition);
                    }

                    if (contains != null) {
                        String containsCondition = keyName + " ILIKE :" + keyName + "_contains";
                        params.put(keyName + "_contains", "%" + contains + "%");
                        issueConditions.add(containsCondition);
                    }
                }
            }
        }
        return issueConditions;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddlStatements = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + SONARQUBE_PROJECT_ISSUES + " " +
                        "(" +
                        " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " project VARCHAR," +
                        " analysis_key varchar," + // deprecated
                        " integration_id INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE," +
                        " pull_request VARCHAR," +
                        " type VARCHAR," +
                        " organization VARCHAR," +
                        " key VARCHAR," +
                        " rule VARCHAR," +
                        " severity VARCHAR," +
                        " component VARCHAR," +
                        " status VARCHAR," +
                        " message VARCHAR," +
                        " effort integer," +
                        " debt INTEGER," +
                        " author VARCHAR," +
                        " tags VARCHAR[]," +
                        " ingested_at DATE," +
                        " issue_creation_date TIMESTAMP WITH TIME ZONE, " +
                        " issue_updation_date TIMESTAMP WITH TIME ZONE," +
                        " creation_date TIMESTAMP WITH TIME ZONE," +
                        " updation_date TIMESTAMP WITH TIME ZONE," +
                        " UNIQUE (key,project, ingested_at,integration_id)" +
                        ");");

        ddlStatements.forEach(ddlStatement -> template.getJdbcTemplate().execute(ddlStatement));
        return true;
    }

}
