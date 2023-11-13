package io.levelops.commons.databases.services.velocity;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.velocity.VelocityConfig;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.services.CiCdInstanceUtils;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobUtils;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationUtils;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.ProductUtils;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.UserUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class VelocityConfigsDatabaseServiceTest {
    private static final Long LOWER_LIMIT = TimeUnit.DAYS.toSeconds(10);
    private static final Long UPPER_LIMIT = TimeUnit.DAYS.toSeconds(30);

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static UserService userService;
    private static ProductService productService;
    private static IntegrationService integrationService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static String company = "test";
    private static VelocityConfigsDatabaseService velocityConfigsDatabaseService;
    private static OrgProfileDatabaseService orgProfileDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        productService = new ProductService(dataSource);
        integrationService = new IntegrationService(dataSource);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        orgProfileDatabaseService = new OrgProfileDatabaseService(dataSource, DefaultObjectMapper.get());
        velocityConfigsDatabaseService = new VelocityConfigsDatabaseService(dataSource, DefaultObjectMapper.get(), orgProfileDatabaseService);


        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        integrationService.ensureTableExistence(company);
        productService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        orgProfileDatabaseService.ensureTableExistence(company);
        velocityConfigsDatabaseService.ensureTableExistence(company);
    }

    //region Create Objects
    private static String getPrefix(int i) {
        return (i%2 ==0) ? "Config EvenPrefix " : "Config OddPrefix ";
    }
    private static String getSuffix(int i) {
        return (i%2 ==0) ? " EvenSuffix" : " OddSuffix";
    }
    private VelocityConfigDTO createConfigDto(List<UUID> cicdJobIds, int i) {
        List<String> cicdJobIdStrings = cicdJobIds.stream().map(x -> x.toString()).collect(Collectors.toList());
        String namePrefix = getPrefix(i);
        String nameSuffix = getSuffix(i);

        VelocityConfigDTO expected = VelocityConfigDTO.builder()
                .name(namePrefix + i + nameSuffix).description("description").defaultConfig(i==0 || i==5)
                .preDevelopmentCustomStages(List.of(
                        VelocityConfigDTO.Stage.builder()
                                .name("Backlog Time").description("stage description").order(0).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.JIRA_STATUS).values(List.of("IN_PROGRESS")).build())
                                .build()
                ))
                .fixedStages(List.of(
                        VelocityConfigDTO.Stage.builder()
                                .name("Lead time to first commit").description("stage description").order(0).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_COMMIT_CREATED).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Dev Time").description("stage description").order(1).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_CREATED).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Lead time to review").description("stage description").order(2).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_REVIEW_STARTED).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Review Time").description("stage description").order(3).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_APPROVED).params(Map.of("last_approval", List.of("true"))).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Merge Time").description("stage description").order(4).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.SCM_PR_MERGED).build())
                                .build()

                ))
                .postDevelopmentCustomStages(List.of(
                        VelocityConfigDTO.Stage.builder()
                                .name("Deploy to Staging").description("stage description").order(0).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.CICD_JOB_RUN).values(cicdJobIdStrings).params(Map.of("branch", List.of("dev","staging"))).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("QA").description("stage description").order(1).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.JIRA_STATUS).values(List.of("QA", "Testing")).build())
                                .build(),
                        VelocityConfigDTO.Stage.builder()
                                .name("Deploy to Prod").description("stage description").order(2).lowerLimitValue(LOWER_LIMIT).lowerLimitUnit(TimeUnit.SECONDS).upperLimitValue(UPPER_LIMIT).upperLimitUnit(TimeUnit.SECONDS)
                                .event(VelocityConfigDTO.Event.builder().type(VelocityConfigDTO.EventType.CICD_JOB_RUN).values(cicdJobIdStrings).params(Map.of("branch", List.of("main","master"))).build())
                                .build()


                ))
                .scmConfig(VelocityConfigDTO.ScmConfig.builder()
                        .defect(Map.of(VelocityConfigDTO.ScmConfig.Field.source_branch, Map.of("contains", List.of("def"))))
                        .deployment(Map.of(VelocityConfigDTO.ScmConfig.Field.source_branch, Map.of("contains", List.of("dep"))))
                        .release(Map.of(VelocityConfigDTO.ScmConfig.Field.target_branch, Map.of("contains", List.of("rel","re"))))
                        .hotfix(Map.of(VelocityConfigDTO.ScmConfig.Field.labels, Map.of("contains", List.of("hf"))))
                        .build())
                .build();
        return expected;
    }
    private VelocityConfig createConfig(VelocityConfigDTO velocityConfigDTO, List<UUID> cicdJobIds) {
        VelocityConfig expected = VelocityConfig.builder()
                .name(velocityConfigDTO.getName())
                .defaultConfig(velocityConfigDTO.getDefaultConfig())
                .config(velocityConfigDTO)
                .cicdJobIds(cicdJobIds)
                .build();
        return expected;
    }
    //endregion

    //region Test Insert
    private VelocityConfig testInsert(List<UUID> cicdJobIds, int i) throws SQLException {
        VelocityConfigDTO velocityConfigDTO = createConfigDto(cicdJobIds, i);
        VelocityConfig expected = createConfig(velocityConfigDTO, cicdJobIds);
        String id = velocityConfigsDatabaseService.insert(company, expected);
        Assert.assertNotNull(id);
        expected = expected.toBuilder().id(UUID.fromString(id)).build();
        return expected;
    }
    private List<VelocityConfig> testInserts(List<UUID> cicdJobIds, int n) throws SQLException {
        List<VelocityConfig> expected = new ArrayList<>();
        for(int i=0; i<n; i++) {
            VelocityConfig current = testInsert(cicdJobIds, i);
            testGet(current);
            expected.add(current);
        }
        return expected;
    }
    //endregion

    //region Test List
    private void testListByFiltersPartialMatch(List<VelocityConfig> allExpected) {
        List<VelocityConfig> evenExpected = new ArrayList<>();
        List<VelocityConfig> oddExpected = new ArrayList<>();
        for(int i=0; i< allExpected.size(); i++) {
            if(i%2 == 0) {
                evenExpected.add(allExpected.get(i));
            } else {
                oddExpected.add(allExpected.get(i));
            }
        }
        DbListResponse<VelocityConfig> result = velocityConfigsDatabaseService.listByFilter(company, 0, 100, null, null, null, Map.of("name", Map.of("$begins", "Config ")));
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        result = velocityConfigsDatabaseService.listByFilter(company, 0, 100, null, null, null, Map.of("name", Map.of("$begins", "Config EvenPrefix")));
        Assert.assertNotNull(result);
        Assert.assertEquals(evenExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(evenExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), evenExpected);

        result = velocityConfigsDatabaseService.listByFilter(company, 0, 100, null, null, null, Map.of("name", Map.of("$begins", "Even")));
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));

        result = velocityConfigsDatabaseService.listByFilter(company, 0, 100, null, null, null, Map.of("name", Map.of("$ends", "EvenSuffix")));
        Assert.assertNotNull(result);
        Assert.assertEquals(evenExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(evenExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), evenExpected);

        result = velocityConfigsDatabaseService.listByFilter(company, 0, 100, null, null, null, Map.of("name", Map.of("$contains", "Even")));
        Assert.assertNotNull(result);
        Assert.assertEquals(evenExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(evenExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), evenExpected);

        result = velocityConfigsDatabaseService.listByFilter(company, 0, 100, null, null, null, Map.of("name", Map.of("$contains", "Ev';--")));
        DefaultObjectMapper.prettyPrint(result);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertEquals(0, result.getCount().intValue());
    }
    private void testListByFiltersDefaultConfig(List<VelocityConfig> allExpected) {
        List<VelocityConfig> expected = allExpected.stream()
                .filter(VelocityConfig::getDefaultConfig)
                .collect(Collectors.toList());
        DbListResponse<VelocityConfig> result = velocityConfigsDatabaseService.listByFilter(company, 0, 100, null, null, true, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), expected);

        expected = allExpected.stream()
                .filter(c -> c.getDefaultConfig() == false)
                .collect(Collectors.toList());
        result = velocityConfigsDatabaseService.listByFilter(company, 0, 100, null, null, false, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), expected);
    }
    private void testListByFiltersNames(List<VelocityConfig> allExpected) {
        Map<String, List<VelocityConfig>> map = allExpected.stream().collect(Collectors.groupingBy(VelocityConfig::getName));
        for (String name : map.keySet()) {
            List<VelocityConfig> expected = map.get(name);
            DbListResponse<VelocityConfig> result = velocityConfigsDatabaseService.listByFilter(company, 0, 100, null, List.of(name), null,null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<String> allNames = allExpected.stream().map(VelocityConfig::getName).collect(Collectors.toList());
        DbListResponse<VelocityConfig> result = velocityConfigsDatabaseService.listByFilter(company, 0, 100, null, allNames, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }
    private void testListByFiltersIds(List<VelocityConfig> allExpected) {
        List<UUID> ids = new ArrayList<>();
        List<VelocityConfig> expected = new ArrayList<>();
        for(int i=0; i< allExpected.size(); i++) {
            if(i%2 == 0) {
                continue;
            }
            ids.add(allExpected.get(i).getId());
            expected.add(allExpected.get(i));
        }
        DbListResponse<VelocityConfig> result = velocityConfigsDatabaseService.listByFilter(company, 0, 100, ids, null, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), expected);
    }
    private void testList(List<VelocityConfig> expected) throws SQLException {
        DbListResponse<VelocityConfig> result = velocityConfigsDatabaseService.list(company, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        List<VelocityConfig> actual = result.getRecords();
        verifyRecords(actual, expected);

        result = velocityConfigsDatabaseService.list(company, 0, 2);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(2, result.getCount().intValue());
        actual = result.getRecords();
        verifyRecords(actual, expected.subList(expected.size() - 2, expected.size()));
    }
    private void testAllList(List<VelocityConfig> allExpected) throws SQLException {
        testList(allExpected);
        testListByFiltersIds(allExpected);
        testListByFiltersNames(allExpected);
        testListByFiltersDefaultConfig(allExpected);
        testListByFiltersPartialMatch(allExpected);
    }
    //endregion

    //region Test Get
    private void testGet(VelocityConfig allExpected) throws SQLException {
        Optional<VelocityConfig> result = velocityConfigsDatabaseService.get(company, allExpected.getId().toString());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isPresent());
        verifyRecord(result.get(), allExpected);
    }
    //endregion

    //region Test Update
    private VelocityConfig testUpdate(VelocityConfig actual, int i) throws SQLException {
        String prefix = getPrefix(100 + i);
        String suffix = getSuffix(100 + i);
        VelocityConfigDTO velocityConfigDTO = VelocityConfigDTO.builder()
                .name(prefix + (100 +i) + suffix).defaultConfig(actual.getDefaultConfig()).build();
        VelocityConfig updated = createConfig(velocityConfigDTO, List.of()).toBuilder().id(actual.getId()).build();

        velocityConfigsDatabaseService.update(company, updated);
        testGet(updated);
        return updated;
    }
    private List<VelocityConfig> testUpdates(List<VelocityConfig> allExpected) throws SQLException {
        List<VelocityConfig> allUpdated = new ArrayList<>();
        for(int i=0; i< allExpected.size(); i++) {
            VelocityConfig updated = testUpdate(allExpected.get(i), i);
            allUpdated.add(updated);
        }
        testList(allUpdated);
        return allUpdated;
    }
    //endregion

    //region Test Change Default
    private List<VelocityConfig> testChangeDefault(List<VelocityConfig> allExpected) throws SQLException {
        try {
            velocityConfigsDatabaseService.setDefault(company, UUID.randomUUID());
            Assert.fail("Exception expected");
        } catch (Exception e) {
            System.out.println(e);
        }
        VelocityConfig existingDefault = velocityConfigsDatabaseService.get(company, allExpected.get(0).getId().toString()).get();
        Assert.assertTrue(existingDefault.getDefaultConfig());


        List<VelocityConfig> allUpdated = new ArrayList<>();
        for(int i=0; i< allExpected.size(); i++) {
            if(i!=0 && i!=1) {
                allUpdated.add(allExpected.get(i));
            }
        }
        for(int i=0; i< allExpected.size(); i++) {
            if(i==0) {
                VelocityConfig updated = allExpected.get(i).toBuilder().defaultConfig(false).build();
                allUpdated.add(updated);
            } else if (i == 1) {
                VelocityConfig updated = allExpected.get(i).toBuilder().defaultConfig(true).build();
                allUpdated.add(updated);
            }
        }
        velocityConfigsDatabaseService.setDefault(company, allExpected.get(1).getId());
        testList(allUpdated);
        return allUpdated;
    }
    //endregion

    //region Test Delete
    private void testDelete(List<VelocityConfig> allExpected) throws SQLException {
        for(int i=0; i< allExpected.size(); i++){
            VelocityConfig current = allExpected.get(0);
            Boolean success = velocityConfigsDatabaseService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            allExpected.remove(0);
            testList(allExpected);
        }
        testList(allExpected);
    }
    //endregion

    //region Test Duplicate Default Config
    private void testDuplicateConfig(List<UUID> cicdJobIds) {
        try {
            testInsert(cicdJobIds, 5);
            Assert.fail("DuplicateKeyException expected");
        } catch (SQLException e) {
            Assert.fail("DuplicateKeyException expected");
        } catch (DuplicateKeyException e) {
            System.out.println(e);
        }
    }
    //endregion


    @Test
    public void test() throws SQLException {
        //region setup
        Integration integration = IntegrationUtils.createIntegration(integrationService, company, 0);
        User user = UserUtils.createUser(userService, company, 0);
        Product product = ProductUtils.createProduct(productService, company, 0, user);
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, integration,0);
        List<CICDJob> cicdJobs = CiCdJobUtils.createCICDJobs(ciCdJobsDatabaseService, company, cicdInstance,2);
        List<UUID> cicdJobIds = cicdJobs.stream().map(x -> x.getId()).collect(Collectors.toList());
        //endregion

        List<VelocityConfig> expected = testInserts(cicdJobIds, 5);
        testAllList(expected);
        expected = testUpdates(expected);
        expected = testChangeDefault(expected);
        testDuplicateConfig(cicdJobIds);
        testDelete(expected);
    }

    //region Verify
    private void verifyRecord(VelocityConfig a, VelocityConfig e) {
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getName(), e.getName());
        Assert.assertEquals(a.getDefaultConfig(), e.getDefaultConfig());
        Assert.assertEquals(a.getConfig(), e.getConfig());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }
    private void verifyRecords(List<VelocityConfig> a, List<VelocityConfig> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, VelocityConfig> actualMap = a.stream().collect(Collectors.toMap(VelocityConfig::getId, x -> x));
        Map<UUID, VelocityConfig> expectedMap = e.stream().collect(Collectors.toMap(VelocityConfig::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }
    //endregion
}