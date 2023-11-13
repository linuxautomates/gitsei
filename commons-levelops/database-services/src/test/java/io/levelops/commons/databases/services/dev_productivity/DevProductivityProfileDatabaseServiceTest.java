package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.services.*;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DevProductivityProfileDatabaseServiceTest {
    private static final Integer SECTION_ORDER = 0;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static UserService userService;
    private static ProductService productService;
    private static IntegrationService integrationService;
    private static String company = "test";
    private static String schemeId1;
    private static String schemeId2;
    private static String categoryId1;
    private static String categoryId2;
    private static String categoryId3;
    private static String categoryId4;
    private static DevProductivityProfileDatabaseService devProductivityProfileDatabaseService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private static ObjectMapper MAPPER  = DefaultObjectMapper.get();

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userService = new UserService(dataSource, MAPPER);
        productService = new ProductService(dataSource);
        integrationService = new IntegrationService(dataSource);
        devProductivityProfileDatabaseService = new DevProductivityProfileDatabaseService(dataSource, MAPPER);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource,MAPPER);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        integrationService.ensureTableExistence(company);
        productService.ensureTableExistence(company);
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(company);
        devProductivityProfileDatabaseService.ensureTableExistence(company);

        schemeId1 = ticketCategorizationSchemeDatabaseService.insert("test", TicketCategorizationScheme.builder()
                .name("scheme-5")
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .description("desc scheme-5")
                        .integrationType("int-type")
                        .categories(Map.of("1", TicketCategorizationScheme.TicketCategorization.builder()
                                .index(1)
                                .name("category 1")
                                .description("desc cat1")
                                .filter(Map.of("test1", "filter1"))
                                .build(),
                                "2",TicketCategorizationScheme.TicketCategorization.builder()
                                .index(2)
                                .name("category 2")
                                .description("desc cat2")
                                .filter(Map.of("test2", "filter2"))
                                .build(),
                                "3",TicketCategorizationScheme.TicketCategorization.builder()
                                        .index(3)
                                        .name("category 3")
                                        .description("desc cat3")
                                        .filter(Map.of("test3", "filter3"))
                                        .build()))
                        .build())
                .build());
        schemeId2 = ticketCategorizationSchemeDatabaseService.insert("test", TicketCategorizationScheme.builder()
                .name("scheme-2")
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .description("desc scheme-2")
                        .integrationType("int-type")
                        .categories(Map.of("1", TicketCategorizationScheme.TicketCategorization.builder()
                                        .index(1)
                                        .name("category 1")
                                        .description("desc cat1")
                                        .filter(Map.of("test1", "filter1"))
                                        .build(),
                                "2",TicketCategorizationScheme.TicketCategorization.builder()
                                        .index(2)
                                        .name("category 2")
                                        .description("desc cat2")
                                        .filter(Map.of("test2", "filter2"))
                                        .build(),
                                "3",TicketCategorizationScheme.TicketCategorization.builder()
                                        .index(3)
                                        .name("category 3")
                                        .description("desc cat3")
                                        .filter(Map.of("test3", "filter3"))
                                        .build()))
                        .build())
                .build());
        TicketCategorizationScheme scheme = ticketCategorizationSchemeDatabaseService.get("test",schemeId1).get();
        categoryId1 = scheme.getConfig().getCategories().get("1").getId();
        categoryId2 = scheme.getConfig().getCategories().get("2").getId();
        categoryId3 = scheme.getConfig().getCategories().get("3").getId();

    }

    @Test
    public void test() throws SQLException {
        List<DevProductivityProfile> expected = testInserts(5);
        List<DevProductivityProfile> predefinedProfiles = devProductivityProfileDatabaseService.listByFilter(company,0,10,null,List.of("Software Developer Profile","SRE Profile"),null,null).getRecords();
        expected.addAll(predefinedProfiles);
        testAllList(expected);
        expected = testUpdates(expected);
        expected = testChangeDefault(expected);
        testDelete(expected);
    }

    private List<DevProductivityProfile> testInserts(int n) throws SQLException {
        List<DevProductivityProfile> expected = new ArrayList<>();
        for(int i=1; i<=n; i++) {
            DevProductivityProfile current = testInsert(i);
            testGet(current);
            expected.add(current);
        }
        return expected;
    }

    private DevProductivityProfile testInsert(int i) throws SQLException {
        DevProductivityProfile devProductivityProfile = createDevProductivityProfile(i);
        String id = devProductivityProfileDatabaseService.insert(company, devProductivityProfile);
        Assert.assertNotNull(id);
        devProductivityProfile = devProductivityProfile.toBuilder().id(UUID.fromString(id)).build();
        return devProductivityProfile;
    }

    private void testList(List<DevProductivityProfile> expected) throws SQLException {
        DbListResponse<DevProductivityProfile> result = devProductivityProfileDatabaseService.list(company, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        List<DevProductivityProfile> actual = result.getRecords();
        verifyRecords(actual, expected);

        result = devProductivityProfileDatabaseService.list(company, 0, 2);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(2, result.getCount().intValue());
        actual = result.getRecords();
        verifyRecords(actual, expected.subList(expected.size() - 4, expected.size()-2));
    }
    private void testAllList(List<DevProductivityProfile> allExpected) throws SQLException {
        testList(allExpected);
        testListByFiltersIds(allExpected);
        testListByFiltersNames(allExpected);
        testListByOuRefIds(allExpected);
    }
    //endregion

    private void testListByFiltersNames(List<DevProductivityProfile> allExpected) {
        Map<String, List<DevProductivityProfile>> map = allExpected.stream().collect(Collectors.groupingBy(DevProductivityProfile::getName));
        for (String name : map.keySet()) {
            List<DevProductivityProfile> expected = map.get(name);
            DbListResponse<DevProductivityProfile> result = devProductivityProfileDatabaseService.listByFilter(company, 0, 100, null, List.of(name),null,null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<String> allNames = allExpected.stream().map(DevProductivityProfile::getName).collect(Collectors.toList());
        DbListResponse<DevProductivityProfile> result = devProductivityProfileDatabaseService.listByFilter(company, 0, 100, null, allNames, null,null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }
    private void testListByFiltersIds(List<DevProductivityProfile> allExpected) {
        List<UUID> ids = new ArrayList<>();
        List<DevProductivityProfile> expected = new ArrayList<>();
        for(int i=0; i< allExpected.size(); i++) {
            if(i%2 == 0) {
                continue;
            }
            ids.add(allExpected.get(i).getId());
            expected.add(allExpected.get(i));
        }
        DbListResponse<DevProductivityProfile> result = devProductivityProfileDatabaseService.listByFilter(company, 0, 100, ids, null, null,null);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), expected);
    }

    private void testListByOuRefIds(List<DevProductivityProfile> allExpected) {
        for(DevProductivityProfile profile : allExpected){
            if(CollectionUtils.isEmpty(profile.getAssociatedOURefIds()))
                continue;
            List<DevProductivityProfile> expected = List.of(profile);
            DbListResponse<DevProductivityProfile> result = devProductivityProfileDatabaseService.listByFilter(company, 0, 100, null, null,null,
                    profile.getAssociatedOURefIds().stream().map(Integer::valueOf).collect(Collectors.toList()));
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
    }

    //region Test Get
    private void testGet(DevProductivityProfile allExpected) throws SQLException {
        Optional<DevProductivityProfile> result = devProductivityProfileDatabaseService.get(company, allExpected.getId().toString());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isPresent());
        verifyRecord(result.get(), allExpected);
    }

    private DevProductivityProfile removeIds(DevProductivityProfile result) {
        return result.toBuilder().sections(
                result.getSections().stream().map(s -> s.toBuilder().id(null).features(
                        s.getFeatures().stream().map(f -> f.toBuilder().id(null).build())
                                .collect(Collectors.toList())).build())
                        .collect(Collectors.toList())).build();
    }
    //endregion

    //region Test Update
    private DevProductivityProfile testUpdate(DevProductivityProfile actual, int i) throws SQLException {
        if(i > 5)
            return actual;
        DevProductivityProfile updated = null;
        List<DevProductivityProfile.Section> actualSections = actual.getSections();
        List<DevProductivityProfile.Section> newSections = new ArrayList<>(actualSections);
        if(i == 1){
            Boolean update = ticketCategorizationSchemeDatabaseService.update("test", TicketCategorizationScheme.builder()
                    .id(schemeId1)
                    .name("scheme-5-edit")
                    .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                            .categories(Map.of("1", TicketCategorizationScheme.TicketCategorization.builder()
                                            .id(categoryId1)
                                            .index(1)
                                            .name("category 1 - edit")
                                            .filter(Map.of("test1 - eidt", "filter1 - edit"))
                                            .build(),
                                    "2",TicketCategorizationScheme.TicketCategorization.builder()
                                            .index(2)
                                            .name("category 4")
                                            .filter(Map.of("test4", "filter4"))
                                            .build(),
                                    "3",TicketCategorizationScheme.TicketCategorization.builder()
                                            .id(categoryId2)
                                            .index(3)
                                            .name("category 2")
                                            .filter(Map.of("test2", "filter2"))
                                            .build()))
                            .build())
                    .build());
            assertThat(update).isTrue();
            TicketCategorizationScheme scheme = ticketCategorizationSchemeDatabaseService.get("test",schemeId1).get();
            categoryId4 = scheme.getConfig().getCategories().get("2").getId();
            DevProductivityProfile.Section newSection = DevProductivityProfile.Section.builder()
                    .name("New Section")
                    .description("New Section")
                    .order(2)
                    .features(List.of(
                            DevProductivityProfile.Feature.builder()
                                    .name("New feature 1")
                                    .description("New feature 1")
                                    .order(0)
                                    .featureType(DevProductivityProfile.FeatureType.PERCENTAGE_OF_LEGACY_REWORK)
                                    .maxValue(70L)
                                    .ticketCategories(List.of(UUID.fromString(categoryId1),UUID.fromString(categoryId2)))
                                    .lowerLimitPercentage(25)
                                    .upperLimitPercentage(75)
                                    .build(),
                            DevProductivityProfile.Feature.builder()
                                    .name("New Feature 2")
                                    .description("New feature 2")
                                    .order(1)
                                    .featureType(DevProductivityProfile.FeatureType.BUGS_PER_HUNDRED_LINES_OF_CODE)
                                    .maxValue(70L)
                                    .lowerLimitPercentage(25)
                                    .upperLimitPercentage(75)
                                    .build()

                    )).build();
            newSections.add(newSection);
        }
        updated = DevProductivityProfile.builder()
                    .name("Profile - edit - "+i)
                    .description("Description - edit - "+i)
                    .id(actual.getId())
                .associatedOURefIds(List.of(20+i,30+i).stream().map(String::valueOf).collect(Collectors.toList()))
                .effortInvestmentProfileId(UUID.fromString(schemeId2))
                    .sections(newSections)
                .settings(Map.of())
                .build();
        devProductivityProfileDatabaseService.update(company, updated);
        List<String> associatedOURefIds = i == 2 ? List.of() : List.of(25+i, 35+i).stream().map(String::valueOf).collect(Collectors.toList());
        devProductivityProfileDatabaseService.updateProfileOUMappings(company, updated.getId(), associatedOURefIds);
        updated = updated.toBuilder().associatedOURefIds(associatedOURefIds).build();
        testGet(updated);
        return updated;
    }
    private List<DevProductivityProfile> testUpdates(List<DevProductivityProfile> allExpected) throws SQLException {
        List<DevProductivityProfile> allUpdated = new ArrayList<>();
        for(int i=0; i< allExpected.size(); i++) {
            DevProductivityProfile updated = testUpdate(allExpected.get(i), i+1);
            allUpdated.add(updated);
        }
        testList(allUpdated);
        return allUpdated;
    }
    //endregion

    //region Test Change Default
    private List<DevProductivityProfile> testChangeDefault(List<DevProductivityProfile> allExpected) throws SQLException {
        try {
            devProductivityProfileDatabaseService.upsertDefaultProfile(company, UUID.randomUUID());
            Assert.fail("Exception expected");
        } catch (Exception e) {
            System.out.println(e);
        }
        testGet(allExpected.get(0));
        devProductivityProfileDatabaseService.upsertDefaultProfile(company, allExpected.get(0).getId());
        DevProductivityProfile existingDefault = devProductivityProfileDatabaseService.getDefaultDevProductivityProfile(company).get();
        Assert.assertEquals(allExpected.get(0).getId(),existingDefault.getId());
        devProductivityProfileDatabaseService.upsertDefaultProfile(company, allExpected.get(1).getId());
        existingDefault = devProductivityProfileDatabaseService.getDefaultDevProductivityProfile(company).get();
        Assert.assertNotEquals(allExpected.get(0).getId(),existingDefault.getId());
        return allExpected;
    }
    //endregion

    //region Test Delete
    private void testDelete(List<DevProductivityProfile> allExpected) throws SQLException {
        for(int i=0; i< allExpected.size()-2; i++){
            DevProductivityProfile current = allExpected.get(0);
            Boolean success = devProductivityProfileDatabaseService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            allExpected.remove(0);
            testList(allExpected);
        }
        testList(allExpected);
    }
    //endregion


    private void verifyRecord(DevProductivityProfile a, DevProductivityProfile e) {
        a = removeIds(a);
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getName(), e.getName());
        Assert.assertEquals(a.getEffortInvestmentProfileId(),e.getEffortInvestmentProfileId());
        Assertions.assertThat(a.getSections()).usingRecursiveComparison().ignoringActualNullFields().ignoringExpectedNullFields()
                .ignoringFields("id").ignoringOverriddenEqualsForTypes(DevProductivityProfile.Feature.class)
        .isEqualTo(e.getSections());
        Assertions.assertThat(a.getAssociatedOURefIds()).hasSameElementsAs(e.getAssociatedOURefIds());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }

    private void verifyRecords(List<DevProductivityProfile> a, List<DevProductivityProfile> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, DevProductivityProfile> actualMap = a.stream().collect(Collectors.toMap(DevProductivityProfile::getId, x -> x));
        Map<UUID, DevProductivityProfile> expectedMap = e.stream().collect(Collectors.toMap(DevProductivityProfile::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private DevProductivityProfile createDevProductivityProfile(int i) {
        return DevProductivityProfile.builder()
                .name("Profile "+i)
                .description("Profile "+i)
                .predefinedProfile(false)
                .associatedOURefIds(List.of(i,10+i).stream().map(String::valueOf).collect(Collectors.toList()))
                .effortInvestmentProfileId(UUID.fromString(schemeId1))
                .sections(List.of(
                        DevProductivityProfile.Section.builder()
                                .name("Quality of Work → Type of Work")
                                .description("Quality of Work → Type of Work")
                                .order(0)
                                .features(List.of(
                                        DevProductivityProfile.Feature.builder()
                                                .name("New Features vs. Rework vs. Legacy work")
                                                .description("New Features vs. Rework vs. Legacy work")
                                                .order(0)
                                                .featureType(DevProductivityProfile.FeatureType.PERCENTAGE_OF_LEGACY_REWORK)
                                                .maxValue(70L)
                                                .ticketCategories(List.of(UUID.fromString(categoryId1),UUID.fromString(categoryId2)))
                                                .lowerLimitPercentage(25)
                                                .upperLimitPercentage(75)
                                                .build(),
                                        DevProductivityProfile.Feature.builder()
                                                .name("Bugs per 100 LoC")
                                                .description("Bugs per 100 LoC")
                                                .order(1)
                                                .featureType(DevProductivityProfile.FeatureType.BUGS_PER_HUNDRED_LINES_OF_CODE)
                                                .maxValue(70L)
                                                .lowerLimitPercentage(25)
                                                .upperLimitPercentage(75)
                                                .build()

                )).build(),
                        DevProductivityProfile.Section.builder()
                                .name("Volume of Work")
                                .description("Volume of Work")
                                .order(1)
                                .features(List.of(
                                        DevProductivityProfile.Feature.builder()
                                                .name("Percentage of Duplicated Code")
                                                .description("Percentage of Duplicated Code")
                                                .order(0)
                                                .featureType(DevProductivityProfile.FeatureType.PERCENTAGE_OF_REWORK)
                                                .maxValue(70L)
                                                .lowerLimitPercentage(25)
                                                .upperLimitPercentage(75)
                                                .build(),
                                        DevProductivityProfile.Feature.builder()
                                                .name("Number of Commits")
                                                .description("Number of Commits")
                                                .order(1)
                                                .featureType(DevProductivityProfile.FeatureType.BUGS_PER_HUNDRED_LINES_OF_CODE)
                                                .maxValue(70L)
                                                .lowerLimitPercentage(25)
                                                .upperLimitPercentage(75)
                                                .build()
                                )).build()
                ))
                .settings(Map.of("exclude", Map.of("title", "first")))
        .build();
    }

}
