package io.levelops.commons.databases.services.business_alignment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.exceptions.NotFoundException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class BaWorkItemsAggsDatabaseService {

    private final NamedParameterJdbcTemplate template;
    private final BaWorkItemsAggsQueryBuilder baWorkItemsAggsQueryBuilder;
    private final BaWorkItemsAggsActiveWorkQueryBuilder baWorkItemsAggsActiveWorkQueryBuilder;
    private final BaWorkItemsScmAggsQueryBuilder baWorkItemsScmAggsQueryBuilder;
    private final TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;

    @Autowired
    public BaWorkItemsAggsDatabaseService(DataSource dataSource,
                                          BaWorkItemsAggsQueryBuilder baWorkItemsAggsQueryBuilder,
                                          BaWorkItemsAggsActiveWorkQueryBuilder baWorkItemsAggsActiveWorkQueryBuilder,
                                          BaWorkItemsScmAggsQueryBuilder baWorkItemsScmAggsQueryBuilder,
                                          TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService) {
        template = new NamedParameterJdbcTemplate(dataSource);
        this.baWorkItemsAggsQueryBuilder = baWorkItemsAggsQueryBuilder;
        this.baWorkItemsAggsActiveWorkQueryBuilder = baWorkItemsAggsActiveWorkQueryBuilder;
        this.baWorkItemsScmAggsQueryBuilder = baWorkItemsScmAggsQueryBuilder;
        this.ticketCategorizationSchemeDatabaseService = ticketCategorizationSchemeDatabaseService;
    }

    // region workitems fte

    /**
     * This is for BA specific filters and special options.
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BaWorkItemsOptions.BaWorkItemsOptionsBuilder.class)
    public static class BaWorkItemsOptions {
        // to customize "completed work" statuses (defaults to BaWorkItemsAggsQueryBuilder.DONE_STATUS_CATEGORY)
        List<String> completedWorkStatuses;
        List<String> completedWorkStatusCategories;

        // to customize "in progress" statuses (used by ticket time spent metric)
        List<String> inProgressStatuses;
        List<String> inProgressStatusCategories;

        // attribution mode
        AttributionMode attributionMode;
        List<String> historicalAssigneesStatuses;

        public AttributionMode getAttributionMode() {
            return MoreObjects.firstNonNull(attributionMode, AttributionMode.CURRENT_ASSIGNEE);
        }

        public enum AttributionMode {
            CURRENT_ASSIGNEE, // default
            CURRENT_AND_PREVIOUS_ASSIGNEES;

            @JsonCreator
            @Nullable
            public static AttributionMode fromString(@Nullable String value) {
                return EnumUtils.getEnumIgnoreCase(AttributionMode.class, value);
            }

            @JsonValue
            @Override
            public String toString() {
                return super.toString().toLowerCase();
            }
        }

        public String generateCacheHashRawString() {
            StringBuilder dataToHash = new StringBuilder();
            CacheHashUtils.hashData(dataToHash, "completedWorkStatuses", completedWorkStatuses);
            CacheHashUtils.hashData(dataToHash, "completedWorkStatusCategories", completedWorkStatusCategories);
            CacheHashUtils.hashData(dataToHash, "inProgressStatuses", inProgressStatuses);
            CacheHashUtils.hashData(dataToHash, "inProgressStatusCategories", inProgressStatusCategories);
            CacheHashUtils.hashData(dataToHash, "attributionMode", attributionMode);
            CacheHashUtils.hashData(dataToHash, "historicalAssigneesStatuses", historicalAssigneesStatuses);
            return dataToHash.toString();
        }

        public String generateCacheHash() {
            return CacheHashUtils.generateCacheHash(generateCacheHashRawString());
        }
    }

    public DbListResponse<DbAggregationResult> calculateTicketCountFTE(String company,
                                                                       BaWorkItemsAggsQueryBuilder.WorkItemsAcross across,
                                                                       WorkItemsFilter filter,
                                                                       WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                       BaWorkItemsOptions baWorkItemsOptions,
                                                                       OUConfiguration ouConfig) {
        return doCalculateWorkItemFTE(company, across, filter, workItemsMilestoneFilter,
                baWorkItemsOptions, BaWorkItemsAggsQueryBuilder.Calculation.TICKET_COUNT, ouConfig);
    }

    public DbListResponse<DbAggregationResult> calculateStoryPointsFTE(String company,
                                                                       BaWorkItemsAggsQueryBuilder.WorkItemsAcross across,
                                                                       WorkItemsFilter filter,
                                                                       WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                       BaWorkItemsOptions baWorkItemsOptions,
                                                                       OUConfiguration ouConfig) {
        return doCalculateWorkItemFTE(company, across, filter, workItemsMilestoneFilter,
                baWorkItemsOptions, BaWorkItemsAggsQueryBuilder.Calculation.STORY_POINTS, ouConfig);
    }

    public DbListResponse<DbAggregationResult> calculateTicketTimeSpentFTE(String company,
                                                                           BaWorkItemsAggsQueryBuilder.WorkItemsAcross across,
                                                                           WorkItemsFilter filter,
                                                                           WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                           BaWorkItemsOptions baWorkItemsOptions,
                                                                           OUConfiguration ouConfig) {
        return doCalculateWorkItemFTE(company, across, filter, workItemsMilestoneFilter,
                baWorkItemsOptions, BaWorkItemsAggsQueryBuilder.Calculation.TICKET_TIME_SPENT, ouConfig);
    }

    protected DbListResponse<DbAggregationResult> doCalculateWorkItemFTE(String company,
                                                                         BaWorkItemsAggsQueryBuilder.WorkItemsAcross across,
                                                                         WorkItemsFilter filter,
                                                                         WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                         @Nullable BaWorkItemsOptions baWorkItemsOptions,
                                                                         BaWorkItemsAggsQueryBuilder.Calculation calculation,
                                                                         OUConfiguration ouConfig) {
        baWorkItemsOptions = (baWorkItemsOptions != null) ? baWorkItemsOptions : BaWorkItemsOptions.builder().build();
        BaWorkItemsAggsQueryBuilder.WorkItemsAggsQuery query = baWorkItemsAggsQueryBuilder.buildWorkItemFTEQuery(company,
                filter, workItemsMilestoneFilter, ouConfig, across, baWorkItemsOptions, calculation);

        String sql = StringSubstitutor.replace(query.getSql(), Map.of("company", company));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", query.getParams());
        List<DbAggregationResult> result = template.query(sql, query.getParams(), query.getRowMapper());
        return DbListResponse.of(result, result.size());
    }
    // endregion

    // region workitems scm commits
    public DbListResponse<DbAggregationResult> calculateScmCommitCountFTE(String company,
                                                                          BaWorkItemsScmAggsQueryBuilder.ScmAcross across,
                                                                          ScmCommitFilter scmCommitFilter,
                                                                          WorkItemsFilter workItemsFilter,
                                                                          WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                          OUConfiguration ouConfig) throws NotFoundException {
        TicketCategorizationScheme scheme = retrieveTicketCategorizationScheme(company, workItemsFilter);
        BaWorkItemsAggsQueryBuilder.WorkItemsAggsQuery query = baWorkItemsScmAggsQueryBuilder.buildScmCommitCountFTEQuery(company,
                scmCommitFilter, workItemsFilter, workItemsMilestoneFilter, ouConfig, scheme, across);
        String sql = StringSubstitutor.replace(query.getSql(), Map.of("company", company));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", query.getParams());
        List<DbAggregationResult> result = template.query(sql, query.getParams(), query.getRowMapper());
        return DbListResponse.of(result, result.size());
    }
    // endregion

    // region active work

    public DbListResponse<DbAggregationResult> calculateTicketCountActiveWork(String company,
                                                                              BaWorkItemsAggsQueryBuilder.WorkItemsAcross across,
                                                                              WorkItemsFilter filter,
                                                                              WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                              OUConfiguration ouConfig) throws NotFoundException {
        return doCalculateActiveWork(company, across, filter, workItemsMilestoneFilter, BaWorkItemsAggsQueryBuilder.Calculation.TICKET_COUNT, ouConfig);
    }

    public DbListResponse<DbAggregationResult> calculateStoryPointsActiveWork(String company,
                                                                              BaWorkItemsAggsQueryBuilder.WorkItemsAcross across,
                                                                              WorkItemsFilter filter,
                                                                              WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                              OUConfiguration ouConfig) throws NotFoundException {
        return doCalculateActiveWork(company, across, filter, workItemsMilestoneFilter, BaWorkItemsAggsQueryBuilder.Calculation.STORY_POINTS, ouConfig);
    }

    protected DbListResponse<DbAggregationResult> doCalculateActiveWork(String company,
                                                                        BaWorkItemsAggsQueryBuilder.WorkItemsAcross across,
                                                                        WorkItemsFilter filter,
                                                                        WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                        BaWorkItemsAggsQueryBuilder.Calculation calculation,
                                                                        OUConfiguration ouConfig) throws NotFoundException {
        TicketCategorizationScheme scheme = retrieveTicketCategorizationScheme(company, filter);

        BaWorkItemsAggsQueryBuilder.WorkItemsAggsQuery query = baWorkItemsAggsActiveWorkQueryBuilder
                .buildActiveWorkSql(company, filter, workItemsMilestoneFilter, ouConfig, across, scheme, calculation);

        String sql = StringSubstitutor.replace(query.getSql(), Map.of("company", company));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", query.getParams());
        List<DbAggregationResult> result = template.query(sql, query.getParams(), query.getRowMapper());
        return DbListResponse.of(result, result.size());
    }

    // endregion

    private TicketCategorizationScheme retrieveTicketCategorizationScheme(String company, WorkItemsFilter filter) throws NotFoundException {
        String ticketCategorizationSchemeId = filter.getTicketCategorizationSchemeId();
        Validate.notBlank(ticketCategorizationSchemeId, "ticketCategorizationSchemeId cannot be null or empty.");
        return ticketCategorizationSchemeDatabaseService.get(company, ticketCategorizationSchemeId)
                .orElseThrow(() -> new NotFoundException("Could not find ticket categorization scheme with id=" + ticketCategorizationSchemeId));
    }
}
