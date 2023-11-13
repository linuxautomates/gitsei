package io.levelops.commons.databases.services.business_alignment;

import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.business_alignment.BaJiraAggsQueryBuilder.Query;
import io.levelops.commons.databases.services.business_alignment.BaJiraScmAggsQueryBuilder.ScmAcross;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.services.business_alignment.models.BaJiraOptions;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
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
public class BaJiraAggsDatabaseService {

    private final NamedParameterJdbcTemplate template;
    private final BaJiraAggsQueryBuilder baJiraAggsQueryBuilder;
    private final BaJiraAggsActiveWorkQueryBuilder baJiraAggsActiveWorkQueryBuilder;
    private final BaJiraScmAggsQueryBuilder baJiraScmAggsQueryBuilder;
    private final TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;

    @Autowired
    public BaJiraAggsDatabaseService(DataSource dataSource,
                                     BaJiraAggsQueryBuilder baJiraAggsQueryBuilder,
                                     BaJiraAggsActiveWorkQueryBuilder baJiraAggsActiveWorkQueryBuilder,
                                     BaJiraScmAggsQueryBuilder baJiraScmAggsQueryBuilder,
                                     TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService) {
        template = new NamedParameterJdbcTemplate(dataSource);
        this.baJiraAggsQueryBuilder = baJiraAggsQueryBuilder;
        this.baJiraAggsActiveWorkQueryBuilder = baJiraAggsActiveWorkQueryBuilder;
        this.baJiraScmAggsQueryBuilder = baJiraScmAggsQueryBuilder;
        this.ticketCategorizationSchemeDatabaseService = ticketCategorizationSchemeDatabaseService;
    }

    // region issue fte


    public DbListResponse<DbAggregationResult> calculateTicketCountFTE(String company, JiraAcross across, JiraIssuesFilter filter, BaJiraOptions baJiraOptions, OUConfiguration ouConfig) throws BadRequestException {
        return calculateTicketCountFTE(company, across, filter, baJiraOptions, ouConfig, 0, Integer.MAX_VALUE);
    }

    public DbListResponse<DbAggregationResult> calculateTicketCountFTE(String company, JiraAcross across, JiraIssuesFilter filter, BaJiraOptions baJiraOptions, OUConfiguration ouConfig,
                                                                       Integer page, Integer pageSize) throws BadRequestException {
        return doCalculateIssueFTE(company, across, filter, baJiraOptions, Calculation.TICKET_COUNT, ouConfig, page, pageSize);
    }

    public DbListResponse<DbAggregationResult> calculateStoryPointsFTE(String company, JiraAcross across, JiraIssuesFilter filter, BaJiraOptions baJiraOptions, OUConfiguration ouConfig) throws BadRequestException {
        return calculateStoryPointsFTE(company, across, filter, baJiraOptions, ouConfig, 0, Integer.MAX_VALUE);
    }

    public DbListResponse<DbAggregationResult> calculateStoryPointsFTE(String company, JiraAcross across, JiraIssuesFilter filter, BaJiraOptions baJiraOptions, OUConfiguration ouConfig,
                                                                       Integer page, Integer pageSize) throws BadRequestException {
        return doCalculateIssueFTE(company, across, filter, baJiraOptions, Calculation.STORY_POINTS, ouConfig, page, pageSize);
    }

    public DbListResponse<DbAggregationResult> calculateTicketTimeSpentFTE(String company, JiraAcross across, JiraIssuesFilter filter, BaJiraOptions baJiraOptions, OUConfiguration ouConfig) throws BadRequestException {
        return calculateTicketTimeSpentFTE(company, across, filter, baJiraOptions, ouConfig, 0, Integer.MAX_VALUE);
    }

    public DbListResponse<DbAggregationResult> calculateTicketTimeSpentFTE(String company, JiraAcross across, JiraIssuesFilter filter, BaJiraOptions baJiraOptions, OUConfiguration ouConfig,
                                                                           Integer page, Integer pageSize) throws BadRequestException {
        return doCalculateIssueFTE(company, across, filter, baJiraOptions, Calculation.TICKET_TIME_SPENT, ouConfig, page, pageSize);
    }

    protected DbListResponse<DbAggregationResult> doCalculateIssueFTE(String company, JiraAcross across, JiraIssuesFilter filter, @Nullable BaJiraOptions baJiraOptions, Calculation calculation, OUConfiguration ouConfig,
                                                                      Integer page, Integer pageSize) throws BadRequestException {
        baJiraOptions = (baJiraOptions != null) ? baJiraOptions : BaJiraOptions.builder().build();
        Query query = baJiraAggsQueryBuilder.buildIssueFTEQuery(company, filter, ouConfig, across, baJiraOptions, calculation, page, pageSize);

        String sql = StringSubstitutor.replace(query.getSql(), Map.of("company", company));
        String countSql = StringSubstitutor.replace(query.getCountSql(), Map.of("company", company));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", query.getParams());
        List<DbAggregationResult> result = template.query(sql, query.getParams(), query.getRowMapper());
        Integer count = template.queryForObject(countSql, query.getParams(), Integer.class);
        return DbListResponse.of(result, count);
    }
    // endregion

    // region jira scm commits

    public DbListResponse<DbAggregationResult> calculateScmCommitCountFTE(String company, ScmAcross across, ScmCommitFilter scmCommitFilter, JiraIssuesFilter jiraIssuesFilter, OUConfiguration ouConfig) throws BadRequestException, NotFoundException {
        return calculateScmCommitCountFTE(company, across, scmCommitFilter, jiraIssuesFilter, ouConfig, 0 , Integer.MAX_VALUE);
    }

    public DbListResponse<DbAggregationResult> calculateScmCommitCountFTE(String company, ScmAcross across, ScmCommitFilter scmCommitFilter, JiraIssuesFilter jiraIssuesFilter, OUConfiguration ouConfig,
                                                                          Integer page, Integer pageSize) throws BadRequestException, NotFoundException {
        TicketCategorizationScheme scheme = retrieveTicketCategorizationScheme(company, jiraIssuesFilter);

        Query query = baJiraScmAggsQueryBuilder.buildScmCommitCountFTEQuery(company, scmCommitFilter, jiraIssuesFilter, ouConfig, scheme, across, page, pageSize);

        String sql = StringSubstitutor.replace(query.getSql(), Map.of("company", company));
        String countSql = StringSubstitutor.replace(query.getCountSql(), Map.of("company", company));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", query.getParams());
        List<DbAggregationResult> result = template.query(sql, query.getParams(), query.getRowMapper());
        Integer count = template.queryForObject(countSql, query.getParams(), Integer.class);
        return DbListResponse.of(result, count);
    }

    // endregion

    // region active work

    public DbListResponse<DbAggregationResult> calculateTicketCountActiveWork(String company, JiraAcross across, JiraIssuesFilter filter, OUConfiguration ouConfig) throws BadRequestException, NotFoundException {
        return calculateTicketCountActiveWork(company, across, filter, ouConfig, 0, Integer.MAX_VALUE);
    }

    public DbListResponse<DbAggregationResult> calculateTicketCountActiveWork(String company, JiraAcross across, JiraIssuesFilter filter, OUConfiguration ouConfig,
                                                                              Integer page, Integer pageSize) throws BadRequestException, NotFoundException {
        return doCalculateActiveWork(company, across, filter, Calculation.TICKET_COUNT, ouConfig, page, pageSize);
    }

    public DbListResponse<DbAggregationResult> calculateStoryPointsActiveWork(String company, JiraAcross across, JiraIssuesFilter filter, OUConfiguration ouConfig) throws BadRequestException, NotFoundException {
        return calculateStoryPointsActiveWork(company, across, filter, ouConfig, 0, Integer.MAX_VALUE);
    }

    public DbListResponse<DbAggregationResult> calculateStoryPointsActiveWork(String company, JiraAcross across, JiraIssuesFilter filter, OUConfiguration ouConfig,
                                                                              Integer page, Integer pageSize) throws BadRequestException, NotFoundException {
        return doCalculateActiveWork(company, across, filter, Calculation.STORY_POINTS, ouConfig, page, pageSize);
    }

    protected DbListResponse<DbAggregationResult> doCalculateActiveWork(String company, JiraAcross across, JiraIssuesFilter filter, Calculation calculation, OUConfiguration ouConfig) throws BadRequestException, NotFoundException {
        return doCalculateActiveWork(company, across, filter, calculation, ouConfig, 0, Integer.MAX_VALUE);
    }

    protected DbListResponse<DbAggregationResult> doCalculateActiveWork(String company, JiraAcross across, JiraIssuesFilter filter, Calculation calculation, OUConfiguration ouConfig,
                                                                            Integer page, Integer pageSize) throws BadRequestException, NotFoundException {
        TicketCategorizationScheme scheme = retrieveTicketCategorizationScheme(company, filter);

        Query query = baJiraAggsActiveWorkQueryBuilder.buildActiveWorkSql(company, filter, ouConfig, across, scheme, calculation, page, pageSize);

        String sql = StringSubstitutor.replace(query.getSql(), Map.of("company", company));
        String countSql = StringSubstitutor.replace(query.getCountSql(), Map.of("company", company));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", query.getParams());
        List<DbAggregationResult> result = template.query(sql, query.getParams(), query.getRowMapper());
        Integer count = template.queryForObject(countSql, query.getParams(), Integer.class);
        return DbListResponse.of(result, count);
    }

    // endregion

    private TicketCategorizationScheme retrieveTicketCategorizationScheme(String company, JiraIssuesFilter filter) throws NotFoundException {
        String ticketCategorizationSchemeId = filter.getTicketCategorizationSchemeId();
        Validate.notBlank(ticketCategorizationSchemeId, "ticketCategorizationSchemeId cannot be null or empty.");
        return ticketCategorizationSchemeDatabaseService.get(company, ticketCategorizationSchemeId)
                .orElseThrow(() -> new NotFoundException("Could not find ticket categorization scheme with id=" + ticketCategorizationSchemeId));
    }


}
