package io.levelops.commons.databases.services.dev_productivity;

import com.google.common.collect.Maps;
import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.business_alignment.BaJiraAggsQueryBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import io.levelops.commons.utils.StringJoiner;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.JiraIssueService.*;

@Log4j2
@Service
public class JiraFeatureHandlerService {

    private final NamedParameterJdbcTemplate template;
    private final BaJiraAggsQueryBuilder baJiraAggsQueryBuilder;
    private final JiraConditionsBuilder jiraConditionsBuilder;

    @Autowired
    public JiraFeatureHandlerService(DataSource dataSource, BaJiraAggsQueryBuilder baJiraAggsQueryBuilder, JiraConditionsBuilder jiraConditionsBuilder){
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.baJiraAggsQueryBuilder = baJiraAggsQueryBuilder;
        this.jiraConditionsBuilder = jiraConditionsBuilder;
    }


    public List<DbAggregationResult> getJiraFeatureResponse(String company, JiraIssuesFilter jiraIssuesFilter, String paramPrefix, List<String> developmentStages, DevProductivityProfile.FeatureType featureType) {
        Map<String,Object> params = Maps.newHashMap();
        String ticketCategorySQL = "";
        if(CollectionUtils.isNotEmpty(jiraIssuesFilter.getTicketCategorizationFilters()) && CollectionUtils.isNotEmpty(jiraIssuesFilter.getTicketCategories())){
            ticketCategorySQL = baJiraAggsQueryBuilder.generateTicketCategorySql(company,jiraIssuesFilter,params,Instant.now().getEpochSecond());
        }
        String query = "";
        String jiraIssuesSQL = generateJiraIssuesSQL(company,jiraIssuesFilter,null,paramPrefix,params, Instant.now().getEpochSecond(), ticketCategorySQL,true,developmentStages);
        String innerSelect = "select issue_key, assignee, round (assignee_time/sum(assignee_time) over (partition by issue_key),2) as ticket_portion, (story_points * round (assignee_time/sum(assignee_time) over (partition by issue_key),2)) as story_points_portion\n";
        String innerSQL = innerSelect +"from (\n" + jiraIssuesSQL +") as assignee_status_time \n";
        String historicalAssigneeCond =  baJiraAggsQueryBuilder.generateHistoricalAssigneesFilter(company, null, paramPrefix, params, jiraIssuesFilter.getAssignees()).orElse("");
        String finalWhere = StringUtils.isEmpty(historicalAssigneeCond) ? "" : " where "+historicalAssigneeCond;
        String finalGroupBy = "";
        if(featureType == DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME){
            String outerGroupBy = " group by assignee";
            String outerSQL = "select assignee,sum(assignee_time) as total_time, count(*) as total_tickets from ( \n"
                    + jiraIssuesSQL +") as assignee_status_time \n";
            finalGroupBy = " group by assignee, total_time, total_tickets";
            query = "select assignee,total_time/total_tickets as time_spent_per_ticket from(\n"
            + outerSQL + outerGroupBy + ") as a" + finalWhere + finalGroupBy;
            query = StringSubstitutor.replace(query, Map.of("company", company));
            log.info("query = {}",query);
            log.info("params = {}",params);
            return template.query(query, params, new RowMapper<DbAggregationResult>() {
                @Override
                public DbAggregationResult mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return DbAggregationResult.builder()
                            .timeSpentPerTicket(Math.round(rs.getDouble("time_spent_per_ticket")))
                            .build();
                }
            });
        }else{
            finalGroupBy = " group by assignee";
            query = "select assignee,sum(ticket_portion) as ticket_count, sum(story_points_portion) as story_points from ("
                    +innerSQL
                    +")   as a "+ finalWhere + finalGroupBy;
            query = StringSubstitutor.replace(query, Map.of("company", company));
            log.info("query = {}",query);
            log.info("params = {}",params);
            return template.query(query, params, new RowMapper<DbAggregationResult>() {
                @Override
                public DbAggregationResult mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return DbAggregationResult.builder().totalTickets(Math.round(rs.getDouble("ticket_count")))
                            .totalStoryPoints(Math.round(rs.getDouble("story_points")))
                            .build();
                }
            });
        }
    }

    public DbListResponse<DbJiraIssue> getFeatureBreakDown(String company, JiraIssuesFilter jiraIssuesFilter, String paramPrefix, List<String> developmentStages, Map<String, SortingOrder> sortBy, DevProductivityProfile.FeatureType featureType, Boolean isSpeedUp, Integer pageNumber, Integer pageSize) {
        Map<String,Object> params = Maps.newHashMap();
        String ticketCategorySQL = "";
        if(CollectionUtils.isNotEmpty(jiraIssuesFilter.getTicketCategorizationFilters()) && CollectionUtils.isNotEmpty(jiraIssuesFilter.getTicketCategories())){
            ticketCategorySQL = baJiraAggsQueryBuilder.generateTicketCategorySql(company,jiraIssuesFilter,params,Instant.now().getEpochSecond());
        }
        String jiraIssuesSQL = generateJiraIssuesSQL(company,jiraIssuesFilter,null,paramPrefix,params, Instant.now().getEpochSecond(), ticketCategorySQL,true,developmentStages);
        String innerSelect = "select issue_key, assignee, round (assignee_time/sum(assignee_time) over (partition by issue_key),2) as ticket_portion, (story_points * round (assignee_time/sum(assignee_time) over (partition by issue_key),2)) as story_points_portion\n";
        String innerSQL = innerSelect +"from (\n" + jiraIssuesSQL +") as assignee_status_time \n";
        String historicalAssigneeCond =  baJiraAggsQueryBuilder.generateHistoricalAssigneesFilter(company, null, paramPrefix, params, jiraIssuesFilter.getAssignees()).orElse("");
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        String query = "";
        String queryWithLimit = "";
        String limitClause = "   OFFSET :skip LIMIT :limit";
        Map<String,Long> jiraIngestedAtByIntegrationId = jiraIssuesFilter.getIngestedAtByIntegrationId();
        Map<String, List<String>> conditions;

        if(isSpeedUp){
           conditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params, paramPrefix, jiraIssuesFilter, null, jiraIngestedAtByIntegrationId.values().stream().findFirst().get(), "issues.", null);
        }else {
           conditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params, paramPrefix,
                    JiraIssuesFilter.builder().ingestedAtByIntegrationId(jiraIngestedAtByIntegrationId).build(), null, jiraIngestedAtByIntegrationId.values().stream().findFirst().get(), "issues.", null);
        }
        List<String> jiraIssueJoinConditions = conditions.get(ISSUES_TABLE);
        if(featureType == DevProductivityProfile.FeatureType.NUMBER_OF_STORY_POINTS_DELIVERED_PER_MONTH){
            jiraIssueJoinConditions.add("issues.story_points IS NOT NULL");
        }
        String jiraIssueJoinConditionsStr = jiraIssueJoinConditions.stream().collect(Collectors.joining(" AND "));
        List<DbJiraIssue> results = null;
        if(featureType == DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME){

            if(isSpeedUp) {
                log.info("using optimised query for feature {}, for {}", featureType.name(), company);
                List<String> conditionList = jiraIssueJoinConditions.stream().filter( str -> !str.contains("issues.assignee_id")).collect(Collectors.toList());
                String whereCond = generateWithIssuesFilter(params, paramPrefix, conditionList,  jiraIssuesFilter.getAssignees()).orElse("");
                String base_issue = "WITH issues AS ( SELECT distinct (issues.*) FROM " + company + ".jira_issues AS issues " +
                        " inner join " + company + ".jira_issue_assignees as a on issues.key = a.issue_key "+whereCond+")";
                String jiraNewIssuesSQL = generateOptimizedJiraIssuesSQL(company,jiraIssuesFilter,null,paramPrefix,params, Instant.now().getEpochSecond(), ticketCategorySQL,true,developmentStages, conditions.get(FINAL_TABLE));
                query = base_issue +" select issues.*, a.assignee_time \n" +
                        "   from issues\n" +
                        "   inner join (\n" + jiraNewIssuesSQL  + " as a"+
                        "   ON a.issue_key = issues.key "+
                        "   AND " + historicalAssigneeCond + "\n";
            }else {
                query = "   select issues.*,a.assignee_time\n" +
                        "   from ${company}.jira_issues issues\n" +
                        "   inner join (\n" +
                        jiraIssuesSQL + ") as a \n" +
                        "   ON a.issue_key = issues.key AND " + jiraIssueJoinConditionsStr + "\n" +
                        "   AND " + historicalAssigneeCond + "\n";
            }
            query = StringSubstitutor.replace(query, Map.of("company", company));
            queryWithLimit = query+limitClause;
            log.info("query = {}",queryWithLimit);
            log.info("params = {}",params);
            results = template.query(queryWithLimit, params, DbJiraIssueConverters.listRowMapper(false, false, false, false, false, false, true));
        }else{
            if(isSpeedUp) {
                log.info("using optimised query for feature {}, for {}", featureType.name(), company);
                List<String> conditionList = jiraIssueJoinConditions.stream().filter( str -> !str.contains("issues.assignee_id")).collect(Collectors.toList());
                String whereCond = generateWithIssuesFilter(params, paramPrefix, conditionList,  jiraIssuesFilter.getAssignees()).orElse("");
                String base_issue = "WITH issues AS ( SELECT distinct (issues.*) FROM " + company + ".jira_issues AS issues " +
                        " inner join " + company + ".jira_issue_assignees as a on issues.key = a.issue_key "+whereCond+")";
                String jiraNewIssuesSQL = generateOptimizedJiraIssuesSQL(company,jiraIssuesFilter,null,paramPrefix,params, Instant.now().getEpochSecond(), ticketCategorySQL,true,developmentStages, conditions.get(FINAL_TABLE));
                String innerNewSelect = "select issue_key, assignee, round (assignee_time/sum(assignee_time) over (partition by issue_key),2) as ticket_portion, (story_points * round (assignee_time/sum(assignee_time) over (partition by issue_key),2)) as story_points_portion\n";
                String innerNewSQL = innerNewSelect +"from (\n" + jiraNewIssuesSQL + " as a" ;
                query = base_issue +" select issues.*, a.issue_key, a.assignee,  ticket_portion, story_points_portion \n" +
                        "   from issues\n" +
                        "   inner join (\n" + innerNewSQL + ") as a " +
                        "   ON a.issue_key = issues.key "+
                        "   AND " + historicalAssigneeCond + "\n";
            }else {

                query = "select issues.*, a.issue_key, a.assignee,  ticket_portion, story_points_portion \n" +
                        "   from ${company}.jira_issues issues\n" +
                        "   inner join (\n" + innerSQL + ") as a " +
                        "   ON a.issue_key = issues.key AND " + jiraIssueJoinConditionsStr + " \n" +
                        "   AND " + historicalAssigneeCond + "\n";
            }
            query = StringSubstitutor.replace(query, Map.of("company", company));
            queryWithLimit = query + limitClause;
            log.info("query = {}",queryWithLimit);
            log.info("params = {}",params);
            results = template.query(queryWithLimit, params, DbJiraIssueConverters.listRowMapper(false, false, false, false, true, true, false));
        }

        Integer count = results.size() + pageNumber * pageSize;

        if(results.size() == pageSize ) {
            String countSql = "SELECT COUNT(*) FROM (" + query + ") as x";
            log.info("countSql = {}", countSql);
            log.info("params = {}", params);
            count = template.queryForObject(countSql, params, Integer.class);
        }

        return DbListResponse.of(results,count);
    }

    private String generateWithClause(String company, JiraIssuesFilter jiraIssuesFilter, String ticketCategorySQL, String paramPrefix, List<String> developmentStages, Map<String,Object> params) {
        String jiraIssuesSQL = generateJiraIssuesSQL(company,jiraIssuesFilter,null,paramPrefix,params, Instant.now().getEpochSecond(), ticketCategorySQL,true,developmentStages);
        log.info("jiraIssuesSQL = {} ",jiraIssuesSQL);
        log.info("params = {} ",params);
        String timeInStatusesSQL = generateTimeInStatusesSQL(company,jiraIssuesFilter.getIntegrationIds(),developmentStages,"",params);
        log.info("timeInStatusesSQL = {} ",jiraIssuesSQL);
        log.info("params = {} ",params);
        return "WITH"+" issue_time_in_statuses as ("+timeInStatusesSQL+"),\n"+
                "assignee_time_in_statuses as ("+jiraIssuesSQL+")\n";
    }

    private String generateTimeInStatusesSQL(String company,List<String> integrationIds, List<String> developmentStages,String paramPrefix,Map<String,Object> params) {
        String statusCondition = CollectionUtils.isNotEmpty(developmentStages) ? " AND status IN (:"+paramPrefix+"historical_assignees_statuses)":"";
        return "select issue_key,sum(end_time-start_time) as time_in_statuses from "+company+".jira_issue_statuses\n"+
        "where integration_id IN ("+paramPrefix+":jira_integration_ids)" +statusCondition+"\n"+
        "group by issue_key";
    }

    private String generateJiraIssuesSQL(String company, JiraIssuesFilter filter, @Nullable OUConfiguration ouConfig, String paramPrefix, Map<String, Object> params, long currentTime, String ticketCategorySql,
                                         boolean needsHistoricalAssignees, @Nullable List<String> historicalAssigneeStatuses){
        List<String> originalAssigneesFilter = filter.getAssignees();
        if (needsHistoricalAssignees) {
            // - remove assignees filter
            filter = filter.toBuilder().assignees(null).build();
            // - disable OU assignees
            if (ouConfig != null) {
                ouConfig = ouConfig.toBuilder()
                        .ouExclusions(ListUtils.addIfNotPresent(ouConfig.getOuExclusions(), "assignees"))
                        .build();
            }
        }

        Map<String, List<String>> conditions = jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params, paramPrefix, filter, currentTime, filter.getIngestedAt(), "issues.", ouConfig);

        String historicalAssigneesTableJoin = "";
        if (needsHistoricalAssignees) {
            historicalAssigneesTableJoin = generateHistoricalAssigneesJoin(company, ouConfig, paramPrefix, params, historicalAssigneeStatuses, originalAssigneesFilter);
        }

        String issuesWhere = baJiraAggsQueryBuilder.generateWhereFromCondition(conditions, ISSUES_TABLE);
        String finalTableWhere = baJiraAggsQueryBuilder.generateWhereFromCondition(conditions, FINAL_TABLE);
        String select = StringJoiner.dedupeAndJoin(", ", "issues.key as issue_key", " CASE WHEN (issues.story_points IS NULL OR issues.story_points < 0) THEN 0 ELSE issues.story_points END AS story_points ",(needsHistoricalAssignees ? "historical_assignee" : null)
                ,"assignee_start_time", "assignee_end_time", "issue_status", "status_start_time", "status_end_time", ticketCategorySql);
        String finalTableGroupBy = " group by issue_key,historical_assignee,story_points";
        String finalTableSelect = " issue_key,historical_assignee as assignee,story_points,sum(least(assignee_end_time,status_end_time)-greatest(assignee_start_time,status_start_time)) as assignee_time";
        return "    select " + finalTableSelect +" from (\n" +
                "      select " + select + "\n" +
                "      from "+company+".jira_issues as issues \n" +
                historicalAssigneesTableJoin +
                "      " + issuesWhere + "\n" +
                "    ) as final_table " + finalTableWhere + finalTableGroupBy + "\n";
    }

    private String generateHistoricalAssigneesJoin(String company, OUConfiguration ouConfig, String paramPrefix, Map<String, Object> params, @Nullable List<String> historicalAssigneesStatuses, List<String> originalAssigneesFilter) {
        List<String> conditions = new ArrayList<>();
        // if we need to filter assignees by status, we need to join on the statuses table
        String statusJoin = statusJoin = "" +
                "          join "+company+".jira_issue_statuses as s\n" +
                "          on \n" +
                "            a.integration_id = s.integration_id and\n" +
                "            a.issue_key = s.issue_key and\n" +
                "            ((s.start_time >= a.start_time  and s.start_time <= a.end_time) OR\n" +
                "             (s.end_time >= a.start_time and s.end_time <= a.end_time ))\n";
        if (CollectionUtils.isNotEmpty(historicalAssigneesStatuses)) {
            String statusListKey = paramPrefix + "historical_assignees_statuses";
            params.put(statusListKey, historicalAssigneesStatuses);
            conditions.add("s.status in (:" + statusListKey + ")");
        }
        // recreate assignee filter logic
        //baJiraAggsQueryBuilder.generateHistoricalAssigneesFilter(company, ouConfig, paramPrefix, params, originalAssigneesFilter).ifPresent(conditions::add);

        //PROP-1686 : exclude assignees such as "UNASSIGNED","_UNASSIGNED"
        conditions.add("a.assignee not in ('UNASSIGNED','_UNASSIGNED_','unassigned','_unassigned_')");

        String where = conditions.isEmpty() ? "" : "          where " + String.join(" and ", conditions) + "\n";
        return "" +
                "        join (\n" +
                "          select a.integration_id, a.issue_key, assignee as historical_assignee, a.start_time as assignee_start_time, a.end_time as assignee_end_time, s.status as issue_status, s.start_time as status_start_time, s.end_time as status_end_time\n" +
                "          from "+company+".jira_issue_assignees as a\n" +
                statusJoin +
                where +
                "          group by a.integration_id, a.issue_key, assignee, a.start_time, a.end_time,s.status,s.start_time, s.end_time\n" +
                "        ) as historical_assignees\n" +
                "        on issues.integration_id = historical_assignees.integration_id and\n" +
                "           issues.key = historical_assignees.issue_key";
    }

    private String generateOptimizedJiraIssuesSQL(String company, JiraIssuesFilter filter, @Nullable OUConfiguration ouConfig, String paramPrefix, Map<String, Object> params, long currentTime, String ticketCategorySql,
                                                  boolean needsHistoricalAssignees, @Nullable List<String> historicalAssigneeStatuses, List<String> conditions){
        List<String> originalAssigneesFilter = filter.getAssignees();
        if (needsHistoricalAssignees) {
            // - remove assignees filter
            filter = filter.toBuilder().assignees(null).build();
            // - disable OU assignees
            if (ouConfig != null) {
                ouConfig = ouConfig.toBuilder()
                        .ouExclusions(ListUtils.addIfNotPresent(ouConfig.getOuExclusions(), "assignees"))
                        .build();
            }
        }

        String historicalAssigneesTableJoin = generateOptimizedHistoricalAssigneesJoin(company, ouConfig, paramPrefix, params, historicalAssigneeStatuses, originalAssigneesFilter, ticketCategorySql);

        String finalTableGroupBy = " group by issue_key,historical_assignee,story_points";
        String finalTableSelect = " issue_key,historical_assignee as assignee,story_points,sum(least(assignee_end_time,status_end_time)-greatest(assignee_start_time,status_start_time)) as assignee_time";
        String whereCondition = conditions.isEmpty() ? "" : "  where " + String.join(" and ", conditions) + "\n";
        return "select "+finalTableSelect+" from ( "+
                historicalAssigneesTableJoin
                +whereCondition
                +finalTableGroupBy
                +") ";
    }
    private String generateOptimizedHistoricalAssigneesJoin(String company, OUConfiguration ouConfig, String paramPrefix, Map<String, Object> params, @Nullable List<String> historicalAssigneesStatuses, List<String> originalAssigneesFilter, String ticketCategorySql) {
        List<String> conditions = new ArrayList<>();

        String issuesJoin = " join issues on issues.key = a.issue_key and issues.integration_id = a.integration_id ";
        String statusJoin = statusJoin = "" +
                "          join "+company+".jira_issue_statuses as s\n" +
                "          on \n" +
                "            a.integration_id = s.integration_id and\n" +
                "            a.issue_key = s.issue_key and\n" +
                "            ((s.start_time >= a.start_time  and s.start_time <= a.end_time) OR\n" +
                "             (s.end_time >= a.start_time and s.end_time <= a.end_time ))\n";
        if (CollectionUtils.isNotEmpty(historicalAssigneesStatuses)) {
            String statusListKey = paramPrefix + "historical_assignees_statuses";
            params.put(statusListKey, historicalAssigneesStatuses);
            conditions.add("s.status in (:" + statusListKey + ")");
        }

        conditions.add("a.assignee not in ('UNASSIGNED','_UNASSIGNED_','unassigned','_unassigned_')");

        String storyPointSelect = "CASE WHEN ( issues.story_points IS NULL OR issues.story_points < 0 )\n" +
                " THEN 0 ELSE issues.story_points END AS story_points,";

        ticketCategorySql = StringUtils.isEmpty(ticketCategorySql) ? ticketCategorySql : ", "+ticketCategorySql;
        String groupByticketCategory = StringUtils.isEmpty(ticketCategorySql) ? "" : ", ticket_category";

        String where = conditions.isEmpty() ? "" : "  where " + String.join(" and ", conditions) + "\n";
        return " select a.integration_id, a.issue_key, a.assignee as historical_assignee,"+storyPointSelect+" a.start_time as assignee_start_time, a.end_time as assignee_end_time, s.status as issue_status, s.start_time as status_start_time, s.end_time as status_end_time\n" +
                ticketCategorySql +
                " from "+company+".jira_issue_assignees as a\n" +
                issuesJoin +
                statusJoin +
                where +
                "          group by a.integration_id, a.issue_key, issues.story_points, a.assignee, a.start_time, a.end_time,s.status,s.start_time, s.end_time\n" +groupByticketCategory+
                "        ) as final_table\n";
    }


    public static Optional<String> generateWithIssuesFilter(Map<String, Object> params, String paramPrefix,  List<String> conditions, List<String> originalAssigneesFilter) {

        if (CollectionUtils.isNotEmpty(originalAssigneesFilter)) {

            List<String> integrationUsersConditions = new ArrayList<>();
            integrationUsersConditions.add("id::text in (:" + paramPrefix + "jira_assignees)");
            params.put(paramPrefix + "jira_assignees", originalAssigneesFilter);
            if (params.containsKey(paramPrefix + "jira_integration_ids")) {
                integrationUsersConditions.add("integration_id in (:" + paramPrefix + "jira_integration_ids)");
            }
            String assigneeCondition = "";
            if(!integrationUsersConditions.isEmpty()){
                assigneeCondition = "a.assignee IN (select display_name from ${company}.integration_users " +
                        "where " + String.join(" and ", integrationUsersConditions) + ")";
            }
            conditions.add(assigneeCondition);
            return Optional.of(" and " + String.join(" and ", conditions));
        }else if(CollectionUtils.isNotEmpty(conditions)){
            return Optional.of("and " + String.join(" and ", conditions) + ")");
        }
        return Optional.empty();
    }
}
