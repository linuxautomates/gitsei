package io.levelops.commons.databases.services.business_alignment;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.filters.*;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.ScmAggService.COMMITS_TABLE;
import static io.levelops.commons.databases.services.business_alignment.BaWorkItemsAggsQueryBuilder.*;

@Log4j2
@Service
public class BaWorkItemsScmAggsQueryBuilder {

    private final BaWorkItemsAggsQueryBuilder workItemsAggsQueryBuilder;
    private final ScmAggService scmAggService;

    public BaWorkItemsScmAggsQueryBuilder(BaWorkItemsAggsQueryBuilder workItemsAggsQueryBuilder,
                                          ScmAggService scmAggService) {
        this.workItemsAggsQueryBuilder = workItemsAggsQueryBuilder;
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
        public static BaWorkItemsScmAggsQueryBuilder.ScmAcross fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(BaWorkItemsScmAggsQueryBuilder.ScmAcross.class, value);
        }
    }

    public BaWorkItemsAggsQueryBuilder.WorkItemsAggsQuery buildScmCommitCountFTEQuery(String company,
                                                                                      ScmCommitFilter scmCommitFilter,
                                                                                      WorkItemsFilter workItemsFilter,
                                                                                      WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                                      OUConfiguration ouConfig,
                                                                                      TicketCategorizationScheme ticketCategorizationScheme,
                                                                                      BaWorkItemsScmAggsQueryBuilder.ScmAcross across) {
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
                String fteSql = generateCommitFteSql(company, scmCommitFilter, workItemsFilter, workItemsMilestoneFilter, ouConfig,
                        ticketCategorizationScheme, params, aggTimeQuery.getHelperColumn(), aggTimeQuery.getGroupBy());
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
                String fteSql = generateCommitFteSql(company, scmCommitFilter, workItemsFilter, workItemsMilestoneFilter, ouConfig,
                        ticketCategorizationScheme, params, "author", "author");
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
                String fteSql = generateCommitFteSql(company, scmCommitFilter, workItemsFilter, workItemsMilestoneFilter, ouConfig,
                        ticketCategorizationScheme, params, null, null);
                sql = "select ticket_category, sum(fte) as fte, sum(effort_for_cat) as effort, sum(total_effort) as total_effort from ( \n" +
                        fteSql + "\n" +
                        ") as t \n" +
                        "group by ticket_category\n" +
                        "order by fte desc";
                rowMapper = TICKET_CAT_FTE_ROW_MAPPER;
                break;
            }
        }

        String limit = scmCommitFilter.getAcrossLimit() != null ? "\nlimit " + scmCommitFilter.getAcrossLimit() + "\n" : "";
        sql += limit;

        return BaWorkItemsAggsQueryBuilder.WorkItemsAggsQuery.builder()
                .sql(sql)
                .params(params)
                .rowMapper(rowMapper)
                .build();
    }


    /**
     * @param workItemsFilter only used to for ticket categorization (ingested_at, integration_ids + all ticket categorization fields)
     * @return
     */
    private String generateCommitFteSql(String company,
                                        ScmCommitFilter scmCommitFilter,
                                        WorkItemsFilter workItemsFilter,
                                        WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                        OUConfiguration ouConfig,
                                        TicketCategorizationScheme ticketCategorizationScheme,
                                        Map<String, Object> params,
                                        String helperColumn,
                                        String groupByColumn) {
        // -- conditions for basis
        String scmBasisSql = "select * from ${company}.scm_commits";

        // -- condition for filtered set
        String scmSql = generateScmCommitsWithCategoriesSql(company, scmCommitFilter, workItemsFilter, workItemsMilestoneFilter, ouConfig, params);
        ArrayList<String> finalConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(workItemsFilter.getTicketCategories())) {
            finalConditions.add("ticket_category IN (:final_ticket_categories)");
            params.put("final_ticket_categories", workItemsFilter.getTicketCategories());
        }
        String finalWhere = StringJoiner.prefixIfNotBlank("WHERE ", String.join(" AND ", finalConditions));

        // -- select
        String effortCalculation = "count(*) as effort";
        String categoriesManyToOne = workItemsAggsQueryBuilder.generateTicketCategoryManyToOneSql(company, ticketCategorizationScheme, "categories");
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

    public String generateScmCommitsWithCategoriesSql(String company,
                                                      ScmCommitFilter scmCommitFilter,
                                                      WorkItemsFilter workItemsFilter,
                                                      WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                      OUConfiguration ouConfig,
                                                      Map<String, Object> params) {
        String workItemsSql = workItemsAggsQueryBuilder.generateWorkItemsSql(company, workItemsFilter, workItemsMilestoneFilter, params, "workitem", ouConfig, false, null);

        String columns = String.join(", ", "cm.integration_id", "cm.commit_sha", "cm.author", "cm.author_id", "cm.committed_at");

        return "" +
                "      select " + columns + ",\n" +
                "        array_agg(distinct coalesce(workitem.ticket_category, 'Unplanned')) as categories\n" +
                "      from (" + generateScmCommitsSql(company, scmCommitFilter, ouConfig, params) + ") as cm\n" +
                "      left join ${company}.scm_commit_workitem_mappings as cwm\n" +
                "      on cm.commit_sha = cwm.commit_sha\n" +
                "      left join (\n" +
                "      " + workItemsSql +
                "      ) as workitem\n" +
                "      on workitem.workitem_id = cwm.workitem_id\n" +
                "      group by " + columns + "\n";
    }

    private String generateScmCommitsSql(String company,
                                         ScmCommitFilter scmCommitFilter,
                                         OUConfiguration ouConfig,
                                         Map<String, Object> params) {
        Map<String, List<String>> conditions = scmAggService.createCommitsWhereClauseAndUpdateParams(company, params,
                scmCommitFilter, "scm_commits_", "", false, ouConfig);
        String commitsWhere = generateWhereFromCondition(conditions, COMMITS_TABLE);

        String commitAuthorTableJoin = ScmQueryUtils.sqlForAuthorTableJoin(company);
        String commitCommitterTableJoin = ScmQueryUtils.sqlForCommitterTableJoin(company);

        String columns = ScmQueryUtils.COMMITS_SELECT;
        return "select * from (select " + columns + " from ${company}.scm_commits " +
                commitAuthorTableJoin + commitCommitterTableJoin +
                ") as scm_commits" + commitsWhere;
    }

    public static String generateWhereFromCondition(Map<String, List<String>> conditions, String table) {
        return conditions.get(table).isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions.get(table));
    }
}
