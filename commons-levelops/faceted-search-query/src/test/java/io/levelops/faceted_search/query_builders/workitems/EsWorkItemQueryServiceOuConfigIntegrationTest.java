package io.levelops.faceted_search.query_builders.workitems;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.*;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESClusterInfo;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.faceted_search.services.workitems.EsWorkItemsQueryService;
import io.levelops.ingestion.models.IntegrationType;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Before running this integration test, you have to run below-mentioned test class to make sure data is there in ES and DB
 * io.levelops.faceted_search.services.jira.EsWorkItemInsertionIntegrationTest
 */
public class EsWorkItemQueryServiceOuConfigIntegrationTest {
    private static final String company = "test";

    private static EsWorkItemsQueryService esWorkItemsQueryService;
    private static ESClientFactory esClientFactory;
    private static DataSource dataSource;
    private static OrgUnitHelper unitsHelper;
    private static OrgUnitsDatabaseService unitsService;
    private static Long ingestedAt;

    @BeforeClass
    public static void setup() throws GeneralSecurityException, IOException, SQLException {
        if (dataSource != null)
            return;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + "127.0.0.1" + "/postgres?");
        config.setUsername("postgres");
        config.setPassword("");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);

        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);

        IntegrationService integrationService = new IntegrationService(dataSource);
        TagItemDBService tagItemService = new TagItemDBService(dataSource);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, DefaultObjectMapper.get());
        OrgVersionsDatabaseService versionsService = new OrgVersionsDatabaseService(dataSource);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        OrgUsersDatabaseService usersService = new OrgUsersDatabaseService(dataSource, DefaultObjectMapper.get(), versionsService, userIdentityService);

        unitsService = new OrgUnitsDatabaseService(dataSource, DefaultObjectMapper.get(), tagItemService, usersService, versionsService, dashboardWidgetService);

        unitsHelper = new OrgUnitHelper(unitsService, integrationService);

        ingestedAt = 1647138501L;

        esClientFactory = new ESClientFactory(List.of(ESClusterInfo.builder()
                .name("CLUSTER_1")
                .ipAddresses(List.of("localhost"))
                .port(9200)
                .defaultCluster(true)
                .build()));
        OrgUsersDatabaseService orgUsersDatabaseService = new OrgUsersDatabaseService(dataSource, new ObjectMapper(), new OrgVersionsDatabaseService(dataSource), userIdentityService);
        esWorkItemsQueryService = new EsWorkItemsQueryService(esClientFactory, workItemFieldsMetaService, orgUsersDatabaseService);
    }

    @Test
    public void test() throws SQLException, IOException {

        Optional<DBOrgUnit> dbOrgUnit1 = unitsService.get(company, 1, true);
        DefaultListRequest defaultListRequest = DefaultListRequest.builder().ouIds(Set.of(1)).build();
        OUConfiguration ouConfig = unitsHelper.getOuConfigurationFromDBOrgUnit(company,
                IntegrationType.getIssueManagementIntegrationTypes(), defaultListRequest,
                dbOrgUnit1.orElseThrow(), false);

        DbListResponse<DbWorkItem> list = esWorkItemsQueryService.getWorkItemsList(company, WorkItemsFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .build(), WorkItemsMilestoneFilter.builder().build(), ouConfig.toBuilder().staticUsers(true).build(), 0, 10);

        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(list.getRecords().size()).isEqualTo(3);
        Assertions.assertThat(list.getRecords().stream().map(DbWorkItem::getAssignee).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("maxime", "maxime", "maxime") ;

    }
}
