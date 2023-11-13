package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbGithubConverters;
import io.levelops.commons.databases.converters.DbScmConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.github.DbGithubCardTransition;
import io.levelops.commons.databases.models.database.github.DbGithubProject;
import io.levelops.commons.databases.models.database.github.DbGithubProjectCard;
import io.levelops.commons.databases.models.database.github.DbGithubProjectCardWithIssue;
import io.levelops.commons.databases.models.database.github.DbGithubProjectColumn;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.GithubCardFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.parsers.ScmFilterParserCommons;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.helper.organization.OrgUnitHelper.newOUConfigForStacks;

@Log4j2
@Service
public class GithubAggService extends DatabaseService<DbGithubProject> {
    private static final String PROJECTS_TABLE = "github_projects";
    private static final String COLUMNS_TABLE = "github_project_columns";
    private static final String CARDS_TABLE = "github_cards";
    private static final String STATUSES_TABLE = "github_cards_statuses";
    private static final String ISSUES_TABLE = "scm_issues";
    private static final String PRS_TABLE = "scm_pullrequests";
    private static final Set<String> CARDS_SORTABLE_COLUMNS = Set.of("card_updated_at");

    private final NamedParameterJdbcTemplate template;
    private final GithubCardFilterParser githubCardFilterParser;
    private final ScmFilterParserCommons scmFilterParserCommons;
    private ProductsDatabaseService productsDatabaseService;
    private final Set<GithubCardFilter.DISTINCT> stackSupported = Set.of(
            GithubCardFilter.DISTINCT.column,
            GithubCardFilter.DISTINCT.card_creator,
            GithubCardFilter.DISTINCT.project,
            GithubCardFilter.DISTINCT.organization,
            GithubCardFilter.DISTINCT.project_creator,
            GithubCardFilter.DISTINCT.assignee,
            GithubCardFilter.DISTINCT.label,
            GithubCardFilter.DISTINCT.repo_id,
            GithubCardFilter.DISTINCT.issue_created,
            GithubCardFilter.DISTINCT.issue_closed);

    @Autowired
    public GithubAggService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        githubCardFilterParser = new GithubCardFilterParser();
        productsDatabaseService = new ProductsDatabaseService(dataSource, DefaultObjectMapper.get());
        scmFilterParserCommons = new ScmFilterParserCommons(productsDatabaseService);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(ScmAggService.class);
    }

    @Override
    public String insert(String company, DbGithubProject project) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String projectSql = "INSERT INTO " + company + "." + PROJECTS_TABLE +
                    " (project,project_id,integration_id,organization,description,state,creator,private,project_created_at,project_updated_at) " +
                    " VALUES (?,?,?,?,?,?,?,?,?,?) ON CONFLICT (integration_id,project_id)" +
                    " DO UPDATE SET (project,organization,description,state,creator,private,project_created_at,project_updated_at,updated_at) " +
                    " = (EXCLUDED.project,EXCLUDED.organization,EXCLUDED.description,EXCLUDED.state,EXCLUDED.creator," +
                    " EXCLUDED.private,EXCLUDED.project_created_at,EXCLUDED.project_updated_at," +
                    " trunc(extract(epoch from now()))) RETURNING id";

            try (PreparedStatement insertProject = conn.prepareStatement(projectSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 0;
                insertProject.setObject(++i, project.getProject());
                insertProject.setObject(++i, NumberUtils.toInt(project.getProjectId()));
                insertProject.setObject(++i, NumberUtils.toInt(project.getIntegrationId()));
                insertProject.setObject(++i, project.getOrganization());
                insertProject.setObject(++i, project.getDescription());
                insertProject.setObject(++i, project.getState());
                insertProject.setObject(++i, project.getCreator());
                insertProject.setObject(++i, project.getIsPrivate());
                insertProject.setObject(++i, project.getProjectCreatedAt());
                insertProject.setObject(++i, project.getProjectUpdatedAt());

                int insertedRows = insertProject.executeUpdate();
                if (insertedRows == 0)
                    throw new SQLException("Failed to insert project.");
                String insertedRowId = null;
                try (ResultSet rs = insertProject.getGeneratedKeys()) {
                    if (rs.next())
                        insertedRowId = rs.getString(1);
                }
                if (insertedRowId == null)
                    throw new SQLException("Failed to get inserted project's Id.");
                return insertedRowId;
            }
        }));
    }

    public String insertCardTransition(String company, DbGithubCardTransition transition) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String transitionSql = "INSERT INTO " + company + "." + STATUSES_TABLE +
                    " (project_id,card_id,column_id,integration_id,start_time,end_time,updater,created_at)" +
                    " VALUES(?,?,?,?,?,?,?,?) ON CONFLICT (card_id,column_id,start_time,integration_id)" +
                    " DO UPDATE SET (card_id, end_time) = (Excluded.card_id, Excluded.end_time) RETURNING id";
            try (PreparedStatement insertTransition = conn.prepareStatement(transitionSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertTransition.setObject(i++, NumberUtils.toInt(transition.getProjectId()));
                insertTransition.setObject(i++, NumberUtils.toInt(transition.getCardId()));
                insertTransition.setObject(i++, NumberUtils.toInt(transition.getColumnId()));
                insertTransition.setObject(i++, NumberUtils.toInt(transition.getIntegrationId()));
                insertTransition.setObject(i++, transition.getStartTime());
                insertTransition.setObject(i++, transition.getEndTime());
                insertTransition.setObject(i++, transition.getUpdater());
                insertTransition.setObject(i, transition.getCreatedAt());
                insertTransition.executeUpdate();
                String insertedRowId;
                try (ResultSet rs = insertTransition.getGeneratedKeys()) {
                    if (rs.next())
                        insertedRowId = rs.getString(1);
                    else {
                        Optional<DbGithubCardTransition> trans = getTransition(company, transition.getCardId(), transition.getColumnId(),
                                transition.getStartTime(), transition.getIntegrationId());
                        insertedRowId = trans.map(DbGithubCardTransition::getId).orElse(null);
                    }
                }
                if (insertedRowId == null)
                    throw new SQLException("Failed to get transition id for transition." + transition);
                return insertedRowId;
            }
        }));
    }

    public String updateCardTransition(String company, DbGithubCardTransition transition) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            DbGithubCardTransition lastUpdatedTransition = getLastUpdatedTransition(company, transition.getCardId(),
                    transition.getColumnId(), transition.getIntegrationId());
            Long startTime = 0L;
            if (lastUpdatedTransition != null)
                startTime = lastUpdatedTransition.getStartTime();
            String transitionSql = "INSERT INTO " + company + "." + STATUSES_TABLE +
                    " (project_id,card_id,column_id,integration_id,start_time,end_time,updater,created_at)" +
                    " VALUES(?,?,?,?,?,?,?,?) ON CONFLICT (card_id,column_id,start_time,integration_id)" +
                    " DO UPDATE SET (card_id, end_time) = (Excluded.card_id, Excluded.end_time) RETURNING id";
            Long finalStartTime = startTime != 0 ? startTime : transition.getStartTime();
            try (PreparedStatement insertTransition = conn.prepareStatement(transitionSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertTransition.setObject(i++, NumberUtils.toInt(transition.getProjectId()));
                insertTransition.setObject(i++, NumberUtils.toInt(transition.getCardId()));
                insertTransition.setObject(i++, NumberUtils.toInt(transition.getColumnId()));
                insertTransition.setObject(i++, NumberUtils.toInt(transition.getIntegrationId()));
                insertTransition.setObject(i++, finalStartTime);
                insertTransition.setObject(i++, transition.getEndTime());
                insertTransition.setObject(i++, transition.getUpdater());
                insertTransition.setObject(i, transition.getCreatedAt());
                insertTransition.executeUpdate();
                String insertedRowId;
                try (ResultSet rs = insertTransition.getGeneratedKeys()) {
                    if (rs.next())
                        insertedRowId = rs.getString(1);
                    else {
                        Optional<DbGithubCardTransition> trans = getTransition(company, transition.getCardId(), transition.getColumnId(),
                                finalStartTime, transition.getIntegrationId());
                        insertedRowId = trans.map(DbGithubCardTransition::getId).orElse(null);
                    }
                }
                if (insertedRowId == null)
                    throw new SQLException("Failed to get transition id for transition." + transition);
                return insertedRowId;
            }
        }));
    }

    private DbGithubCardTransition getLastUpdatedTransition(String company, String cardId, String columnId, String integrationId) {
        Validate.notBlank(cardId, "Missing card_id.");
        Validate.notBlank(columnId, "Missing column_id.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + STATUSES_TABLE
                + " WHERE card_id = :card_id AND column_id= :column_id AND"
                + " integration_id = :integ_id order by created_at DESC";
        Map<String, Object> params = Map.of("card_id", NumberUtils.toInt(cardId),
                "column_id", NumberUtils.toInt(columnId),
                "integ_id", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbGithubCardTransition> data = template.query(sql, params, DbGithubConverters.cardTransitionRowMapper());
        return data.stream().findFirst().orElse(null);
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(String company,
                                                              GithubCardFilter filter,
                                                              List<GithubCardFilter.DISTINCT> stacks,
                                                              OUConfiguration ouConfig)
            throws SQLException {
        String acrossLogString = filter.getAcross().name();
        log.info("[{}] Github Card Agg: started across '{}'", company, acrossLogString);
        DbListResponse<DbAggregationResult> result = groupByAndCalculateCards(company, filter, ouConfig);
        log.info("[{}] Github Card Agg: done across '{}' - results={}", company, acrossLogString, result.getCount());
        if (stacks == null
                || stacks.size() == 0
                || !stackSupported.contains(stacks.get(0))) {
            return result;
        }
        GithubCardFilter.DISTINCT stack = stacks.get(0);
        OUConfiguration ouConfigForStacks = ouConfig;
        List<DbAggregationResult> finalList = new ArrayList<>();
        for (DbAggregationResult record : result.getRecords()) {
            GithubCardFilter newFilter;
            switch (filter.getAcross()) {
                case column:
                    newFilter = filter.toBuilder().columns(List.of(record.getKey())).across(stack).build();
                    break;
                case card_creator:
                    newFilter = filter.toBuilder().cardCreators(List.of(record.getKey())).across(stack).build();
                    ouConfigForStacks = newOUConfigForStacks(ouConfig, "card_creators");
                    break;
                case organization:
                    newFilter = filter.toBuilder().organizations(List.of(record.getKey())).across(stack).build();
                    break;
                case project_creator:
                    newFilter = filter.toBuilder().projectCreators(List.of(record.getKey())).across(stack).build();
                    ouConfigForStacks = newOUConfigForStacks(ouConfig, "project_creators");
                    break;
                case project:
                    newFilter = filter.toBuilder().projects(List.of(record.getKey())).across(stack).build();
                    break;
                case assignee:
                    newFilter = filter.toBuilder().assignees(List.of(record.getKey())).across(stack).build();
                    ouConfigForStacks = newOUConfigForStacks(ouConfig, "assignees");
                    break;
                case repo_id:
                    newFilter = filter.toBuilder().repoIds(List.of(record.getKey())).across(stack).build();
                    break;
                case label:
                    newFilter = filter.toBuilder().labels(List.of(record.getKey())).across(stack).build();
                    break;
                case issue_created:
                case issue_closed:
                    newFilter = getFilterForTrendStack(filter.toBuilder(), record, filter.getAcross(), stack,
                            MoreObjects.firstNonNull(filter.getAggInterval(), AGG_INTERVAL.day).toString()).build();
                    break;
                default:
                    throw new SQLException("This stack is not available for github issues." + stack);
            }
            finalList.add(record.toBuilder().stacks(groupByAndCalculateCards(company, newFilter, ouConfigForStacks).getRecords()).build());
        }
        return DbListResponse.of(finalList, finalList.size());
    }

    private GithubCardFilter.GithubCardFilterBuilder getFilterForTrendStack(GithubCardFilter.GithubCardFilterBuilder githubCardFilterBuilder,
                                                                            DbAggregationResult row, GithubCardFilter.DISTINCT across,
                                                                            GithubCardFilter.DISTINCT stack, String aggInterval) throws SQLException {
        Calendar cal = Calendar.getInstance();
        long startTimeInSeconds = Long.parseLong(row.getKey());
        cal.setTimeInMillis(TimeUnit.SECONDS.toMillis(startTimeInSeconds));
        if (aggInterval.equals(AGG_INTERVAL.month.toString()))
            cal.add(Calendar.MONTH, 1);
        else if (aggInterval.equals(AGG_INTERVAL.day.toString()))
            cal.add(Calendar.DATE, 1);
        else if (aggInterval.equals(AGG_INTERVAL.year.toString()))
            cal.add(Calendar.YEAR, 1);
        else if (aggInterval.equals(AGG_INTERVAL.quarter.toString()))
            cal.add(Calendar.MONTH, 3);
        else if (aggInterval.equals(AGG_INTERVAL.week.toString()))
            cal.add(Calendar.DATE, 7);
        else
            cal.add(Calendar.DATE, 1);
        long endTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(cal.getTimeInMillis());

        ImmutablePair<Long, Long> timeRange = ImmutablePair.of(startTimeInSeconds, endTimeInSeconds);
        switch (across) {
            case issue_created:
                githubCardFilterBuilder.issueCreatedRange(timeRange);
                break;
            case issue_closed:
                githubCardFilterBuilder.issueClosedRange(timeRange);
                break;
            default:
                throw new SQLException("This across option is not available trend. Provided across: " + across);
        }

        return githubCardFilterBuilder.across(stack);
    }

    public Optional<DbGithubCardTransition> getTransition(String company, String cardId, String columnId, long startTime, String integrationId) {
        Validate.notBlank(cardId, "Missing card_id.");
        Validate.notBlank(columnId, "Missing column_id.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + STATUSES_TABLE
                + " WHERE card_id = :card_id AND column_id= :column_id AND start_time" + (startTime == 0 ? " IS NULL " : "= :start_time")
                + " AND integration_id = :integ_id";
        Map<String, Object> params = Map.of("card_id", NumberUtils.toInt(cardId),
                "column_id", NumberUtils.toInt(columnId),
                "start_time", startTime,
                "integ_id", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbGithubCardTransition> data = template.query(sql, params, DbGithubConverters.cardTransitionRowMapper());
        return data.stream().findFirst();
    }

    private Map<String, List<String>> createCardWhereClauseAndUpdateParams(String company,
                                                                           Map<String, Object> params,
                                                                           List<String> projects,
                                                                           List<String> projectCreators,
                                                                           List<String> organizations,
                                                                           List<String> projectStates,
                                                                           List<String> columns,
                                                                           List<String> cardIds,
                                                                           List<String> cardCreators,
                                                                           List<String> integrationIds,
                                                                           List<String> excludeColumns,
                                                                           List<String> currentColumns,
                                                                           Boolean isPrivateProject,
                                                                           Boolean isArchivedCard,
                                                                           List<String> repos,
                                                                           List<String> labels,
                                                                           List<String> assignees,
                                                                           ImmutablePair<Long, Long> issueClosedRange,
                                                                           ImmutablePair<Long, Long> issueCreatedRange,
                                                                           String paramSuffix,
                                                                           OUConfiguration ouConfig) {

        List<String> projectTableConditions = new ArrayList<>();
        List<String> columnTableConditions = new ArrayList<>();
        List<String> cardTableConditions = new ArrayList<>();
        List<String> statusTableConditions = new ArrayList<>();
        List<String> issueTableConditions = new ArrayList<>();
        String paramSuffixString = StringUtils.isEmpty(paramSuffix) ? "" : "_" + paramSuffix;
        if (CollectionUtils.isNotEmpty(projects)) {
            projectTableConditions.add("project IN (:projects" + paramSuffixString + ")");
            params.put("projects" + paramSuffixString, projects);
        }
        if (CollectionUtils.isNotEmpty(projectCreators) || OrgUnitHelper.doesOuConfigHaveGithubProjectCreators(ouConfig)) { // OU: project_creators
            if (OrgUnitHelper.doesOuConfigHaveGithubProjectCreators(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    projectTableConditions.add(MessageFormat.format("{0} IN (SELECT display_name FROM ({1}) l)", "creator", usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(projectCreators)) {
                projectTableConditions.add("creator IN (:project_creators" + paramSuffixString + ")");
                params.put("project_creators" + paramSuffixString, projectCreators);
            }
        }
        if (CollectionUtils.isNotEmpty(organizations)) {
            projectTableConditions.add("organization IN (:organizations" + paramSuffixString + ")");
            params.put("organizations" + paramSuffixString, organizations);
        }
        if (CollectionUtils.isNotEmpty(projectStates)) {
            projectTableConditions.add("state IN (:states" + paramSuffixString + ")");
            params.put("states" + paramSuffixString, projectStates);
        }
        if (isPrivateProject != null) {
            projectTableConditions.add("private IN (:private_project" + paramSuffixString + ")");
            params.put("private_project" + paramSuffixString, isPrivateProject);
        }
        if (CollectionUtils.isNotEmpty(columns)) {
            columnTableConditions.add("name IN (:columns" + paramSuffixString + ")");
            params.put("columns" + paramSuffixString, columns);
        }
        if (CollectionUtils.isNotEmpty(currentColumns)) {
            columnTableConditions.add("name IN (:current_columns" + paramSuffixString + ")");
            params.put("current_columns" + paramSuffixString, currentColumns);
        }
        if (CollectionUtils.isNotEmpty(excludeColumns)) {
            columnTableConditions.add("name NOT IN (:exclude_columns" + paramSuffixString + ")");
            params.put("exclude_columns" + paramSuffixString, excludeColumns);
        }
        if (CollectionUtils.isNotEmpty(cardIds)) {
            statusTableConditions.add("card_id IN (:card_ids" + paramSuffixString + ")");
            cardTableConditions.add("card_id IN (:card_ids" + paramSuffixString + ")");
            params.put("card_ids" + paramSuffixString, cardIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(cardCreators) || OrgUnitHelper.doesOuConfigHaveGithubCardCreators(ouConfig)) { // OU: card_creators
            if (OrgUnitHelper.doesOuConfigHaveGithubCardCreators(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    cardTableConditions.add(MessageFormat.format("{0} IN (SELECT display_name FROM ({1}) l)", "creator", usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(cardCreators)) {
                cardTableConditions.add("creator IN (:card_creators" + paramSuffixString + ")");
                params.put("card_creators" + paramSuffixString, cardCreators);
            }
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            projectTableConditions.add("integration_id IN (:integration_ids" + paramSuffixString + ")");
            statusTableConditions.add("integration_id IN (:integration_ids" + paramSuffixString + ")");
            params.put("integration_ids" + paramSuffixString,
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (isArchivedCard != null) {
            cardTableConditions.add("archived IN (:archived_card" + paramSuffixString + ")");
            params.put("archived_card" + paramSuffixString, isArchivedCard);
        }
        if (CollectionUtils.isNotEmpty(labels)) {
            issueTableConditions.add("labels && ARRAY[ :labels" + paramSuffixString + " ]::varchar[]");
            params.put("labels" + paramSuffixString, labels);
        }
        if (CollectionUtils.isNotEmpty(repos)) {
            issueTableConditions.add("repo_id IN (:repos" + paramSuffixString + ")");
            params.put("repos" + paramSuffixString, repos);
        }
        if (CollectionUtils.isNotEmpty(assignees) || OrgUnitHelper.doesOuConfigHaveGithubAssignees(ouConfig)) { // OU: assignees
            if (OrgUnitHelper.doesOuConfigHaveGithubAssignees(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    issueTableConditions.add(MessageFormat.format("assignees && (SELECT ARRAY(SELECT display_name FROM ({0}) l) g) ", usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(assignees)) {
                issueTableConditions.add("assignees && ARRAY[ :assignees" + paramSuffixString + " ]::varchar[]");
                params.put("assignees" + paramSuffixString, assignees);
            }
        }
        if (issueClosedRange != null) {
            if (issueClosedRange.getLeft() != null) {
                issueTableConditions.add("issue_closed_at > TO_TIMESTAMP(:issue_closed_at_start)");
                params.put("issue_closed_at_start", issueClosedRange.getLeft());
            }
            if (issueClosedRange.getRight() != null) {
                issueTableConditions.add("issue_closed_at < TO_TIMESTAMP(:issue_closed_at_end)");
                params.put("issue_closed_at_end", issueClosedRange.getRight());
            }
        }
        if (issueCreatedRange != null) {
            if (issueCreatedRange.getLeft() != null) {
                issueTableConditions.add("issue_created_at > TO_TIMESTAMP(:issue_created_at_start)");
                params.put("issue_created_at_start", issueCreatedRange.getLeft());
            }
            if (issueCreatedRange.getRight() != null) {
                issueTableConditions.add("issue_created_at < TO_TIMESTAMP(:issue_created_at_end)");
                params.put("issue_created_at_end", issueCreatedRange.getRight());
            }
        }
        return Map.of(PROJECTS_TABLE, projectTableConditions,
                COLUMNS_TABLE, columnTableConditions,
                CARDS_TABLE, cardTableConditions,
                STATUSES_TABLE, statusTableConditions,
                ISSUES_TABLE, issueTableConditions);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCards(String company, GithubCardFilter filter) {
        return groupByAndCalculateCards(company, filter, null);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCards(String company, GithubCardFilter filter, Integer pageNumber, Integer pageSize) {
        return groupByAndCalculateCards(company, filter, null, pageNumber, pageSize);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCards(String company, GithubCardFilter filter, OUConfiguration ouConfig) {
        return groupByAndCalculateCards(company, filter, ouConfig, 0, Integer.MAX_VALUE);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCards(String company, GithubCardFilter filter, OUConfiguration ouConfig, Integer pageNumber, Integer pageSize) {
        GithubCardFilter.DISTINCT across = filter.getAcross();
        Map<String, SortingOrder> sortBy = filter.getSort();
        Validate.notNull(across, "Across cant be missing for groupby query.");
        GithubCardFilter.CALCULATION calculation = filter.getCalculation();
        String filterByProductSQL = "";
        if (calculation == null)
            calculation = GithubCardFilter.CALCULATION.count;
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForGithubAgg(company, filter, params, false, ouConfig);
        }
        String calculationComponent, selectDistinctString, groupByString, orderByString;
        boolean needResolutionTimeReport = false;
        boolean needTimeAcrossColumnReport = false;
        Map<String, List<String>> conditions = createCardWhereClauseAndUpdateParams(company, params,
                filter.getProjects(), filter.getProjectCreators(), filter.getOrganizations(), filter.getProjectStates(),
                filter.getColumns(), filter.getCardIds(), filter.getCardCreators(), filter.getIntegrationIds(),
                filter.getExcludeColumns(), filter.getCurrentColumns(), filter.getPrivateProject(),
                filter.getArchivedCard(), filter.getRepoIds(), filter.getLabels(), filter.getAssignees(),
                filter.getIssueClosedRange(), filter.getIssueCreatedRange(), null, ouConfig);
        switch (calculation) {
            case resolution_time:
                calculationComponent = "COUNT(*) AS ct, PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY solve_time) as md";
                orderByString = "md DESC";
                needResolutionTimeReport = true;
                break;
            case stage_times_report:
                calculationComponent = "COUNT(*) AS ct, AVG(time_spent) AS mean_time, PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY time_spent) as md";
                orderByString = "mean_time DESC";
                needTimeAcrossColumnReport = true;
                break;
            default:
                orderByString = "ct DESC";
                calculationComponent = "COUNT(*) AS ct";
                break;
        }
        String intervalColumn = "";
        AggTimeQueryHelper.AggTimeQuery issueModAggQuery;
        AGG_INTERVAL aggInterval = filter.getAggInterval();
        if (aggInterval == null) {
            aggInterval = AGG_INTERVAL.day;
        }
        Optional<String> additionalKey = Optional.empty();
        String innerDistinctString = "";
        switch (filter.getAcross()) {
            case project:
            case project_creator:
            case organization:
            case card_creator:
                groupByString = filter.getAcross().toString();
                selectDistinctString = filter.getAcross().toString();
                innerDistinctString = groupByString;
                break;
            case repo_id:
                groupByString = "issues_repo_id";
                selectDistinctString = "issues_repo_id";
                innerDistinctString = groupByString;
                break;
            case column:
                groupByString = "column_name";
                selectDistinctString = "column_name";
                innerDistinctString = groupByString;
                break;
            case assignee:
                groupByString = "issues_assignee";
                selectDistinctString = "UNNEST(issues_assignees) AS issues_assignee"; //unnest for assignee as that is an array
                innerDistinctString = "issues_assignees";
                break;
            case label:
                groupByString = "issues_label";
                selectDistinctString = "UNNEST(issues_labels) AS issues_label"; //unnest for label as that is an array
                innerDistinctString = "issues_labels";
                break;
            case issue_closed:
                conditions.get(ISSUES_TABLE).add("issue_closed_at IS NOT NULL");
            case issue_created:
                issueModAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery(filter.getAcross().toString() +
                                "_at", filter.getAcross().toString(), aggInterval.toString(), false, true);
                intervalColumn = issueModAggQuery.getHelperColumn();
                groupByString = issueModAggQuery.getGroupBy();
                orderByString = issueModAggQuery.getOrderBy();
                selectDistinctString = issueModAggQuery.getSelect();
                additionalKey = Optional.of(issueModAggQuery.getIntervalKey());
                innerDistinctString = filter.getAcross() + "_interval";
                log.debug("issue_updated orderByString = {}", orderByString);
                break;

            default:
                Validate.notNull(null, "Invalid across field provided.");
                return DbListResponse.of(List.of(), 0);
        }
        String projectsWhere = "";
        if (conditions.get(PROJECTS_TABLE).size() > 0)
            projectsWhere = " WHERE " + String.join(" AND ", conditions.get(PROJECTS_TABLE));
        String columnsWhere = "";
        if (conditions.get(COLUMNS_TABLE).size() > 0)
            columnsWhere = " WHERE " + String.join(" AND ", conditions.get(COLUMNS_TABLE));
        String cardsWhere = "";
        if (conditions.get(CARDS_TABLE).size() > 0)
            cardsWhere = " WHERE " + String.join(" AND ", conditions.get(CARDS_TABLE));
        String statusesWhere = "";
        if (conditions.get(STATUSES_TABLE).size() > 0)
            statusesWhere = " WHERE " + String.join(" AND ", conditions.get(STATUSES_TABLE));
        String issuesWhere = "";
        if (conditions.get(ISSUES_TABLE).size() > 0)
            issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));
        String projectJoin = " INNER JOIN ( SELECT id, project_id, project, organization, "
                + " creator as project_creator, integration_id as project_integration_id FROM " + company + "."
                + PROJECTS_TABLE + projectsWhere + " ) AS projects ON statuses.project_id"
                + " = projects.project_id AND statuses.integration_id=projects.project_integration_id ";
        String columnsJoin = " INNER JOIN ( SELECT id, project_id, column_id,name as column_name FROM "
                + company + "." + COLUMNS_TABLE + columnsWhere + " ) AS columns ON"
                + " statuses.column_id = columns.column_id AND projects.id=columns.project_id ";
        String cardsJoin = " INNER JOIN ( SELECT id, current_column_id,creator as card_creator,"
                + " card_id as project_card_id, card_created_at, issue_id FROM " + company + "." + CARDS_TABLE
                + cardsWhere + " ) AS cards ON statuses.card_id=cards.project_card_id ";
        String issuesJoin = " INNER JOIN (SELECT id,issue_id as issues_issue_id,repo_id as issues_repo_id,labels as issues_labels,"
                + " assignees as issues_assignees,issue_closed_at,issue_created_at FROM " + company
                + "." + ISSUES_TABLE + issuesWhere + " ) AS issues ON issues.issues_issue_id = cards.issue_id ";
        String resolutionTimeColumn = "";
        if (needResolutionTimeReport) {
            resolutionTimeColumn = ", card_id, max(COALESCE(end_time," + currentTime + "))"
                    + "-min(COALESCE(start_time,card_created_at)) as solve_time ";
            if (CollectionUtils.isNotEmpty(filter.getExcludeColumns()))
                resolutionTimeColumn = ", card_id, max(COALESCE(end_time," + currentTime + "))"
                        + "-min(COALESCE(start_time,card_created_at))-COALESCE((SELECT SUM(end_time - start_time)"
                        + " AS exclude_time FROM " + company + "." + STATUSES_TABLE
                        + " WHERE integration_id IN (:integration_ids)"
                        + " AND column_id IN (Select column_id from " + company + "." + COLUMNS_TABLE
                        + " WHERE name IN (:exclude_columns))),0) AS solve_time ";
        }
        String columnTime = "";
        if (needTimeAcrossColumnReport) {
            columnTime = ", card_id, max(COALESCE(end_time," + currentTime + "))"
                    + "-min(COALESCE(start_time,card_created_at)) as time_spent ";
        }
        if (StringUtils.isEmpty(filterByProductSQL)) {
            String needResolutionSql = needResolutionTimeReport ? " SELECT " + innerDistinctString
                    + resolutionTimeColumn : "";
            String needColumnSql = needTimeAcrossColumnReport ? " SELECT " + innerDistinctString + columnTime : "";

            filterByProductSQL = needResolutionSql + needColumnSql
                    + (StringUtils.isEmpty(needResolutionSql) && StringUtils.isEmpty(needColumnSql) ? "  SELECT * " : " FROM ( SELECT * ")
                    + intervalColumn
                    + " FROM " + company + "." + STATUSES_TABLE + " as statuses "
                    + projectJoin
                    + columnsJoin
                    + cardsJoin
                    + issuesJoin
                    + statusesWhere
                    + (needResolutionTimeReport || needTimeAcrossColumnReport ? ") temp GROUP BY card_id," + innerDistinctString : "");
        }
        if (MapUtils.isNotEmpty(sortBy)) {
            orderByString = getOrderBy(sortBy, across, calculation);
        }

        List<DbAggregationResult> results = List.of();
        Integer count = 0;
        if (pageSize > 0) {
            String intrSql = "SELECT " + selectDistinctString + "," + calculationComponent + " FROM ( "
                    + filterByProductSQL + "  ) a GROUP BY " + groupByString;
            String sql = intrSql + " ORDER BY " + orderByString
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            String key = filter.getAcross().toString();
            switch (key) {
                case "column":
                    key = "column_name";
                    break;
                case "repo_id":
                    key = "issues_repo_id";
                    break;
                case "assignee":
                    key = "issues_assignee";
                    break;
                case "label":
                    key = "issues_label";
                    break;
            }
            results = template.query(sql, params, DbGithubConverters.distinctCardRowMapper(
                    key, calculation, additionalKey));
            String countSql = "SELECT COUNT(*) FROM ( "+ intrSql +" ) i";
            count = template.queryForObject(countSql, params, Integer.class);
        }
        return DbListResponse.of(results, count);

    }

    private String getOrderBy(Map<String, SortingOrder> sortBy, GithubCardFilter.DISTINCT across, GithubCardFilter.CALCULATION calculation) {
        String groupByField = sortBy.keySet().stream().findFirst().get();
        SortingOrder sortOrder = sortBy.values().stream().findFirst().get();
        if (!across.toString().equals(groupByField)) {
            switch (calculation) {
                case resolution_time:
                    Validate.isTrue((calculation.toString().equals(groupByField)), "Invalid sort option");
                    return "md " + sortOrder + " NULLS LAST";
                case stage_times_report:
                    Validate.isTrue((calculation.toString().equals(groupByField)), "Invalid sort option");
                    return "mean_time " + sortOrder + " NULLS LAST";
                case count:
                    Validate.isTrue((calculation.toString().equals(groupByField)), "Invalid sort option");
                    return "ct " + sortOrder + " NULLS LAST";
            }
        }
        switch (across) {
            case repo_id:
                return " LOWER( issues_repo_id ) " + sortOrder + " NULLS LAST";
            case label:
                return " LOWER( UNNEST(issues_labels) ) " + sortOrder + " NULLS LAST";
            case column:
                return " LOWER( column_name ) " + sortOrder + " NULLS LAST";
            case assignee:
                return " LOWER( UNNEST(issues_assignees) ) " + sortOrder + " NULLS LAST";
            default:
                break;
        }
        return " LOWER(" + groupByField + ") " + sortOrder + " NULLS LAST";
    }

    public String getUnionSqlForGithubAgg(String company, GithubCardFilter reqFilter,
                                          Map<String, Object> params, boolean isListQuery, OUConfiguration ouConfig) {
        Map<String, List<String>> conditions;
        long currentTime = (new Date()).toInstant().getEpochSecond();
        int paramSuffix = 1;
        List<String> listOfUnionSqls = new ArrayList<>();
        Map<Integer, Map<String, Object>> integFiltersMap = null;
        try {
            integFiltersMap = (reqFilter.getOrgProductIds() == null) ? Map.of() :
                    scmFilterParserCommons.getProductFilters(company, reqFilter.getOrgProductIds());
        } catch (SQLException e) {
            log.warn("Cannot fetch list of filter for company {}, {}", company, e);
        }
        if (MapUtils.isEmpty(integFiltersMap) || integFiltersMap.values().stream().allMatch(Objects::isNull))
            return null;
        for (Integer integrationId : integFiltersMap.keySet()) {
            GithubCardFilter githubCardFilter = githubCardFilterParser.merge(integrationId, reqFilter, integFiltersMap.get(integrationId));
            conditions = createCardWhereClauseAndUpdateParams(company, params,
                    githubCardFilter.getProjects(), githubCardFilter.getProjectCreators(), githubCardFilter.getOrganizations(), githubCardFilter.getProjectStates(),
                    githubCardFilter.getColumns(), githubCardFilter.getCardIds(), githubCardFilter.getCardCreators(), githubCardFilter.getIntegrationIds(),
                    githubCardFilter.getExcludeColumns(), githubCardFilter.getCurrentColumns(), githubCardFilter.getPrivateProject(),
                    githubCardFilter.getArchivedCard(), githubCardFilter.getRepoIds(), githubCardFilter.getLabels(),
                    githubCardFilter.getAssignees(), githubCardFilter.getIssueClosedRange(), githubCardFilter.getIssueCreatedRange(), String.valueOf(paramSuffix++), ouConfig);
            listOfUnionSqls.add(githubCardFilterParser.getSqlStmt(company, conditions, githubCardFilter, isListQuery, currentTime));
        }
        return String.join(" UNION ", listOfUnionSqls);
    }


    public DbListResponse<DbGithubProjectCardWithIssue> list(String company,
                                                             GithubCardFilter filter,
                                                             Map<String, SortingOrder> sortBy,
                                                             Integer pageNumber,
                                                             Integer pageSize) throws SQLException {
        return list(company, filter, null, sortBy, pageNumber, pageSize);
    }

    public DbListResponse<DbGithubProjectCardWithIssue> list(String company,
                                                             GithubCardFilter filter,
                                                             OUConfiguration ouConfig,
                                                             Map<String, SortingOrder> sortBy,
                                                             Integer pageNumber,
                                                             Integer pageSize)
            throws SQLException {
        Map<String, Object> params = new HashMap<>();
        String filterByProductSQL = "";
        Map<String, List<String>> conditions = createCardWhereClauseAndUpdateParams(company, params,
                filter.getProjects(), filter.getProjectCreators(), filter.getOrganizations(), filter.getProjectStates(),
                filter.getColumns(), filter.getCardIds(), filter.getCardCreators(), filter.getIntegrationIds(),
                filter.getExcludeColumns(), filter.getCurrentColumns(), filter.getPrivateProject(), filter.getArchivedCard(),
                filter.getRepoIds(), filter.getLabels(), filter.getAssignees(), filter.getIssueClosedRange(), filter.getIssueCreatedRange(),
                null, ouConfig);
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (CARDS_SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "card_updated_at";
                })
                .orElse("card_updated_at");
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForGithubAgg(company, filter, params, true, ouConfig);
        }
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String projectsWhere = "";
        if (conditions.get(PROJECTS_TABLE).size() > 0)
            projectsWhere = " WHERE " + String.join(" AND ", conditions.get(PROJECTS_TABLE));
        String columnsWhere = "";
        if (conditions.get(COLUMNS_TABLE).size() > 0)
            columnsWhere = " WHERE " + String.join(" AND ", conditions.get(COLUMNS_TABLE));
        String cardsWhere = "";
        if (conditions.get(CARDS_TABLE).size() > 0)
            cardsWhere = " WHERE " + String.join(" AND ", conditions.get(CARDS_TABLE));
        String statusesWhere = "";
        if (conditions.get(STATUSES_TABLE).size() > 0)
            statusesWhere = " WHERE " + String.join(" AND ", conditions.get(STATUSES_TABLE));
        String issuesWhere = "";
        if (conditions.get(ISSUES_TABLE).size() > 0)
            issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));

        String statusesSql = " (SELECT * from " + company + "." + STATUSES_TABLE + statusesWhere + ") as statuses ";
        String projectJoin = " INNER JOIN ( SELECT id, project_id, integration_id as project_integration_id FROM " + company + "."
                + PROJECTS_TABLE + projectsWhere + " ) AS projects ON statuses.project_id = projects.project_id and statuses.integration_id = projects.project_integration_id ";
        String columnsJoin = " INNER JOIN ( SELECT id as column_row_id, column_id, project_id FROM "
                + company + "." + COLUMNS_TABLE + columnsWhere + " ) AS columns ON statuses.column_id = columns.column_id and projects.id = columns.project_id ";
        String cardsJoin = " INNER JOIN ( SELECT id, current_column_id, archived, creator, card_id as project_card_id," +
                " card_created_at, content_url, card_updated_at, updated_at, issue_id FROM " + company + "." + CARDS_TABLE + cardsWhere
                + " ) as cards ON statuses.card_id = cards.project_card_id";
        String issuesJoin = " INNER JOIN (SELECT id, title as issue_title, state as issue_state, issue_id as issues_issue_id, " +
                "repo_id as issues_repo_id,labels as issues_labels, number as issues_number,assignees as issues_assignees, " +
                "issue_closed_at,issue_created_at FROM " + company + "." + ISSUES_TABLE + issuesWhere +
                " ) AS issues ON issues.issues_issue_id = cards.issue_id ";
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT  distinct on (card_id) card_id, current_column_id, archived, creator, content_url," +
                    " card_created_at, card_updated_at, created_at, updated_at, issues_issue_id, issues_repo_id, issues_number," +
                    " issue_title, issue_state, issues_assignees, issues_labels, issue_created_at, issue_closed_at FROM ( "
                    + statusesSql + projectJoin + columnsJoin + cardsJoin + issuesJoin + " ) x order by card_id, card_updated_at";
        }
        List<DbGithubProjectCardWithIssue> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT * FROM (" + filterByProductSQL + ") a"
                    + " ORDER BY " + sortByKey + " " + sortOrder.toString()
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbGithubConverters.issueCardWIthIssueRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM (" + filterByProductSQL + ") a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean update(String company, DbGithubProject project) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbGithubProject> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Optional<DbGithubProject> getProject(String company, String projectId, String integrationId) {
        Validate.notBlank(projectId, "Missing projectId.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + PROJECTS_TABLE
                + " WHERE project_id = :projectId AND integration_id = :integid";
        Map<String, Object> params = Map.of(
                "projectId", NumberUtils.toInt(projectId), "integid", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbGithubProject> data = template.query(sql, params, DbGithubConverters.projectRowMapper());
        return data.stream().findFirst();
    }

    @Override
    public DbListResponse<DbGithubProject> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String insertColumn(String company, DbGithubProjectColumn column) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String columnSql = "INSERT INTO " + company + "." + COLUMNS_TABLE +
                    " (project_id,column_id,name,column_created_at,column_updated_at) VALUES (?,?,?,?,?) " +
                    " ON CONFLICT (project_id,column_id) DO UPDATE SET (name,column_created_at,column_updated_at,updated_at) " +
                    " = (EXCLUDED.name,EXCLUDED.column_created_at,EXCLUDED.column_updated_at," +
                    " trunc(extract(epoch from now()))) RETURNING id";

            try (PreparedStatement insertColumn = conn.prepareStatement(columnSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 0;
                insertColumn.setObject(++i, UUID.fromString(column.getProjectId()));
                insertColumn.setObject(++i, NumberUtils.toInt(column.getColumnId()));
                insertColumn.setObject(++i, column.getName());
                insertColumn.setObject(++i, column.getColumnCreatedAt());
                insertColumn.setObject(++i, column.getColumnUpdatedAt());

                int insertedRows = insertColumn.executeUpdate();
                if (insertedRows == 0)
                    throw new SQLException("Failed to insert column.");
                String insertedRowId = null;
                try (ResultSet rs = insertColumn.getGeneratedKeys()) {
                    if (rs.next())
                        insertedRowId = rs.getString(1);
                }
                if (insertedRowId == null)
                    throw new SQLException("Failed to get inserted column's Id.");
                return insertedRowId;
            }
        }));
    }

    public Optional<DbGithubProjectColumn> getColumn(String company, String projectRowId, String columnId) {
        Validate.notBlank(projectRowId, "Missing projectId.");
        Validate.notBlank(columnId, "Missing columnId.");
        String sql = "SELECT * FROM " + company + "." + COLUMNS_TABLE
                + " WHERE project_id = :projectId AND column_id = :columnId";
        Map<String, Object> params = Map.of(
                "projectId", UUID.fromString(projectRowId), "columnId", NumberUtils.toInt(columnId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbGithubProjectColumn> data = template.query(sql, params, DbGithubConverters.columnRowMapper());
        return data.stream().findFirst();
    }

    public DbListResponse<DbGithubProjectColumn> listColumns(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Optional<DbScmIssue> getIssue(String company, String integrationId, String repoId, String issueNumber) {
        Validate.notBlank(integrationId, "Missing integration id");
        Validate.notBlank(issueNumber, "Missing issue number");
        Validate.notBlank(repoId, "Missing repo id");
        String sql = "SELECT * FROM " + company + "." + ISSUES_TABLE
                + " WHERE number = :issueNumber AND repo_id = :repoId AND integration_id = :integrationId AND project = :repoId";
        Map<String, Object> params = Map.of(
                "issueNumber", issueNumber, "repoId", repoId, "integrationId", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbScmIssue> data = template.query(sql, params, DbScmConverters.issueRowMapper());
        return data.stream().findFirst();
    }

    private Optional<DbScmPullRequest> getPR(String company, String integrationId, String repoId, String prNumber) {
        Validate.notBlank(integrationId, "Missing integration id");
        Validate.notBlank(prNumber, "Missing pr number");
        Validate.notBlank(repoId, "Missing repo id");
        String sql = "SELECT * , scm_pullrequests.metadata->>'pr_link' as pr_link FROM " + company + "." + PRS_TABLE
                + " WHERE number = :prNumber AND repo_id && ARRAY[ :repoId ]::varchar[] AND integration_id = :integrationId AND project = :repoId";
        Map<String, Object> params = Map.of(
                "prNumber", prNumber, "repoId", repoId, "integrationId", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbScmPullRequest> data = template.query(sql, params, DbScmConverters.prRowMapper());
        return data.stream().findFirst();
    }

    public String insertCard(String company, String integrationId, DbGithubProjectCard card) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String cardSql = "INSERT INTO " + company + "." + CARDS_TABLE +
                    " (current_column_id,card_id,archived,creator,content_url,issue_id,pr_id,card_created_at,card_updated_at) " +
                    " VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT (current_column_id,card_id)" +
                    " DO UPDATE SET (archived,creator,content_url,issue_id,card_created_at,card_updated_at,updated_at) " +
                    " = (EXCLUDED.archived,EXCLUDED.creator,EXCLUDED.content_url,EXCLUDED.issue_id,EXCLUDED.card_created_at,EXCLUDED.card_updated_at," +
                    " trunc(extract(epoch from now()))) RETURNING id";
            String issueId = null;
            String prId = null;
            if (card.getNumber() != null && card.getRepoId() != null) {
                issueId = getIssue(company, integrationId, card.getRepoId(), card.getNumber()).map(DbScmIssue::getIssueId).orElse(null);
                prId = getPR(company, integrationId, card.getRepoId(), card.getNumber()).map(DbScmPullRequest::getNumber).orElse(null);
            }
            try (PreparedStatement insertCard = conn.prepareStatement(cardSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 0;
                insertCard.setObject(++i, UUID.fromString(card.getCurrentColumnId()));
                insertCard.setObject(++i, NumberUtils.toInt(card.getCardId()));
                insertCard.setObject(++i, card.getArchived());
                insertCard.setObject(++i, card.getCreator());
                insertCard.setObject(++i, card.getContentUrl());
                insertCard.setObject(++i, issueId);
                insertCard.setObject(++i, prId);
                insertCard.setObject(++i, card.getCardCreatedAt());
                insertCard.setObject(++i, card.getCardUpdatedAt());

                int insertedRows = insertCard.executeUpdate();
                if (insertedRows == 0)
                    throw new SQLException("Failed to insert card.");
                String insertedRowId = null;
                try (ResultSet rs = insertCard.getGeneratedKeys()) {
                    if (rs.next())
                        insertedRowId = rs.getString(1);
                }
                if (insertedRowId == null)
                    throw new SQLException("Failed to get inserted card's Id.");
                return insertedRowId;
            }
        }));
    }

    public Optional<DbGithubProjectCard> getCard(String company, String currentColumnRowId, String cardId) {
        Validate.notBlank(currentColumnRowId, "Missing currentColumnId.");
        Validate.notBlank(cardId, "Missing cardId.");
        String sql = "SELECT * FROM " + company + "." + CARDS_TABLE
                + " WHERE current_column_id = :currentColumnId AND card_id = :cardId";
        Map<String, Object> params = Map.of(
                "currentColumnId", UUID.fromString(currentColumnRowId), "cardId", NumberUtils.toInt(cardId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbGithubProjectCard> data = template.query(sql, params, DbGithubConverters.cardRowMapper());
        return data.stream().findFirst();
    }

    public DbListResponse<DbGithubProjectCard> listCards(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Stream<DbGithubProjectCard> streamCardsWithNullIssueId(String company, String integrationId) {
        Validate.notBlank(company, "Missing company.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + CARDS_TABLE + " cards, "
                + company + "." + COLUMNS_TABLE + " columns, " + company + "." + PROJECTS_TABLE + " projects "
                + " WHERE projects.integration_id = :integrationId AND projects.id = columns.project_id "
                + " AND columns.id = cards.current_column_id AND cards.issue_id IS NULL";
        Map<String, Object> params = Map.of("integrationId", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbGithubProjectCard> cards = template.query(sql, params, DbGithubConverters.cardRowMapper());
        return cards.stream();
    }

    public void updateCardIssueId(String company, String cardId, String issueId) {
        template.update("UPDATE " + company + "." + CARDS_TABLE +
                        " SET issue_id = :issueId WHERE id = :cardId",
                Map.of("issueId", issueId, "cardId", UUID.fromString(cardId)));
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + PROJECTS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    project_id INTEGER NOT NULL,\n" +
                        "    project VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    organization VARCHAR NOT NULL,\n" +
                        "    description VARCHAR,\n" +
                        "    state VARCHAR NOT NULL,\n" +
                        "    creator VARCHAR NOT NULL,\n" +
                        "    private BOOLEAN,\n" +
                        "    project_created_at BIGINT NOT NULL,\n" +
                        "    project_updated_at BIGINT NOT NULL,\n" +
                        "    created_at BIGINT NOT NULL DEFAULT extract(epoch from now()),\n" +
                        "    updated_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (integration_id,project_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + PROJECTS_TABLE + "_project_integration_id_compound_idx " +
                        "on " + company + "." + PROJECTS_TABLE + "(project,integration_id)",
                "CREATE TABLE IF NOT EXISTS " + company + "." + COLUMNS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    project_id UUID NOT NULL REFERENCES "
                        + company + "." + PROJECTS_TABLE + "(id) ON DELETE CASCADE,\n" +
                        "    column_id INTEGER NOT NULL,\n" +
                        "    name VARCHAR NOT NULL,\n" +
                        "    column_created_at BIGINT NOT NULL,\n" +
                        "    column_updated_at BIGINT NOT NULL,\n" +
                        "    created_at BIGINT NOT NULL DEFAULT extract(epoch from now()),\n" +
                        "    updated_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (column_id,project_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + COLUMNS_TABLE + "_column_id_project_id_compound_idx " +
                        "on " + company + "." + COLUMNS_TABLE + "(column_id,project_id)",
                "CREATE TABLE IF NOT EXISTS " + company + "." + CARDS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    current_column_id UUID NOT NULL REFERENCES "
                        + company + "." + COLUMNS_TABLE + "(id) ON DELETE CASCADE,\n" +
                        "    card_id INTEGER NOT NULL,\n" +
                        "    archived BOOLEAN,\n" +
                        "    creator VARCHAR NOT NULL,\n" +
                        "    content_url VARCHAR,\n" +
                        "    issue_id VARCHAR,\n" +
                        "    pr_id VARCHAR,\n" +
                        "    card_created_at BIGINT NOT NULL,\n" +
                        "    card_updated_at BIGINT NOT NULL,\n" +
                        "    created_at BIGINT NOT NULL DEFAULT extract(epoch from now()),\n" +
                        "    updated_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (current_column_id,card_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + CARDS_TABLE + "_card_id_current_column_id_compound_idx " +
                        "on " + company + "." + CARDS_TABLE + "(card_id,current_column_id)",
                "CREATE TABLE IF NOT EXISTS " + company + "." + STATUSES_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    project_id INTEGER NOT NULL,\n" +
                        "    card_id INTEGER NOT NULL,\n" +
                        "    column_id INTEGER NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    start_time BIGINT,\n" +
                        "    end_time BIGINT,\n" +
                        "    updater VARCHAR NOT NULL,\n" +
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (card_id,column_id,start_time,integration_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + STATUSES_TABLE + "_card_id_integration_id_compound_idx " +
                        "on " + company + "." + STATUSES_TABLE + "(card_id,integration_id)");

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}