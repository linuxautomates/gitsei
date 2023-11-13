package io.levelops.commons.databases.services.velocity;

import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.VelocityAggsConverters;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.cicd_scm.DBDummyObj;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.*;
import io.levelops.commons.databases.services.*;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Log4j2
@Service
public class VelocityAggsDatabaseService extends DatabaseService<DBDummyObj>  {
    private final NamedParameterJdbcTemplate template;
    private final JiraConditionsBuilder jiraConditionsBuilder;
    private final ScmAggService scmAggService;
    private final CiCdMetadataConditionBuilder ciCdMetadataConditionBuilder;
    private final CiCdPartialMatchConditionBuilder ciCdPartialMatchConditionBuilder;

    // region CSTOR
    @Autowired
    public VelocityAggsDatabaseService(DataSource dataSource, JiraConditionsBuilder JiraConditionsBuilder,
                                       ScmAggService scmAggService, CiCdMetadataConditionBuilder ciCdMetadataConditionBuilder, CiCdPartialMatchConditionBuilder ciCdPartialMatchConditionBuilder) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.jiraConditionsBuilder = JiraConditionsBuilder;
        this.scmAggService = scmAggService;
        this.ciCdMetadataConditionBuilder = ciCdMetadataConditionBuilder;
        this.ciCdPartialMatchConditionBuilder = ciCdPartialMatchConditionBuilder;
    }
    // endregion

    // region Get References
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(JiraIssueService.class, ScmAggService.class, CiCdScmMappingService.class,
                CiCdInstancesDatabaseService.class, CiCdJobsDatabaseService.class,
                CiCdJobRunsDatabaseService.class);
    }
    // endregion

    // region Unused
    @Override
    public String insert(String company, DBDummyObj t) throws SQLException {
        throw new UnsupportedOperationException();
    }
    @Override
    public Boolean update(String company, DBDummyObj t) throws SQLException {
        throw new UnsupportedOperationException();
    }
    @Override
    public Optional<DBDummyObj> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }
    @Override
    public DbListResponse<DBDummyObj> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        return true;
    }
    // endregion

    public DbListResponse<List<DbAggregationResult>> calculateVelocityWithoutStacks(
                                                                                    String company,
                                                                                    VelocityConfigDTO velocityConfigDTO,
                                                                                    VelocityFilter velocityFilter,
                                                                                    WorkItemsType workItemsType,
                                                                                    JiraIssuesFilter jiraFilter,
                                                                                    WorkItemsFilter workItemsFilter,
                                                                                    WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                                    ScmPrFilter scmPrFilter,
                                                                                    ScmCommitFilter scmCommitFilter,
                                                                                    CiCdJobRunsFilter ciCdJobRunsFilter,
                                                                                    List<DbWorkItemField> workItemCustomFields,
                                                                                    OUConfiguration ouConfig,
                                                                                    boolean disablePrJiraCorrelationForPrVelocity) throws BadRequestException {
        final VelocityAggsQueryBuilder velocityAggsQueryBuilder = new VelocityAggsQueryBuilder(jiraConditionsBuilder, scmAggService, disablePrJiraCorrelationForPrVelocity, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);;
        VelocityAggsQueryBuilder.Query query = velocityAggsQueryBuilder.buildQuery(company, velocityConfigDTO, velocityFilter, workItemsType, jiraFilter, workItemsFilter, workItemsMilestoneFilter, scmPrFilter, scmCommitFilter, ciCdJobRunsFilter, workItemCustomFields, ouConfig);

        StringSubstitutor sub = new StringSubstitutor(Map.of("company", company));
        String sql = sub.replace(query.getSql());
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", query.getParams());
        List<List<DbAggregationResult>> result = template.query(sql, query.getParams(), VelocityAggsConverters.mapVelocityAgg(velocityFilter, query.getOffsetStageMap(), velocityConfigDTO.getCicdJobIdNameMappings()));
        return DbListResponse.of(result, result.size());
    }

    public DbListResponse<DbAggregationResult> calculateVelocityValues(
                                                                        String company,
                                                                        VelocityConfigDTO velocityConfigDTO,
                                                                        VelocityFilter velocityFilter,
                                                                        WorkItemsType workItemsType,
                                                                        JiraIssuesFilter jiraFilter,
                                                                        WorkItemsFilter workItemsFilter,
                                                                        WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                        ScmPrFilter scmPrFilter,
                                                                        ScmCommitFilter scmCommitFilter,
                                                                        CiCdJobRunsFilter ciCdJobRunsFilter,
                                                                        List<DbWorkItemField> workItemCustomFields,
                                                                        OUConfiguration ouConfig,
                                                                        boolean disablePrJiraCorrelationForPrVelocity) throws BadRequestException {
        final VelocityAggsQueryBuilder velocityAggsQueryBuilder = new VelocityAggsQueryBuilder(jiraConditionsBuilder, scmAggService, disablePrJiraCorrelationForPrVelocity, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);;
        VelocityAggsQueryBuilder.Query query = velocityAggsQueryBuilder.buildQuery(company, velocityConfigDTO, velocityFilter, workItemsType, jiraFilter, workItemsFilter, workItemsMilestoneFilter, scmPrFilter, scmCommitFilter, ciCdJobRunsFilter, workItemCustomFields, ouConfig);

        StringSubstitutor sub = new StringSubstitutor(Map.of("company", company));
        String sql = sub.replace(query.getSql());
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", query.getParams());
        List<DbAggregationResult> result = template.query(sql, query.getParams(), VelocityAggsConverters.mapVelocityValues(velocityFilter, query.getOffsetStageMap(), velocityConfigDTO.getCicdJobIdNameMappings()));
        Integer totCount = 0;
        Integer pageNumber = velocityFilter.getPage();
        Integer pageSize = velocityFilter.getPageSize();
        if (result.size() > 0) {
            totCount = result.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (result.size() == pageSize) {
                String countSql = sub.replace(query.getCountSql());
                log.info("sql = " + countSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.info("params = {}", query.getParams());
                totCount = template.query(countSql, query.getParams(), CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(result, totCount);
    }

    public DbHistogramResult calculateVelocityStageHistogram(
                                                                String company,
                                                                VelocityConfigDTO velocityConfigDTO,
                                                                VelocityFilter velocityFilter,
                                                                WorkItemsType workItemsType,
                                                                JiraIssuesFilter jiraFilter,
                                                                WorkItemsFilter workItemsFilter,
                                                                WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                ScmPrFilter scmPrFilter,
                                                                ScmCommitFilter scmCommitFilter,
                                                                CiCdJobRunsFilter ciCdJobRunsFilter,
                                                                List<DbWorkItemField> workItemCustomFields,
                                                                OUConfiguration ouConfig,
                                                                boolean disablePrJiraCorrelationForPrVelocity) throws BadRequestException {
        final VelocityAggsQueryBuilder velocityAggsQueryBuilder = new VelocityAggsQueryBuilder(jiraConditionsBuilder, scmAggService, disablePrJiraCorrelationForPrVelocity, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);;
        VelocityAggsQueryBuilder.Query query = velocityAggsQueryBuilder.buildQuery(company, velocityConfigDTO, velocityFilter, workItemsType, jiraFilter, workItemsFilter, workItemsMilestoneFilter, scmPrFilter, scmCommitFilter, ciCdJobRunsFilter, workItemCustomFields, ouConfig);

        StringSubstitutor sub = new StringSubstitutor(Map.of("company", company));
        String sql = sub.replace(query.getSql());
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", query.getParams());
        List<DbHistogramBucket> buckets = template.query(sql, query.getParams(), VelocityAggsConverters.mapVelocityHistogram());
        DbHistogramResult result = DbHistogramResult.builder()
                .name(velocityFilter.getHistogramStageName())
                .buckets(buckets)
                .build();
        log.info("result = {}", result);
        return result;
    }

    public VelocityRatingResult calculateVelocityStageRating(
            String company,
            VelocityConfigDTO velocityConfigDTO,
            VelocityFilter velocityFilter,
            WorkItemsType workItemsType,
            JiraIssuesFilter jiraFilter,
            WorkItemsFilter workItemsFilter,
            WorkItemsMilestoneFilter workItemsMilestoneFilter,
            ScmPrFilter scmPrFilter,
            ScmCommitFilter scmCommitFilter,
            CiCdJobRunsFilter ciCdJobRunsFilter,
            List<DbWorkItemField> workItemCustomFields,
            OUConfiguration ouConfig,
            boolean disablePrJiraCorrelationForPrVelocity) throws BadRequestException {
        final VelocityAggsQueryBuilder velocityAggsQueryBuilder = new VelocityAggsQueryBuilder(jiraConditionsBuilder, scmAggService, disablePrJiraCorrelationForPrVelocity, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);;
        VelocityAggsQueryBuilder.Query query = velocityAggsQueryBuilder.buildQuery(company, velocityConfigDTO, velocityFilter, workItemsType, jiraFilter, workItemsFilter, workItemsMilestoneFilter, scmPrFilter, scmCommitFilter, ciCdJobRunsFilter, workItemCustomFields, ouConfig);

        StringSubstitutor sub = new StringSubstitutor(Map.of("company", company));
        String sql = sub.replace(query.getSql());
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", query.getParams());
        List<VelocityCountByRating> buckets = template.query(sql, query.getParams(), VelocityAggsConverters.mapVelocityCountByRating());
        VelocityRatingResult result = VelocityRatingResult.builder()
                .name(velocityFilter.getHistogramStageName())
                .buckets(buckets)
                .build();
        log.info("result = {}", result);
        return result;
    }
}
