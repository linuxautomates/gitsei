package io.levelops.faceted_search.services.scm_service;

import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.Alias;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmContributorAgg;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmRepoAgg;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ScmReposFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESContext;
import io.levelops.commons.faceted_search.db.models.scm.EsScmCommit;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.SneakyThrows;
import org.apache.commons.lang3.math.NumberUtils;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

//@Ignore
public class EsScmCommitsServiceTest {

    private static ESContext esContext;
    private static EsTestUtils esTestUtils = new EsTestUtils();
    private static EsScmCommitsService esScmCommitsService;

    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;
    private static NamedParameterJdbcTemplate template;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String index = "scm_commits_test";
    public static String company = "test";

    @BeforeClass
    public static void startESCreateClient() throws IOException, InterruptedException, SQLException {

        esContext = esTestUtils.initializeESClient();
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        template = new NamedParameterJdbcTemplate(dataSource);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
//        ensureTableExistence(company);

        createIndex();
        insertData();
        insertDBData();

        ESClientFactory esClientFactory = EsTestUtils.buildESClientFactory(esContext);
        esScmCommitsService = new EsScmCommitsService(esClientFactory, dataSource, userIdentityService);
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
    }

    @Test
    public void testAggregations() throws IOException {

        ScmCommitFilter filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.project)
                .calculation(ScmCommitFilter.CALCULATION.count)
                .legacyCodeConfig(0L)
                .build();

        DbListResponse<DbAggregationResult> res = esScmCommitsService.groupByAndCalculateCommits(company, filter, false, null, 0, 10);

        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(3);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(5);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(74);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(88);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(162);
        Assertions.assertThat(result.get(0).getPctNewLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(0).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(0).getPctRefactoredLines()).isEqualTo(100.0);
        Assertions.assertThat(result.get(0).getAvgChangeSize()).isEqualTo(1.667f);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("levelops/api-levelops");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(12);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(233);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(93);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(326);
        Assertions.assertThat(result.get(1).getPctNewLines()).isEqualTo(88.96);
        Assertions.assertThat(result.get(1).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(1).getPctRefactoredLines()).isEqualTo(11.04);
        Assertions.assertThat(result.get(1).getAvgChangeSize()).isEqualTo(6.0f);

        Assertions.assertThat(result.get(2).getKey()).isEqualTo("depot");
        Assertions.assertThat(result.get(2).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(2).getFilesChangedCount()).isEqualTo(1);
        Assertions.assertThat(result.get(2).getLinesAddedCount()).isEqualTo(27);
        Assertions.assertThat(result.get(2).getLinesRemovedCount()).isEqualTo(0);
        Assertions.assertThat(result.get(2).getLinesChangedCount()).isEqualTo(0);
        Assertions.assertThat(result.get(2).getPctNewLines()).isEqualTo(100.0);
        Assertions.assertThat(result.get(2).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(2).getPctRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(2).getAvgChangeSize()).isEqualTo(1.0f);

        filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.author)
                .calculation(ScmCommitFilter.CALCULATION.count)
                .legacyCodeConfig(0L)
                .build();

        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, false, null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);
        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(5);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(8);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(79);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(71);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(150);
        Assertions.assertThat(result.get(0).getPctNewLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(0).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(0).getPctRefactoredLines()).isEqualTo(100.0);
        Assertions.assertThat(result.get(0).getAvgChangeSize()).isEqualTo(1.6f);

        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(14);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(259);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(133);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(392);
        Assertions.assertThat(result.get(1).getPctNewLines()).isEqualTo(73.98);
        Assertions.assertThat(result.get(1).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(1).getPctRefactoredLines()).isEqualTo(26.02);
        Assertions.assertThat(result.get(1).getAvgChangeSize()).isEqualTo(7.0f);

        filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.technology)
                .calculation(ScmCommitFilter.CALCULATION.count)
                .legacyCodeConfig(0L)
                .build();

        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, false, null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("Java");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(7);
        Assertions.assertThat(result.get(1).getKey()).isEqualTo("Dockerfile");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(3);
        Assertions.assertThat(result.get(2).getKey()).isEqualTo("HTML");
        Assertions.assertThat(result.get(2).getCount()).isEqualTo(1);

        filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.trend)
                .aggInterval(AGG_INTERVAL.month)
                .calculation(ScmCommitFilter.CALCULATION.count)
                .legacyCodeConfig(0L)
                .build();

        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, false, null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("1643673600");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(3);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(15);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(286);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(133);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(392);
        Assertions.assertThat(result.get(0).getPctNewLines()).isEqualTo(74.85);
        Assertions.assertThat(result.get(0).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(0).getPctRefactoredLines()).isEqualTo(25.15);
        Assertions.assertThat(result.get(0).getAvgChangeSize()).isEqualTo(5.0f);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("1651363200");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(5);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(8);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(79);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(71);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(150);
        Assertions.assertThat(result.get(1).getPctNewLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(1).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(1).getPctRefactoredLines()).isEqualTo(100.0);
        Assertions.assertThat(result.get(1).getAvgChangeSize()).isEqualTo(1.6f);

        filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.code_change)
                .calculation(ScmCommitFilter.CALCULATION.count)
                .legacyCodeConfig(0L)
                .build();

        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, false, null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("small");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(7);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(12);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(133);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(112);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(218);
        Assertions.assertThat(result.get(0).getPctNewLines()).isEqualTo(5.83);
        Assertions.assertThat(result.get(0).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(0).getPctRefactoredLines()).isEqualTo(94.17);
        Assertions.assertThat(result.get(0).getAvgChangeSize()).isEqualTo(66.143f);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("medium");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(11);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(232);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(92);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(324);
        Assertions.assertThat(result.get(1).getPctNewLines()).isEqualTo(89.51);
        Assertions.assertThat(result.get(1).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(1).getPctRefactoredLines()).isEqualTo(10.49);
        Assertions.assertThat(result.get(1).getAvgChangeSize()).isEqualTo(648.0f);

        filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.code_category)
                .calculation(ScmCommitFilter.CALCULATION.count)
                .legacyCodeConfig(0L)
                .build();

        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, false, null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);

        Assertions.assertThat(result.get(0).getKey()).isEqualTo("new_lines");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(11);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(236);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(81);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(290);
        Assertions.assertThat(result.get(0).getPctNewLines()).isEqualTo(100.0);
        Assertions.assertThat(result.get(0).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(0).getPctRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(0).getAvgChangeSize()).isEqualTo(303.5f);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("refactored_lines");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(7);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(12);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(129);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(123);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(252);
        Assertions.assertThat(result.get(1).getPctNewLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(1).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(1).getPctRefactoredLines()).isEqualTo(100.0);
        Assertions.assertThat(result.get(1).getAvgChangeSize()).isEqualTo(72.0f);

        filter = ScmCommitFilter.builder().across(ScmCommitFilter.DISTINCT.commit_branch).build();
        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(7);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1861"))
                .across(ScmCommitFilter.DISTINCT.commit_branch)
                .commitBranches(List.of("PROP-476"))
                .build();
        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .commitBranches(List.of("main"))
                .across(ScmCommitFilter.DISTINCT.commit_branch)
                .build();
        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .excludeCommitBranches(List.of("main"))
                .across(ScmCommitFilter.DISTINCT.commit_branch)
                .build();
        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .across(ScmCommitFilter.DISTINCT.commit_branch)
                .partialMatch(Map.of(String.valueOf(ScmCommitFilter.DISTINCT.commit_branch), Map.of("$begins", "LEV")))
                .build();
        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .across(ScmCommitFilter.DISTINCT.commit_branch)
                .partialMatch(Map.of(String.valueOf(ScmCommitFilter.DISTINCT.commit_branch), Map.of("$contains", "LEV")))
                .build();
        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .across(ScmCommitFilter.DISTINCT.commit_branch)
                .partialMatch(Map.of(String.valueOf(ScmCommitFilter.DISTINCT.commit_branch), Map.of("$contains", "ain")))
                .build();
        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .across(ScmCommitFilter.DISTINCT.commit_branch)
                .partialMatch(Map.of(String.valueOf(ScmCommitFilter.DISTINCT.commit_branch), Map.of("$contains", "ev")))
                .build();
        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(0);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .across(ScmCommitFilter.DISTINCT.commit_branch)
                .partialMatch(Map.of(String.valueOf(ScmCommitFilter.DISTINCT.commit_branch), Map.of("$contains", "LEV")))
                .build();
        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .across(ScmCommitFilter.DISTINCT.commit_branch)
                .partialMatch(Map.of(String.valueOf(ScmCommitFilter.DISTINCT.commit_branch), Map.of("$contains", "main")))
                .build();
        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);

        filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.commit_branch)
                .commitBranches(List.of("main"))
                .calculation(ScmCommitFilter.CALCULATION.count)
                .legacyCodeConfig(0L)
                .build();
        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, false, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.get(0).getKey()).isEqualTo("main");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(14);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(259);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(133);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(392);
        Assertions.assertThat(result.get(0).getPctNewLines()).isEqualTo(73.98);
        Assertions.assertThat(result.get(0).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(0).getPctRefactoredLines()).isEqualTo(26.02);
        Assertions.assertThat(result.get(0).getAvgChangeSize()).isEqualTo(7.0f);
    }

    @Test
    public void testListCommits() throws IOException {

        ScmCommitFilter filter = ScmCommitFilter.builder()
                .projects(List.of("levelops/commons-levelops", "levelops/api-levelops"))
                .authors(List.of("9b530651-3062-4097-9465-de20b5241408"))
                .build();
        List<DbScmCommit> list = esScmCommitsService.listCommits(company, filter, Map.of(), null, 0, 10).getRecords();

        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(2);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1861"))
                .repoIds(List.of("levelops/api-levelops", "depot"))
                .build();
        list = esScmCommitsService.listCommits(company, filter, Map.of(), null, 0, 10).getRecords();
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(2);

        filter = ScmCommitFilter.builder().build();
        list = esScmCommitsService.listCommits(company, filter, Map.of(), null, 0, 10).getRecords();
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(8);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1861"))
                .commitBranches(List.of("PROP-476"))
                .build();
        list = esScmCommitsService.listCommits(company, filter, Map.of(), null, 0, 10).getRecords();
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(1);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .commitBranches(List.of("main"))
                .build();
        list = esScmCommitsService.listCommits(company, filter, Map.of(), null, 0, 10).getRecords();
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(2);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .excludeCommitBranches(List.of("main"))
                .build();
        list = esScmCommitsService.listCommits(company, filter, Map.of(), null, 0, 10).getRecords();
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(5);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .partialMatch(Map.of(String.valueOf(ScmCommitFilter.DISTINCT.commit_branch), Map.of("$begins", "LEV")))
                .build();
        list = esScmCommitsService.listCommits(company, filter, Map.of(), null, 0, 10).getRecords();
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(5);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .partialMatch(Map.of(String.valueOf(ScmCommitFilter.DISTINCT.commit_branch), Map.of("$contains", "LEV")))
                .build();
        list = esScmCommitsService.listCommits(company, filter, Map.of(), null, 0, 10).getRecords();
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(5);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .partialMatch(Map.of(String.valueOf(ScmCommitFilter.DISTINCT.commit_branch), Map.of("$contains", "ain")))
                .build();
        list = esScmCommitsService.listCommits(company, filter, Map.of(), null, 0, 10).getRecords();
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(2);

        filter = ScmCommitFilter.builder()
                .integrationIds(List.of("2228", "1815"))
                .partialMatch(Map.of(String.valueOf(ScmCommitFilter.DISTINCT.commit_branch), Map.of("$contains", "ev")))
                .build();
        list = esScmCommitsService.listCommits(company, filter, Map.of(), null, 0, 10).getRecords();
        Assert.assertNotNull(list);
        Assertions.assertThat(list.size()).isEqualTo(0);
    }

    @Test
    public void testValues() throws IOException {

        ScmCommitFilter filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.author)
                .build();

        DbListResponse<DbAggregationResult> res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);

        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);
        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(5);
        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(2).getAdditionalKey()).isEqualTo("srnthc");
        Assertions.assertThat(result.get(2).getCount()).isEqualTo(1);

        filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.project)
                .build();

        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);
        Assertions.assertThat(result.get(0).getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(3);
        Assertions.assertThat(result.get(1).getKey()).isEqualTo("levelops/api-levelops");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(2).getKey()).isEqualTo("depot");
        Assertions.assertThat(result.get(2).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(3).getKey()).isEqualTo("levelops/faceted-search");
        Assertions.assertThat(result.get(3).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(4).getKey()).isEqualTo("levelops/integrations-levelops");
        Assertions.assertThat(result.get(4).getCount()).isEqualTo(1);

        filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.file_type)
                .build();

        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);

        Assertions.assertThat(result.size()).isEqualTo(3);
        Assertions.assertThat(result.get(0).getKey()).isEqualTo("java");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(6);
        Assertions.assertThat(result.get(1).getKey()).isEqualTo("gradle");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(2).getKey()).isEqualTo("txt");
        Assertions.assertThat(result.get(2).getCount()).isEqualTo(1);

        filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.commit_branch)
                .build();

        res = esScmCommitsService.groupByAndCalculateCommits(company, filter, true, null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(7);
        Assertions.assertThat(result.get(0).getKey()).isEqualTo("main");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getKey()).isEqualTo("LEV-5041,5042");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(2).getKey()).isEqualTo("LEV-5196-fix");
        Assertions.assertThat(result.get(2).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(3).getKey()).isEqualTo("LEV-5219-Phase-3");
        Assertions.assertThat(result.get(3).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(4).getKey()).isEqualTo("LEV-5272");
        Assertions.assertThat(result.get(4).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(5).getKey()).isEqualTo("LEV-5280");
        Assertions.assertThat(result.get(5).getCount()).isEqualTo(1);
        Assertions.assertThat(result.get(6).getKey()).isEqualTo("PROP-476");
        Assertions.assertThat(result.get(6).getCount()).isEqualTo(1);
    }

    @Test
    public void testStackData() throws IOException {

        ScmCommitFilter filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.author)
                .calculation(ScmCommitFilter.CALCULATION.count)
                .legacyCodeConfig(0L)
                .build();

        DbListResponse<DbAggregationResult> res = esScmCommitsService.stackedCommitsGroupBy(company, filter, List.of(ScmCommitFilter.DISTINCT.project), null);

        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);
        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(5);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(8);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(79);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(71);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(150);
        Assertions.assertThat(result.get(0).getPctNewLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(0).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(0).getPctRefactoredLines()).isEqualTo(100.0);
        Assertions.assertThat(result.get(0).getAvgChangeSize()).isEqualTo(1.6f);
        List<DbAggregationResult> stacks = result.get(0).getStacks();
        Assertions.assertThat(stacks.size()).isEqualTo(4);
        Assertions.assertThat(stacks.get(0).getKey()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(stacks.get(0).getCount()).isEqualTo(2);
        Assertions.assertThat(stacks.get(0).getFilesChangedCount()).isEqualTo(2);
        Assertions.assertThat(stacks.get(0).getLinesAddedCount()).isEqualTo(47);
        Assertions.assertThat(stacks.get(0).getLinesRemovedCount()).isEqualTo(47);
        Assertions.assertThat(stacks.get(0).getLinesChangedCount()).isEqualTo(94);

        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getFilesChangedCount()).isEqualTo(14);
        Assertions.assertThat(result.get(1).getLinesAddedCount()).isEqualTo(259);
        Assertions.assertThat(result.get(1).getLinesRemovedCount()).isEqualTo(133);
        Assertions.assertThat(result.get(1).getLinesChangedCount()).isEqualTo(392);
        Assertions.assertThat(result.get(1).getPctNewLines()).isEqualTo(73.98);
        Assertions.assertThat(result.get(1).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(1).getPctRefactoredLines()).isEqualTo(26.02);
        Assertions.assertThat(result.get(1).getAvgChangeSize()).isEqualTo(7.0f);
        stacks = result.get(1).getStacks();
        Assertions.assertThat(stacks.size()).isEqualTo(2);
        Assertions.assertThat(stacks.get(0).getKey()).isEqualTo("levelops/api-levelops");
        Assertions.assertThat(stacks.get(0).getCount()).isEqualTo(1);
        Assertions.assertThat(stacks.get(0).getFilesChangedCount()).isEqualTo(11);
        Assertions.assertThat(stacks.get(0).getLinesAddedCount()).isEqualTo(232);
        Assertions.assertThat(stacks.get(0).getLinesRemovedCount()).isEqualTo(92);
        Assertions.assertThat(stacks.get(0).getLinesChangedCount()).isEqualTo(324);

        filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.commit_branch)
                .commitBranches(List.of("main"))
                .calculation(ScmCommitFilter.CALCULATION.count)
                .legacyCodeConfig(0L)
                .codeChangeUnit("files")
                .build();

        res = esScmCommitsService.stackedCommitsGroupBy(company, filter, List.of(ScmCommitFilter.DISTINCT.code_category), null);
        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);
        Assertions.assertThat(result.get(0).getKey()).isEqualTo("main");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(2);
        Assertions.assertThat(result.get(0).getFilesChangedCount()).isEqualTo(14);
        Assertions.assertThat(result.get(0).getLinesAddedCount()).isEqualTo(259);
        Assertions.assertThat(result.get(0).getLinesRemovedCount()).isEqualTo(133);
        Assertions.assertThat(result.get(0).getLinesChangedCount()).isEqualTo(392);
        Assertions.assertThat(result.get(0).getPctNewLines()).isEqualTo(73.98);
        Assertions.assertThat(result.get(0).getPctLegacyRefactoredLines()).isEqualTo(0.0);
        Assertions.assertThat(result.get(0).getPctRefactoredLines()).isEqualTo(26.02);
        Assertions.assertThat(result.get(0).getAvgChangeSize()).isEqualTo(7.0f);

        stacks = result.get(0).getStacks();

        Assertions.assertThat(stacks.get(0).getKey()).isEqualTo("new_lines");
        Assertions.assertThat(stacks.get(0).getCount()).isEqualTo(1);
        Assertions.assertThat(stacks.get(0).getFilesChangedCount()).isEqualTo(10);
        Assertions.assertThat(stacks.get(0).getLinesAddedCount()).isEqualTo(209);
        Assertions.assertThat(stacks.get(0).getLinesRemovedCount()).isEqualTo(81);
        Assertions.assertThat(stacks.get(0).getLinesChangedCount()).isEqualTo(290);

        Assertions.assertThat(stacks.get(1).getKey()).isEqualTo("refactored_lines");
        Assertions.assertThat(stacks.get(1).getCount()).isEqualTo(2);
        Assertions.assertThat(stacks.get(1).getFilesChangedCount()).isEqualTo(4);
        Assertions.assertThat(stacks.get(1).getLinesAddedCount()).isEqualTo(50);
        Assertions.assertThat(stacks.get(1).getLinesRemovedCount()).isEqualTo(52);
        Assertions.assertThat(stacks.get(1).getLinesChangedCount()).isEqualTo(102);
    }

    @Test
    public void testCodingDaysReport() throws IOException {

        ScmCommitFilter filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.author)
                .calculation(ScmCommitFilter.CALCULATION.commit_days)
                .build();

        DbListResponse<DbAggregationResult> res = esScmCommitsService.groupByAndCalculateCodingDays(company, filter, null);

        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);
        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(0).getCommitSize()).isEqualTo(300);
        Assertions.assertThat(result.get(0).getMean()).isEqualTo(4.67);
        Assertions.assertThat(result.get(0).getMedian()).isEqualTo(10);

        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(1).getCommitSize()).isEqualTo(784);
        Assertions.assertThat(result.get(1).getMean()).isEqualTo(2.33);
        Assertions.assertThat(result.get(1).getMedian()).isEqualTo(23);

        Assertions.assertThat(result.get(2).getAdditionalKey()).isEqualTo("srnthc");
        Assertions.assertThat(result.get(2).getCommitSize()).isEqualTo(27);
        Assertions.assertThat(result.get(2).getMean()).isEqualTo(1.17);
        Assertions.assertThat(result.get(2).getMedian()).isEqualTo(27);

    }

    @Test
    public void testCommitsPerCodingDays() throws IOException {

        ScmCommitFilter filter = ScmCommitFilter.builder()
                .across(ScmCommitFilter.DISTINCT.author)
                .calculation(ScmCommitFilter.CALCULATION.commit_count)
                .build();

        DbListResponse<DbAggregationResult> res = esScmCommitsService.groupByAndCalculateCodingDays(company, filter, null);

        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);
        Assertions.assertThat(result.get(0).getAdditionalKey()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(0).getDayOfWeek()).isEqualTo("Thursday");
        Assertions.assertThat(result.get(0).getMean()).isEqualTo(3.5);
        Assertions.assertThat(result.get(0).getCommitSize()).isEqualTo(112);
        Assertions.assertThat(result.get(1).getAdditionalKey()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(1).getDayOfWeek()).isEqualTo("Tuesday");
        Assertions.assertThat(result.get(1).getMean()).isEqualTo(2.333333333333333);
        Assertions.assertThat(result.get(1).getCommitSize()).isEqualTo(188);

        Assertions.assertThat(result.get(2).getAdditionalKey()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(2).getDayOfWeek()).isEqualTo("Friday");
        Assertions.assertThat(result.get(2).getMean()).isEqualTo(1.1666666666666665);
        Assertions.assertThat(result.get(2).getCommitSize()).isEqualTo(648);
        Assertions.assertThat(result.get(3).getAdditionalKey()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(3).getDayOfWeek()).isEqualTo("Monday");
        Assertions.assertThat(result.get(3).getMean()).isEqualTo(1.1666666666666665);
        Assertions.assertThat(result.get(3).getCommitSize()).isEqualTo(136);

    }

    @Test
    public void testListModules() throws IOException {

        ScmFilesFilter filter = ScmFilesFilter.builder()
                .across(ScmFilesFilter.DISTINCT.repo_id)
                .build();
        DbListResponse<DbAggregationResult> res = esScmCommitsService.listModules(company, filter, Map.of(), 0, 10);

        Assert.assertNotNull(res);
        List<DbAggregationResult> result = res.getRecords();
        Assert.assertNotNull(result);

        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(6);
        Assertions.assertThat(result.get(0).getKey()).isEqualTo("server_api");
        Assertions.assertThat(result.get(0).getRepoId()).isEqualTo("levelops/api-levelops");
        Assertions.assertThat(result.get(0).getProject()).isEqualTo("levelops/api-levelops");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(10);

        Assertions.assertThat(result.get(1).getKey()).isEqualTo("database-services");
        Assertions.assertThat(result.get(1).getRepoId()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(1).getProject()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(1).getCount()).isEqualTo(5);

        filter = ScmFilesFilter.builder()
                .across(ScmFilesFilter.DISTINCT.repo_id)
                .module("database-services/src")
                .build();
        res = esScmCommitsService.listModules(company, filter, Map.of(), 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);

        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(1);
        Assertions.assertThat(result.get(0).getKey()).isEqualTo("main");
        Assertions.assertThat(result.get(0).getRepoId()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(0).getProject()).isEqualTo("levelops/commons-levelops");
        Assertions.assertThat(result.get(0).getCount()).isEqualTo(5);

    }

    @Test
    public void testListFiles() throws IOException {

        ScmFilesFilter filter = ScmFilesFilter.builder()
                .repoIds(List.of("levelops/commons-levelops"))
                .build();
        DbListResponse<DbScmFile> res = esScmCommitsService.listFile(company, filter, Map.of(), 0, 10);

        Assert.assertNotNull(res);
        List<DbScmFile> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(5);
        Assertions.assertThat(result.get(0).getIntegrationId()).isEqualTo("1815");
        Assertions.assertThat(result.get(0).getFilename()).isEqualTo("database-services/src/main/java/io/levelops/commons/databases/services/SonarQubeAggService.java");
        Assertions.assertThat(result.get(0).getCreatedAt()).isEqualTo(1646099996);

        Assertions.assertThat(result.get(1).getIntegrationId()).isEqualTo("1815");
        Assertions.assertThat(result.get(1).getFilename()).isEqualTo("database-services/src/main/java/io/levelops/commons/databases/services/JiraIssueService.java");
        Assertions.assertThat(result.get(1).getCreatedAt()).isEqualTo(1646099696);
        filter = ScmFilesFilter.builder()
                .repoIds(List.of("levelops/api-levelops"))
                .build();

        res = esScmCommitsService.listFile(company, filter, Map.of(), 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(10);
        Assertions.assertThat(result.get(0).getIntegrationId()).isEqualTo("1815");
        Assertions.assertThat(result.get(0).getFilename()).isEqualTo("server_api/src/main/java/io/levelops/api/services/dev_productivity/OrgDevProductivityReportService.java");
        Assertions.assertThat(result.get(0).getCreatedAt()).isEqualTo(1646099700);

        Assertions.assertThat(result.get(1).getIntegrationId()).isEqualTo("1815");
        Assertions.assertThat(result.get(1).getFilename()).isEqualTo("server_api/src/main/java/io/levelops/api/controllers/PagerDutyAggregationsController.java");
        Assertions.assertThat(result.get(1).getCreatedAt()).isEqualTo(1646188777);
    }

    @Test
    public void testFileTypeList() throws IOException {

        ScmReposFilter filter = ScmReposFilter.builder()
                .build();

        DbListResponse<DbScmRepoAgg> res = esScmCommitsService.listFileTypes(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        List<DbScmRepoAgg> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(res.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getName()).isEqualTo("java");
        Assertions.assertThat(result.get(0).getNumCommits()).isEqualTo(6);
        Assertions.assertThat(result.get(0).getNumAdditions()).isEqualTo(337);
        Assertions.assertThat(result.get(0).getNumDeletions()).isEqualTo(203);
        Assertions.assertThat(result.get(0).getNumChanges()).isEqualTo(540);
        Assertions.assertThat(result.get(0).getNumWorkitems()).isEqualTo(7);

        Assertions.assertThat(result.get(1).getName()).isEqualTo("gradle");
        Assertions.assertThat(result.get(1).getNumCommits()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getNumAdditions()).isEqualTo(233);
        Assertions.assertThat(result.get(1).getNumDeletions()).isEqualTo(93);
        Assertions.assertThat(result.get(1).getNumChanges()).isEqualTo(326);
        Assertions.assertThat(result.get(1).getNumWorkitems()).isEqualTo(2);

        Assertions.assertThat(result.get(0).getNumPrs()).isEqualTo(0);
        Assertions.assertThat(result.get(1).getNumPrs()).isEqualTo(0);
        Assertions.assertThat(result.get(2).getNumPrs()).isEqualTo(0);

        res = esScmCommitsService.listFileTypes(company, filter, Map.of("num_prs", SortingOrder.ASC), null, 0, 10);

        Assert.assertNotNull(res);
        result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getNumPrs()).isEqualTo(0);
        Assertions.assertThat(result.get(1).getNumPrs()).isEqualTo(0);
        Assertions.assertThat(result.get(2).getNumPrs()).isEqualTo(0);

    }

    @Test
    public void testContributorsList() throws IOException {

        ScmContributorsFilter filter = ScmContributorsFilter.builder()
                .across(ScmContributorsFilter.DISTINCT.author)
                .build();

        DbListResponse<DbScmContributorAgg> res = esScmCommitsService.list(company, filter, Map.of(), null, 0, 10);

        Assert.assertNotNull(res);
        List<DbScmContributorAgg> result = res.getRecords();
        Assert.assertNotNull(result);
        Assertions.assertThat(result.size()).isEqualTo(3);

        Assertions.assertThat(result.get(0).getName()).isEqualTo("ctlo2020");
        Assertions.assertThat(result.get(0).getNumCommits()).isEqualTo(5);
        Assertions.assertThat(result.get(0).getNumRepos()).isEqualTo(4);
        Assertions.assertThat(result.get(0).getNumAdditions()).isEqualTo(79);
        Assertions.assertThat(result.get(0).getNumDeletions()).isEqualTo(71);
        Assertions.assertThat(result.get(0).getNumChanges()).isEqualTo(150);
        Assertions.assertThat(result.get(0).getNumWorkitems()).isEqualTo(6);
        Assertions.assertThat(result.get(0).getRepoBreadth().size()).isEqualTo(4);
        Assertions.assertThat(result.get(0).getNumPrs()).isEqualTo(0);

        Assertions.assertThat(result.get(1).getName()).isEqualTo("ashish-levelops");
        Assertions.assertThat(result.get(1).getNumCommits()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getNumRepos()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getNumAdditions()).isEqualTo(259);
        Assertions.assertThat(result.get(1).getNumDeletions()).isEqualTo(133);
        Assertions.assertThat(result.get(1).getNumChanges()).isEqualTo(392);
        Assertions.assertThat(result.get(1).getNumWorkitems()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getRepoBreadth().size()).isEqualTo(2);
        Assertions.assertThat(result.get(1).getNumPrs()).isEqualTo(0);
    }


    private static void insertData() throws IOException {

        String data = ResourceUtils.getResourceAsString("data/commits.json");
        List<EsScmCommit> allScomCommits = MAPPER.readValue(data, MAPPER.getTypeFactory().constructCollectionType(List.class, EsScmCommit.class));

        List<BulkOperation> bulkOperations = new ArrayList<>();
        for (EsScmCommit commit : allScomCommits) {
            BulkOperation.Builder b = new BulkOperation.Builder();
            b.update(v -> v
                    .index(index)
                    .id(commit.getId())
                    .action(a -> a
                            .docAsUpsert(true)
                            .doc(commit)
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
        insertIntegrationId(company, "2228");
        insertIntegrationId(company, "1861");
        insertIntegrationId(company, "1815");
        DbScmUser dbScmUser = DbScmUser.builder()
                .id("ab986f2d-6f05-4653-89b8-7e094ebf966c")
                .integrationId("2228")
                .cloudId("ctlo2020")
                .displayName("ctlo2020")
                .originalDisplayName("ctlo2020")
                .build();
        insertIntegrationUsers(company, dbScmUser);

        dbScmUser = DbScmUser.builder()
                .id("32d2cedc-edaf-466f-a962-7723f94915f1")
                .integrationId("1861")
                .cloudId("srnthc")
                .displayName("srnthc")
                .originalDisplayName("srnthc")
                .build();
        insertIntegrationUsers(company, dbScmUser);

        dbScmUser = DbScmUser.builder()
                .id("9b530651-3062-4097-9465-de20b5241408")
                .integrationId("1815")
                .cloudId("ashish-levelops")
                .displayName("ashish-levelops")
                .originalDisplayName("srnthc")
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

        String indexTemplate = ResourceUtils.getResourceAsString("index/commits_index_template.json");
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
