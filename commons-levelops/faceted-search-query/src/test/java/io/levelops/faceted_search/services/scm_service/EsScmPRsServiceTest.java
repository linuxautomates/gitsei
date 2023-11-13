package io.levelops.faceted_search.services.scm_service;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.Alias;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmPrSorting;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESContext;
import io.levelops.commons.faceted_search.db.models.scm.EsScmPullRequest;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.SneakyThrows;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EsScmPRsServiceTest {

    private static ESContext esContext;
    private static final EsTestUtils esTestUtils = new EsTestUtils();
    private static EsScmPRsService esScmPRsService;
    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;
    private static NamedParameterJdbcTemplate template;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String index = "scm_prs_test";
    public static String company = "test";

    @BeforeClass
    public static void startESCreateClient() throws IOException, InterruptedException, SQLException {

        esContext = esTestUtils.initializeESClient();
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        template = new NamedParameterJdbcTemplate(dataSource);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);

        createIndex();
        insertData();
        insertDBData();

        ESClientFactory esClientFactory = EsTestUtils.buildESClientFactory(esContext);
        esScmPRsService = new EsScmPRsService(esClientFactory, dataSource, userIdentityService);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
    }

    @Test
    public void testAggregation() throws IOException {
        ScmPrFilter filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.project)
                .calculation(ScmPrFilter.CALCULATION.count)
                .build();

        DbListResponse<DbAggregationResult> res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);

        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(147);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(99);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(246);
        Assertions.assertThat(result.get(0).getTotalComments()).isEqualTo(4);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(10);
        Assertions.assertThat(result.get(0).getAvgLinesChanged()).isEqualTo(61.5);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("levelops/api-levelops");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(3);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(5);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(25);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(30);
        Assertions.assertThat(result.get(1).getTotalComments()).isEqualTo(3);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(4);
        //ToDo: Fix Unit Tests
        //Assertions.assertThat(result.get(1).getAvgLinesChanged()).isEqualTo(15.0);


        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.project)
                .calculation(ScmPrFilter.CALCULATION.count)
                .approverCount(ImmutablePair.of(0l, 2l))
                .build();

        res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(2);


        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.collab_state)
                .calculation(ScmPrFilter.CALCULATION.count)
                .build();

        res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(2);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("unapproved");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(8);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(153);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(104);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(257);
        Assertions.assertThat(result.get(0).getTotalComments()).isEqualTo(8);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(15);
        //ToDo: Fix Unit Tests
        //Assertions.assertThat(result.get(0).getAvgLinesChanged()).isEqualTo(36.71);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("unassigned-peer-approved");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(82);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(52);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(134);
        Assertions.assertThat(result.get(1).getTotalComments()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(4);
        Assertions.assertThat(result.get(1).getAvgLinesChanged()).isEqualTo(67.0);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.technology)
                .calculation(ScmPrFilter.CALCULATION.count)
                .build();

        res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(7);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("Java");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(9);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(168);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(124);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(292);
        Assertions.assertThat(result.get(0).getTotalComments()).isEqualTo(8);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(16);
        //ToDo: Fix Unit Tests
        //Assertions.assertThat(result.get(0).getAvgLinesChanged()).isEqualTo(36.5);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("Dockerfile");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(5);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(80);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(57);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(137);
        Assertions.assertThat(result.get(1).getTotalComments()).isEqualTo(6);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(8);
        //ToDo: Fix Unit Tests
        //Assertions.assertThat(result.get(1).getAvgLinesChanged()).isEqualTo(34.25);


        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.creator)
                .calculation(ScmPrFilter.CALCULATION.count)
                .build();

        res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(32);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(48);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(80);
        Assertions.assertThat(result.get(0).getTotalComments()).isEqualTo(7);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(9);
        Assertions.assertThat(result.get(0).getAvgLinesChanged()).isEqualTo(20.0);

        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(3);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(16);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(20);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(36);
        Assertions.assertThat(result.get(1).getTotalComments()).isEqualTo(1);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(3);
        Assertions.assertThat(result.get(1).getAvgLinesChanged()).isEqualTo(12.0);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.pr_created)
                .aggInterval(AGG_INTERVAL.year)
                .calculation(ScmPrFilter.CALCULATION.count)
                .build();

        res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);
        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("2021");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(10);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(235);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(156);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(391);
        Assertions.assertThat(result.get(0).getTotalComments()).isEqualTo(10);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(19);
        //ToDo: Fix Unit Tests
        //Assertions.assertThat(result.get(0).getAvgLinesChanged()).isEqualTo(43.44);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.pr_created)
                .aggInterval(AGG_INTERVAL.quarter)
                .calculation(ScmPrFilter.CALCULATION.count)
                .build();

        res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);
        //Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("Q3-2021"); time sensitive case - causing failure on jenkins
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(10);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(235);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(156);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(391);
        Assertions.assertThat(result.get(0).getTotalComments()).isEqualTo(10);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(19);
        //ToDo: Fix Unit Tests
        //Assertions.assertThat(result.get(0).getAvgLinesChanged()).isEqualTo(43.44);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.pr_created)
                .aggInterval(AGG_INTERVAL.month)
                .calculation(ScmPrFilter.CALCULATION.count)
                .build();

        res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(2);
        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("11-2021");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(9);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(115);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(100);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(215);
        Assertions.assertThat(result.get(0).getTotalComments()).isEqualTo(10);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(15);
        //ToDo: Fix Unit Tests
        //Assertions.assertThat(result.get(0).getAvgLinesChanged()).isEqualTo(26.88);

        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("12-2021");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(120);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(56);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(176);
        Assertions.assertThat(result.get(1).getTotalComments()).isEqualTo(0);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(4);
        Assertions.assertThat(result.get(1).getAvgLinesChanged()).isEqualTo(176.0);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.pr_created)
                .aggInterval(AGG_INTERVAL.week)
                .calculation(ScmPrFilter.CALCULATION.count)
                .build();

        res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(4);
        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("46-2021");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(21);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(25);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(46);
        Assertions.assertThat(result.get(0).getTotalComments()).isEqualTo(4);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(6);
        Assertions.assertThat(result.get(0).getAvgLinesChanged()).isEqualTo(11.5);

        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("47-2021");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(3);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(94);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(52);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(146);
        Assertions.assertThat(result.get(1).getTotalComments()).isEqualTo(4);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(7);
        Assertions.assertThat(result.get(1).getAvgLinesChanged()).isEqualTo(48.67);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.pr_created)
                .aggInterval(AGG_INTERVAL.day)
                .calculation(ScmPrFilter.CALCULATION.count)
                .build();

        res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(9);


        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.code_change)
                .calculation(ScmPrFilter.CALCULATION.count)
                .build();

        res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(2);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("small");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(9);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(115);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(100);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(215);
        Assertions.assertThat(result.get(0).getTotalComments()).isEqualTo(10);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(15);
        Assertions.assertThat(result.get(0).getAvgLinesChanged()).isEqualTo(23.89);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("medium");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(120);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(56);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(176);
        Assertions.assertThat(result.get(1).getTotalComments()).isEqualTo(0);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(4);
        Assertions.assertThat(result.get(1).getAvgLinesChanged()).isEqualTo(176.0);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.comment_density)
                .calculation(ScmPrFilter.CALCULATION.count)
                .build();

        res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("shallow");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(10);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(235);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(156);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(391);
        Assertions.assertThat(result.get(0).getTotalComments()).isEqualTo(10);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(19);
        //ToDo: Fix Unit Tests
        //Assertions.assertThat(result.get(0).getAvgLinesChanged()).isEqualTo(43.44);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.reviewer_count)
                .calculation(ScmPrFilter.CALCULATION.count)
                .build();

        res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.get(0).getKey()).isEqualTo("1");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(5);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(98);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(72);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(170);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.creator)
                .hasIssueKeys("true")
                .build();

        res = esScmPRsService.groupByAndCalculatePrs(company, filter, false, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);
        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("viraj-levelops");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(3);
        Assertions.assertThat(result.get(2).getAdditionalKey()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(2).getCount()).isEqualTo(2);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.creator)
                .hasIssueKeys("false")
                .build();

        res = esScmPRsService.groupByAndCalculatePrs(company, filter, false, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);
        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(1);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.repo_id)
                .calculation(ScmPrFilter.CALCULATION.author_response_time)
                .build();

        res = esScmPRsService.groupByAndCalculatePrs(company, filter, false, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.project)
                .calculation(ScmPrFilter.CALCULATION.author_response_time)
                .sort(Map.of(ScmPrSorting.MEAN_AUTHOR_RESPONSE_TIME, SortingOrder.ASC))
                .build();

        res = esScmPRsService.groupByAndCalculatePrs(company, filter, false, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);
        Assertions.assertThat(result.stream().map(DbAggregationResult::getMean).collect(Collectors.toList())).isSortedAccordingTo(Double::compareTo);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.project)
                .calculation(ScmPrFilter.CALCULATION.author_response_time)
                .sort(Map.of(ScmPrFilter.DISTINCT.project.toString(), SortingOrder.ASC))
                .build();

        res = esScmPRsService.groupByAndCalculatePrs(company, filter, false, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);
        Assertions.assertThat(result.stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).isSortedAccordingTo(String::compareTo);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.project)
                .calculation(ScmPrFilter.CALCULATION.author_response_time)
                .sort(Map.of(ScmPrFilter.DISTINCT.project.toString(), SortingOrder.DESC))
                .build();

        res = esScmPRsService.groupByAndCalculatePrs(company, filter, false, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);
        Assertions.assertThat(result.stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).isSortedAccordingTo(Collections.reverseOrder());

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.branch)
                .calculation(ScmPrFilter.CALCULATION.author_response_time)
                .sort(Map.of(ScmPrFilter.DISTINCT.branch.toString(), SortingOrder.ASC))
                .build();

        res = esScmPRsService.groupByAndCalculatePrs(company, filter, false, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).isSortedAccordingTo(String::compareTo);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.branch)
                .calculation(ScmPrFilter.CALCULATION.author_response_time)
                .sort(Map.of(ScmPrFilter.DISTINCT.branch.toString(), SortingOrder.DESC))
                .build();

        res = esScmPRsService.groupByAndCalculatePrs(company, filter, false, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.stream().map(DbAggregationResult::getKey).collect(Collectors.toList())).isSortedAccordingTo(Collections.reverseOrder());

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.label)
                .build();

        res = esScmPRsService.groupByAndCalculatePrs(company, filter, false, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);
        Assertions.assertThat(result.get(0).getKey()).isEqualTo("ready for review");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(1);
    }

    public void testValue() throws IOException {
        ScmPrFilter filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.project)
                .sort(Map.of(ScmPrSorting.MEAN_AUTHOR_RESPONSE_TIME, SortingOrder.ASC))
                .build();

        DbListResponse<DbAggregationResult> res = esScmPRsService.groupByAndCalculatePrs(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);
        List<String> projectValues = result.stream().map(DbAggregationResult::getKey).collect(Collectors.toList());
        Assertions.assertThat(projectValues).containsExactlyInAnyOrder("levelops/api-levelops", "levelops/aggregations-levelops", "levelops/integrations-levelops", "levelops/commons-levelops", "levelops/devops-levelops");

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.label)
                .build();

        res = esScmPRsService.groupByAndCalculatePrs(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);
        Assertions.assertThat(result.get(0).getKey()).isEqualTo("ready for review");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(1);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.creator)
                .build();

        res = esScmPRsService.groupByAndCalculatePrs(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);
        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(3);
        Assertions.assertThat(result.get(2).getAdditionalKey()).isEqualTo("viraj-levelops");
        Assertions.assertThat(result.get(2).getCount()).isEqualTo(3);
    }

    @Test
    public void testCodeChange() throws IOException {

        ScmPrFilter filter = ScmPrFilter.builder()
                .codeChanges(List.of("small"))
                .codeChangeSizeConfig(Map.of("small", "20", "medium", "100"))
                .codeChangeUnit("lines")
                .build();

        DbListResponse<DbScmPullRequest> res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        List<DbScmPullRequest> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(6);

         filter = ScmPrFilter.builder()
                 .codeChanges(List.of("medium"))
                 .codeChangeSizeConfig(Map.of("small", "20", "medium", "100"))
                .codeChangeUnit("lines")
                .build();

         res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        filter = ScmPrFilter.builder()
                .codeChanges(List.of("large"))
                .codeChangeSizeConfig(Map.of("small", "20", "medium", "100"))
                .codeChangeUnit("lines")
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        filter = ScmPrFilter.builder()
                .excludeCodeChanges(List.of("small"))
                .codeChangeSizeConfig(Map.of("small", "20", "medium", "100"))
                .codeChangeUnit("lines")
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(4);

        filter = ScmPrFilter.builder()
                .codeChanges(List.of("small"))
                .codeChangeSizeConfig(Map.of("small", "1", "medium", "3"))
                .codeChangeUnit("files")
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);

        filter = ScmPrFilter.builder()
                .codeChanges(List.of("medium"))
                .codeChangeSizeConfig(Map.of("small", "1", "medium", "3"))
                .codeChangeUnit("files")
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(4);

        filter = ScmPrFilter.builder()
                .codeChanges(List.of("large"))
                .codeChangeSizeConfig(Map.of("small", "1", "medium", "3"))
                .codeChangeUnit("files")
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        filter = ScmPrFilter.builder()
                .excludeCodeChanges(List.of("small"))
                .codeChangeSizeConfig(Map.of("small", "1", "medium", "3"))
                .codeChangeUnit("files")
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);

    }

    @Test
    public void testList() throws IOException {

        ScmPrFilter filter = ScmPrFilter.builder()
                .projects(List.of("levelops/commons-levelops"))
                .creators(List.of("14143532-db10-497d-b9ac-f4b7e8c9050e"))
                .across(ScmPrFilter.DISTINCT.project)
                .build();

        DbListResponse<DbScmPullRequest> res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        List<DbScmPullRequest> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getNumber()).isEqualTo("2466");
        Assertions.assertThat(result.get(0).getReviewType()).isEqualTo("SELF_REVIEWED");
        Assertions.assertThat(result.get(0).getMergedDay()).isEqualTo("Monday");
        Assertions.assertThat(result.get(0).getPrCreatedAt()).isEqualTo(1637925401l);
        Assertions.assertThat(result.get(0).getPrClosedAt()).isEqualTo(1638150060l);
        Assertions.assertThat(result.get(0).getPrMergedAt()).isEqualTo(1638150060);
        Assertions.assertThat(result.get(0).getPrUpdatedAt()).isEqualTo(1638150714);

        Assertions.assertThat(result.get(1).getNumber()).isEqualTo("2442");
        Assertions.assertThat(result.get(1).getReviewType()).isEqualTo("SELF_REVIEWED");
        Assertions.assertThat(result.get(2).getMergedDay()).isEqualTo("Thursday");

        Assertions.assertThat(result.get(2).getNumber()).isEqualTo("2436");
        Assertions.assertThat(result.get(2).getReviewType()).isEqualTo("PEER_REVIEWED");
        Assertions.assertThat(result.get(2).getMergedDay()).isEqualTo("Thursday");

        filter = ScmPrFilter.builder()
                .prCreatedDaysOfWeek(List.of("Thursday"))
                .approvalStatuses(List.of("not reviewed"))
                .integrationIds(List.of("1849"))
                .reviewTypes(List.of("NOT_REVIEWED"))
                .targetBranches(List.of("main"))
                .sourceBranches(List.of("jen"))
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        Assertions.assertThat(result.get(0).getNumber()).isEqualTo("2501");
        Assertions.assertThat(result.get(0).getReviewType()).isEqualTo("NOT_REVIEWED");
        Assertions.assertThat(result.get(0).getState()).isEqualTo("closed");

        filter = ScmPrFilter.builder()
                .prCreatedRange(ImmutablePair.of(1636440042L, 1637196168L))
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(4);

        Assertions.assertThat(result.get(2).getProject()).isEqualTo("levelops/api-levelops");
        Assertions.assertThat(result.get(2).getNumber()).isEqualTo("1153");
        Assertions.assertThat(result.get(2).getCreator()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(2).getReviewType()).isEqualTo("NOT_REVIEWED");
        Assertions.assertThat(result.get(2).getState()).isEqualTo("closed");

        Assertions.assertThat(result.get(1).getProject()).isEqualTo("levelops/devops-levelops");
        Assertions.assertThat(result.get(1).getNumber()).isEqualTo("1110");
        Assertions.assertThat(result.get(1).getCreator()).isEqualTo("viraj-levelops");
        Assertions.assertThat(result.get(1).getReviewType()).isEqualTo("PEER_REVIEWED");
        Assertions.assertThat(result.get(1).getState()).isEqualTo("closed");

        filter = ScmPrFilter.builder()
                .codeChangeSizeConfig(Map.of("small", "10", "medium", "100"))
                .codeChanges(List.of("medium"))
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        //ToDo: Fix Unit Tests
        //Assertions.assertThat(result.size()).isEqualTo(6);

        Assertions.assertThat(result.get(4).getProject()).isEqualTo("levelops/api-levelops");
        Assertions.assertThat(result.get(4).getNumber()).isEqualTo("1153");
        Assertions.assertThat(result.get(4).getCreator()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(4).getChange()).isEqualTo(20);

        Assertions.assertThat(result.get(3).getProject()).isEqualTo("levelops/devops-levelops");
        Assertions.assertThat(result.get(3).getNumber()).isEqualTo("1110");
        Assertions.assertThat(result.get(3).getCreator()).isEqualTo("viraj-levelops");
        Assertions.assertThat(result.get(3).getChange()).isEqualTo(99);

        Assertions.assertThat(result.get(0).getProject()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(0).getNumber()).isEqualTo("2466");
        Assertions.assertThat(result.get(0).getCreator()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(0).getChange()).isEqualTo(23);

        filter = ScmPrFilter.builder()
                .codeChangeSizeConfig(Map.of("small", "10", "medium", "100"))
                .codeChanges(List.of("large"))
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        Assertions.assertThat(result.get(0).getProject()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(0).getNumber()).isEqualTo("2501");
        Assertions.assertThat(result.get(0).getCreator()).isEqualTo("viraj-levelops");
        Assertions.assertThat(result.get(0).getChange()).isEqualTo(176);

        filter = ScmPrFilter.builder()
                .commentDensitySizeConfig(Map.of("shallow", "1", "medium", "3"))
                .commentDensities(List.of("heavy"))
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(0);

        filter = ScmPrFilter.builder()
                .commentDensitySizeConfig(Map.of("shallow", "1", "medium", "3"))
                .commentDensities(List.of("good"))
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(4);

        Assertions.assertThat(result.get(2).getProject()).isEqualTo("levelops/devops-levelops");
        Assertions.assertThat(result.get(2).getNumber()).isEqualTo("1110");
        Assertions.assertThat(result.get(2).getCreator()).isEqualTo("viraj-levelops");
        Assertions.assertThat(result.get(2).getCommentCount()).isEqualTo(2);

        Assertions.assertThat(result.get(0).getProject()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(0).getNumber()).isEqualTo("2466");
        Assertions.assertThat(result.get(0).getCreator()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(0).getCommentCount()).isEqualTo(2);

        filter = ScmPrFilter.builder()
                .projects(List.of("levelops/commons-levelops"))
                .creators(List.of("14143532-db10-497d-b9ac-f4b7e8c9050e"))
                .across(ScmPrFilter.DISTINCT.project)
                .sort(Map.of(ScmPrSorting.MEAN_AUTHOR_RESPONSE_TIME, SortingOrder.ASC))
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getNumber()).isEqualTo("2466");
        Assertions.assertThat(result.get(0).getReviewType()).isEqualTo("SELF_REVIEWED");
        Assertions.assertThat(result.get(0).getMergedDay()).isEqualTo("Monday");
        Assertions.assertThat(result.get(0).getPrCreatedAt()).isEqualTo(1637925401l);
        Assertions.assertThat(result.get(0).getPrClosedAt()).isEqualTo(1638150060l);
        Assertions.assertThat(result.get(0).getPrMergedAt()).isEqualTo(1638150060);
        Assertions.assertThat(result.get(0).getPrUpdatedAt()).isEqualTo(1638150714);

        Assertions.assertThat(result.get(1).getNumber()).isEqualTo("2442");
        Assertions.assertThat(result.get(1).getReviewType()).isEqualTo("SELF_REVIEWED");
        Assertions.assertThat(result.get(2).getMergedDay()).isEqualTo("Thursday");

        Assertions.assertThat(result.get(2).getNumber()).isEqualTo("2436");
        Assertions.assertThat(result.get(2).getReviewType()).isEqualTo("PEER_REVIEWED");
        Assertions.assertThat(result.get(2).getMergedDay()).isEqualTo("Thursday");

        filter = ScmPrFilter.builder()
                .projects(List.of("levelops/commons-levelops"))
                .creators(List.of("14143532-db10-497d-b9ac-f4b7e8c9050e"))
                .across(ScmPrFilter.DISTINCT.project)
                .sort(Map.of(ScmPrFilter.DISTINCT.project.toString(), SortingOrder.ASC))
                .build();

        res = esScmPRsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getNumber()).isEqualTo("2466");
        Assertions.assertThat(result.get(0).getReviewType()).isEqualTo("SELF_REVIEWED");
        Assertions.assertThat(result.get(0).getMergedDay()).isEqualTo("Monday");
        Assertions.assertThat(result.get(0).getPrCreatedAt()).isEqualTo(1637925401l);
        Assertions.assertThat(result.get(0).getPrClosedAt()).isEqualTo(1638150060l);
        Assertions.assertThat(result.get(0).getPrMergedAt()).isEqualTo(1638150060);
        Assertions.assertThat(result.get(0).getPrUpdatedAt()).isEqualTo(1638150714);

        Assertions.assertThat(result.get(1).getNumber()).isEqualTo("2442");
        Assertions.assertThat(result.get(1).getReviewType()).isEqualTo("SELF_REVIEWED");
        Assertions.assertThat(result.get(2).getMergedDay()).isEqualTo("Thursday");

        Assertions.assertThat(result.get(2).getNumber()).isEqualTo("2436");
        Assertions.assertThat(result.get(2).getReviewType()).isEqualTo("PEER_REVIEWED");
        Assertions.assertThat(result.get(2).getMergedDay()).isEqualTo("Thursday");
    }


    @Test
    public void testAvgAuthorResponseTime() throws IOException {

        ScmPrFilter filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.project)
                .calculation(ScmPrFilter.CALCULATION.author_response_time)
                .build();

        DbListResponse<DbAggregationResult> res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);

        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(0).getMedian()).isEqualTo(1066);
        Assertions.assertThat(result.get(0).getMin()).isEqualTo(0);
        Assertions.assertThat(result.get(0).getMax()).isEqualTo(3581);
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(result.get(0).getSum()).isEqualTo(4264);

        Assertions.assertThat(result.get(3).getKey()).isEqualTo("levelops/devops-levelops");
        Assertions.assertThat(result.get(3).getMedian()).isEqualTo(2223);
        Assertions.assertThat(result.get(3).getMin()).isEqualTo(2223);
        Assertions.assertThat(result.get(3).getMax()).isEqualTo(2223);
        Assertions.assertThat(result.get(3).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(3).getSum()).isEqualTo(2223);

    }

    @Test
    public void testAvgReviewerResponseTime() throws IOException {

        ScmPrFilter filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.project)
                .calculation(ScmPrFilter.CALCULATION.reviewer_response_time)
                .build();

        DbListResponse<DbAggregationResult> res = esScmPRsService.stackedPrsGroupBy(company, filter, List.of(), null, 0, 10);

        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(0).getMedian()).isEqualTo(57309);
        Assertions.assertThat(result.get(0).getMin()).isEqualTo(7);
        Assertions.assertThat(result.get(0).getMax()).isEqualTo(224444);
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(4);
        Assertions.assertThat(result.get(0).getSum()).isEqualTo(229236);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("levelops/api-levelops");
        Assertions.assertThat(result.get(1).getMedian()).isEqualTo(25638);
        Assertions.assertThat(result.get(1).getMin()).isEqualTo(8);
        Assertions.assertThat(result.get(1).getMax()).isEqualTo(70687);
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(3);
        Assertions.assertThat(result.get(1).getSum()).isEqualTo(76915);

    }

    @Test
    public void testMergeTrends() throws IOException {

        ScmPrFilter filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.pr_created)
                .calculation(ScmPrFilter.CALCULATION.merge_time)
                .aggInterval(AGG_INTERVAL.month)
                .build();

        DbListResponse<DbAggregationResult> res = esScmPRsService.groupByAndCalculatePrsDuration(company, filter, null);
        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(2);

        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("11-2021");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(9);
        Assertions.assertThat(result.get(0).getMin()).isEqualTo(8);
        Assertions.assertThat(result.get(0).getMax()).isEqualTo(224659);
        Assertions.assertThat(result.get(0).getMedian()).isEqualTo(43377);
        Assertions.assertThat(result.get(0).getSum()).isEqualTo(390396);

        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("12-2021");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(1).getMin()).isEqualTo(7);
        Assertions.assertThat(result.get(1).getMax()).isEqualTo(7);
        Assertions.assertThat(result.get(1).getMedian()).isEqualTo(7);
        Assertions.assertThat(result.get(1).getSum()).isEqualTo(7);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.pr_merged)
                .calculation(ScmPrFilter.CALCULATION.merge_time)
                .aggInterval(AGG_INTERVAL.month)
                .build();

        res = esScmPRsService.groupByAndCalculatePrsDuration(company, filter, null);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);

        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("11-2021");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(9);
        Assertions.assertThat(result.get(0).getMin()).isEqualTo(8);
        Assertions.assertThat(result.get(0).getMax()).isEqualTo(224659);
        Assertions.assertThat(result.get(0).getMedian()).isEqualTo(43377);
        Assertions.assertThat(result.get(0).getSum()).isEqualTo(390396);

        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("12-2021");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(1).getMin()).isEqualTo(7);
        Assertions.assertThat(result.get(1).getMax()).isEqualTo(7);
        Assertions.assertThat(result.get(1).getMedian()).isEqualTo(7);
        Assertions.assertThat(result.get(1).getSum()).isEqualTo(7);
    }

    @Test
    public void testFirstReviewTrends() throws IOException {

        ScmPrFilter filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.pr_created)
                .calculation(ScmPrFilter.CALCULATION.first_review_time)
                .aggInterval(AGG_INTERVAL.month)
                .build();

        DbListResponse<DbAggregationResult> res = esScmPRsService.groupByAndCalculatePrsDuration(company, filter, null);
        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("11-2021");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(6);
        Assertions.assertThat(result.get(0).getMin()).isEqualTo(0);
        Assertions.assertThat(result.get(0).getMax()).isEqualTo(225098);
        Assertions.assertThat(result.get(0).getMedian()).isEqualTo(39091);
        Assertions.assertThat(result.get(0).getSum()).isEqualTo(234546);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.pr_merged)
                .calculation(ScmPrFilter.CALCULATION.first_review_time)
                .aggInterval(AGG_INTERVAL.month)
                .build();

        res = esScmPRsService.groupByAndCalculatePrsDuration(company, filter, null);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("11-2021");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(6);
        Assertions.assertThat(result.get(0).getMin()).isEqualTo(0);
        Assertions.assertThat(result.get(0).getMax()).isEqualTo(225098);
        Assertions.assertThat(result.get(0).getMedian()).isEqualTo(39091);
        Assertions.assertThat(result.get(0).getSum()).isEqualTo(234546);

    }

    @Test
    public void testFirstReviewMergeTrends() throws IOException {

        ScmPrFilter filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.pr_created)
                .calculation(ScmPrFilter.CALCULATION.first_review_to_merge_time)
                .aggInterval(AGG_INTERVAL.month)
                .build();

        DbListResponse<DbAggregationResult> res = esScmPRsService.groupByAndCalculatePrsDuration(company, filter, null);
        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("11-2021");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(6);
        Assertions.assertThat(result.get(0).getMin()).isEqualTo(0);
        Assertions.assertThat(result.get(0).getMax()).isEqualTo(2223);
        Assertions.assertThat(result.get(0).getMedian()).isEqualTo(572);
        Assertions.assertThat(result.get(0).getSum()).isEqualTo(3434);

        filter = ScmPrFilter.builder()
                .across(ScmPrFilter.DISTINCT.pr_closed)
                .calculation(ScmPrFilter.CALCULATION.first_review_time)
                .aggInterval(AGG_INTERVAL.month)
                .build();

        res = esScmPRsService.groupByAndCalculatePrsDuration(company, filter, null);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("11-2021");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(6);
        Assertions.assertThat(result.get(0).getMin()).isEqualTo(0);
        Assertions.assertThat(result.get(0).getMax()).isEqualTo(225098);
        Assertions.assertThat(result.get(0).getMedian()).isEqualTo(39091);
        Assertions.assertThat(result.get(0).getSum()).isEqualTo(234546);
    }

    @Test
    public void testCollabReport() throws IOException, SQLException, ExecutionException {

        DbListResponse<DbAggregationResult> res = esScmPRsService.getStackedCollaborationReport(company, ScmPrFilter.builder().build(), null);
        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);

        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(3);
        Assertions.assertThat(result.get(0).getCollabState()).isEqualTo("unapproved");
        Assertions.assertThat(result.get(0).getStacks().get(0).getAdditionalKey()).isEqualTo("NONE");
        Assertions.assertThat(result.get(0).getStacks().get(0).getCount()).isEqualTo(3);
    }

    private static void insertData() throws IOException {

        String data = ResourceUtils.getResourceAsString("data/prs.json");
        List<EsScmPullRequest> allScmPRs = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, EsScmPullRequest.class));

        List<BulkOperation> bulkOperations = new ArrayList<>();
        for (EsScmPullRequest pr : allScmPRs) {
            BulkOperation.Builder b = new BulkOperation.Builder();
            b.update(v -> v
                    .index(index)
                    .id(pr.getId())
                    .action(a -> a
                            .docAsUpsert(true)
                            .doc(pr)
                    )
            );
            bulkOperations.add(b.build());
        }
        BulkRequest.Builder bldr = new BulkRequest.Builder();
        bldr.operations(bulkOperations);
        esContext.getClient().bulk(bldr.build());
    }

    // This is an ugly hack, the test needs to be rewritten to take dynamic
    // integration ids and use the IntegrationService to persist into the DB
    private static int insertIntegrationId(String company, String inegrationId) {
        String insertStmt = " INSERT INTO " + company + ".integrations AS u " +
                " (id, name, url, status, application)" +
                " VALUES (:id, :name, :url, 'active', 'github') ";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", Integer.parseInt(inegrationId));
        params.addValue("name", "name-" + inegrationId);
        params.addValue("url", "http://url-" + inegrationId);
        int updatedRows = template.update(insertStmt, params);
        return updatedRows;
    }

    private static void insertDBData() throws SQLException {
        insertIntegrationId(company, "1849");
        insertIntegrationId(company, "1861");

        DbScmUser dbScmUser = DbScmUser.builder()
                .id("a03fcd03-f166-4e93-9de1-58e93c2c79e8")
                .integrationId("1849")
                .cloudId("ctlo2020")
                .displayName("ctlo2020")
                .originalDisplayName("ctlo2020")
                .build();

        insertIntegrationUsers(company, dbScmUser);

        dbScmUser = DbScmUser.builder()
                .id("f1617a51-04ba-4010-9c11-028f5e15b943")
                .integrationId("1861")
                .cloudId("viraj-levelops")
                .displayName("viraj-levelops")
                .originalDisplayName("viraj-levelops")
                .build();

        insertIntegrationUsers(company, dbScmUser);

        dbScmUser = DbScmUser.builder()
                .id("14143532-db10-497d-b9ac-f4b7e8c9050e")
                .integrationId("1849")
                .cloudId("ashish-levelops")
                .displayName("ashish-levelops")
                .originalDisplayName("ashish-levelops")
                .build();

        insertIntegrationUsers(company, dbScmUser);
    }

    private static int insertIntegrationUsers(String company, DbScmUser dbScmUser) throws SQLException {

        String insertStmt = " INSERT INTO " + company + ".integration_users AS u " +
                " (id, integration_id, cloud_id, display_name,original_display_name, updated_at)" +
                " VALUES (:id, :integration_id, :cloud_id, :display_name,:original_display_name, EXTRACT(epoch FROM now())) ";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", UUID.fromString(dbScmUser.getId()));
        params.addValue("integration_id", NumberUtils.toInt(dbScmUser.getIntegrationId()));
        params.addValue("cloud_id", dbScmUser.getCloudId());
        params.addValue("display_name", dbScmUser.getDisplayName());
        params.addValue("original_display_name", dbScmUser.getOriginalDisplayName());

        int updatedRows = template.update(insertStmt, params);
        return updatedRows;
    }

    @SneakyThrows
    private static void createIndex() {

        String indexTemplate = ResourceUtils.getResourceAsString("index/prs_index_template.json");
        esContext.getClient().indices().create(new CreateIndexRequest.Builder()
                .index(index)
                .aliases(index + "_a", new Alias.Builder().build())
                .settings(new IndexSettings.Builder().numberOfShards("4").codec("best_compression").build())
                .mappings(TypeMapping.of(s -> s.withJson(new StringReader(indexTemplate))))
                .build());
    }

    @AfterClass
    public static void closeResources() throws Exception {
        esTestUtils.deleteIndex(index);
        esTestUtils.closeResources();
    }

}
