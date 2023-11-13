package io.levelops.commons.databases.services.business_alignment;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.StringJoiner;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.ScmAggService.COMMITS_TABLE;
import static io.levelops.commons.databases.services.business_alignment.BaJiraAggsQueryBuilder.FTE_ROW_MAPPER_BUILDER;
import static io.levelops.commons.databases.services.business_alignment.BaJiraAggsQueryBuilder.TICKET_CAT_FTE_ROW_MAPPER;
import static io.levelops.commons.databases.services.business_alignment.BaJiraAggsQueryBuilder.generateWhereFromCondition;

@Log4j2
@Service
public class BaJiraScmAggsQueryBuilder {

    private final BaJiraAggsQueryBuilder jiraAggsQueryBuilder;
    private final ScmAggService scmAggService;

    public BaJiraScmAggsQueryBuilder(BaJiraAggsQueryBuilder jiraAggsQueryBuilder, ScmAggService scmAggService) {
        this.jiraAggsQueryBuilder = jiraAggsQueryBuilder;
        this.scmAggService = scmAggService;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    public enum ScmAcross {
        AUTHOR,
        TICKET_CATEGORY,
        COMMITTED_AT;

        String acrossColumnName;

        public String getAcrossColumnName() {
            return StringUtils.defaultIfEmpty(acrossColumnName, toString());
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        @Nullable
        public static ScmAcross fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(ScmAcross.class, value);
        }
    }

    public BaJiraAggsQueryBuilder.Query buildScmCommitCountFTEQuery(String company,
                                                                    ScmCommitFilter scmCommitFilter,
                                                                    JiraIssuesFilter jiraIssuesFilter,
                                                                    OUConfiguration ouConfig, TicketCategorizationScheme ticketCategorizationScheme,
                                                                    ScmAcross across,
                                                                    Integer page,
                                                                    Integer pageSize) {
        long currentTime = Instant.now().getEpochSecond();
        String aggInterval = MoreObjects.firstNonNull(scmCommitFilter.getAggInterval(), AGG_INTERVAL.day).toString();
        Map<String, Object> params = new HashMap<>();

        String sql;
        RowMapper<DbAggregationResult> rowMapper;
        switch (across) {

            case COMMITTED_AT: {
                String acrossName = across.toString();
                String columnName = across.getAcrossColumnName();
                AggTimeQueryHelper.AggTimeQuery aggTimeQuery = AggTimeQueryHelper.getAggTimeQuery(AggTimeQueryHelper.Options.builder()
                        .columnName(columnName)
                        .across(acrossName)
                        .interval(aggInterval)
                        .isBigInt(false)
                        .prefixWithComma(false)
                        .build());
                String fteSql = generateCommitFteSql(company, scmCommitFilter, jiraIssuesFilter, ouConfig, ticketCategorizationScheme, params, currentTime, aggTimeQuery.getHelperColumn(), aggTimeQuery.getGroupBy());
                sql = "" +
                        "select " + aggTimeQuery.getSelect() + ", fte, effort, total_effort from (\n" +
                        "  select " + aggTimeQuery.getGroupBy() + ", sum(fte) as fte, sum(effort_for_cat) as effort, sum(total_effort) as total_effort from ( \n" +
                        fteSql + "\n" +
                        "  ) as interval_table \n" +
                        "  group by " + aggTimeQuery.getGroupBy() + "\n" +
                        "  order by " + aggTimeQuery.getOrderBy() + "\n" +
                        ") as t";
                rowMapper = (rs, rowNumber) -> DbAggregationResult.builder()
                        .key(String.valueOf(rs.getInt(acrossName)))
                        .additionalKey(rs.getString("interval"))
                        .fte(rs.getFloat("fte"))
                        .effort(rs.getLong("effort"))
                        .total(rs.getLong("total_effort"))
                        .build();
                break;
            }
            case AUTHOR: {
                String fteSql = generateCommitFteSql(company, scmCommitFilter, jiraIssuesFilter, ouConfig, ticketCategorizationScheme, params, currentTime, "author", "author");
                sql = "select author, author_id, sum(fte) as fte, sum(effort_for_cat) as effort, sum(total_effort) as total_effort from ( \n" +
                        fteSql + "\n" +
                        ") as t \n" +
                        "group by author, author_id\n" +
                        "order by fte desc";
                rowMapper = FTE_ROW_MAPPER_BUILDER.apply("author");
                break;
            }
            case TICKET_CATEGORY:
            default: {
                String fteSql = generateCommitFteSql(company, scmCommitFilter, jiraIssuesFilter, ouConfig, ticketCategorizationScheme, params, currentTime, null, null);
                sql = "select ticket_category, sum(fte) as fte, sum(effort_for_cat) as effort, sum(total_effort) as total_effort from ( \n" +
                        fteSql + "\n" +
                        ") as t \n" +
                        "group by ticket_category\n" +
                        "order by fte desc";
                rowMapper = TICKET_CAT_FTE_ROW_MAPPER;
                break;
            }
        }

        String limit = "";
        String offset = "";

        if (scmCommitFilter.getAcrossLimit() != null && scmCommitFilter.getAcrossLimit() != DefaultListRequest.DEFAULT_ACROSS_LIMIT) {
            limit = "\nlimit " + scmCommitFilter.getAcrossLimit() + "\n";
        } else {
            Integer skip = page * pageSize;
            limit = "\nlimit " + pageSize + "\n";
            offset = "\noffset " + skip + "\n";
        }

        String countSql = "SELECT COUNT(*) FROM ( " + sql + " ) i";
        sql += limit + offset;

        return BaJiraAggsQueryBuilder.Query.builder()
                .sql(sql)
                .params(params)
                .rowMapper(rowMapper)
                .countSql(countSql)
                .build();
    }


    /**
     * @param jiraIssuesFilter only used to for ticket categorization (ingested_at, integration_ids + all ticket categorization fields)
     * @return
     */
    private String generateCommitFteSql(String company, ScmCommitFilter scmCommitFilter, JiraIssuesFilter jiraIssuesFilter, OUConfiguration ouConfig, TicketCategorizationScheme ticketCategorizationScheme, Map<String, Object> params, long currentTime, String helperColumn, String groupByColumn) {
        // -- conditions for basis
        String scmBasisSql = "select * from ${company}.scm_commits"; // TODO

        // -- condition for filtered set
        String scmSql = generateScmCommitsWithCategoriesSql(company, scmCommitFilter, jiraIssuesFilter, ouConfig, params, currentTime);
        ArrayList<String> finalConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(jiraIssuesFilter.getTicketCategories())) {
            finalConditions.add("ticket_category IN (:final_ticket_categories)");
            params.put("final_ticket_categories", jiraIssuesFilter.getTicketCategories());
        }
        String finalWhere = StringJoiner.prefixIfNotBlank("WHERE ", String.join(" AND ", finalConditions));

        // -- select
        String effortCalculation = "count(*) as effort";
        String categoriesManyToOne = jiraAggsQueryBuilder.generateTicketCategoryManyToOneSql(company, ticketCategorizationScheme, "categories");
        String select = StringJoiner.dedupeAndJoin(", ", "author", "author_id", categoriesManyToOne + " as ticket_category", effortCalculation, helperColumn);
        String selectBasis = StringJoiner.dedupeAndJoin(", ", "author", "author_id", effortCalculation, helperColumn);

        // -- group by
        String groupBy = StringJoiner.dedupeAndJoin(", ", "author", "author_id", "ticket_category", groupByColumn);
        String groupByBasis = StringJoiner.dedupeAndJoin(", ", "author", "author_id", groupByColumn);

        // -- join
        String joinCondition = StringJoiner.dedupeStream(Stream.of("author", groupByColumn))
                .map(str -> String.format("cat.%1$s = basis.%1$s", str))
                .collect(Collectors.joining(" and "));

        return "\n" +
                "  select cat.*, cat.effort as effort_for_cat, basis.effort as total_effort,\n" +
                "    coalesce(cat.effort::float / nullif(basis.effort, 0), 0)::numeric as fte \n" + // use nullif + coalesce to return 0 when dividing by 0
                "  from ( \n" +
                "    select " + select + "\n" +
                "    from (\n" + scmSql + ") as filtered_set\n" +
                "    group by " + groupBy + "\n" +
                "  ) as cat \n" +
                "  join ( \n" +
                "    select " + selectBasis + "\n" +
                "    from (\n" + scmBasisSql + ") as basis\n" +
                "    group by " + groupByBasis + "\n" +
                "  ) as basis \n" +
                "  on " + joinCondition + "\n" +
                "  " + finalWhere;
    }

    public String generateScmCommitsWithCategoriesSql(String company, ScmCommitFilter scmCommitFilter, JiraIssuesFilter jiraIssuesFilter, OUConfiguration ouConfig, Map<String, Object> params, long currentTime) {
        String ticketCategorySql = jiraAggsQueryBuilder.generateTicketCategorySql(company, jiraIssuesFilter, params, currentTime);
        JiraIssuesFilter filter = JiraIssuesFilter.builder()
                .ingestedAt(jiraIssuesFilter.getIngestedAt())
                .ingestedAtByIntegrationId(jiraIssuesFilter.getIngestedAtByIntegrationId())
                .isActive(true)
                .integrationIds(jiraIssuesFilter.getIntegrationIds())
                .build();
        String jiraIssuesSql = jiraAggsQueryBuilder.generateJiraIssuesSql(company, filter, null /* TODO do we want all tickets regardless of OU or make it smarter? */, "jira", params, currentTime, ticketCategorySql, false, null);

        String columns = String.join(", ", "cm.integration_id", "cm.commit_sha", "cm.author", "cm.author_id", "cm.committed_at");

        return "" +
                "      select " + columns + ",\n" +
                "        array_agg(distinct coalesce(jira.ticket_category, 'Other')) as categories\n" +
                "      from (" + generateScmCommitsSql(company, scmCommitFilter, ouConfig, params) + ") as cm\n" +
                "      left join ${company}.scm_commit_jira_mappings as cjm\n" +
                "      on cm.commit_sha = cjm.commit_sha\n" +
                "      left join (\n" +
                "      " + jiraIssuesSql +
                "      ) as jira\n" +
                "      on jira.key = cjm.issue_key\n" +
                "      group by " + columns + "\n";
    }

    private String generateScmCommitsSql(String company, ScmCommitFilter scmCommitFilter, OUConfiguration ouConfig, Map<String, Object> params) {
        Map<String, List<String>> conditions = scmAggService.createCommitsWhereClauseAndUpdateParams(company, params, scmCommitFilter, "scm_commits_", "", false, ouConfig);
        String commitsWhere = generateWhereFromCondition(conditions, COMMITS_TABLE);

        String commitAuthorTableJoin = ScmQueryUtils.sqlForAuthorTableJoin(company);
        String commitCommitterTableJoin = ScmQueryUtils.sqlForCommitterTableJoin(company);

        String columns = ScmQueryUtils.COMMITS_SELECT;
        return "select * from (select " + columns + " from ${company}.scm_commits " +
                commitAuthorTableJoin + commitCommitterTableJoin +
                ") as scm_commits" + commitsWhere;
    }

}
