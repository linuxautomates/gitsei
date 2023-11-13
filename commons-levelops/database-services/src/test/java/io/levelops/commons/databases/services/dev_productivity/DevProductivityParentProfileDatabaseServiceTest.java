package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.services.*;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DevProductivityParentProfileDatabaseServiceTest {
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
    private static DevProductivityParentProfileDatabaseService devProductivityParentProfileDatabaseService;
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
        devProductivityParentProfileDatabaseService = new DevProductivityParentProfileDatabaseService(dataSource, MAPPER, true);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource,MAPPER);
        devProductivityProfileDatabaseService = new DevProductivityProfileDatabaseService(dataSource, MAPPER, true);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        integrationService.ensureTableExistence(company);
        productService.ensureTableExistence(company);
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(company);
        devProductivityProfileDatabaseService.ensureTableExistence(company);
        devProductivityParentProfileDatabaseService.ensureTableExistence(company);


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
    public void test() throws Exception {
        List<DevProductivityParentProfile> expected = testInserts(5);
        DevProductivityParentProfile centralProfile = devProductivityParentProfileDatabaseService.getDefaultDevProductivityProfile(company).get();
        DevProductivityParentProfile centralProfileExpected = DevProductivityParentProfileDatabaseServiceTestUtils.loadCentralProfileTemplate();
        verifyRecord(centralProfile, centralProfileExpected);
        expected.add(centralProfile);
        testAllList(expected);
        expected = testUpdates(expected);
        testDelete(expected);
    }


    private List<DevProductivityParentProfile> testInserts(int n) throws Exception {
        List<DevProductivityParentProfile> expected = new ArrayList<>();
        for(int i=1; i<=n; i++) {
            DevProductivityParentProfile current = testInsert(i);
            testGet(current);
            expected.add(current);
        }
        return expected;
    }

    private DevProductivityParentProfile testInsert(int i) throws Exception {
        DevProductivityParentProfile devProductivityParentProfile = createDevProductivityParentProfile(i);
        String id = devProductivityParentProfileDatabaseService.insert(company, devProductivityParentProfile);
        Assert.assertNotNull(id);
        devProductivityParentProfile = devProductivityParentProfile.toBuilder().id(UUID.fromString(id)).build();
        return devProductivityParentProfile;
    }

    private void testList(List<DevProductivityParentProfile> expected) throws SQLException {
        DbListResponse<DevProductivityParentProfile> result = devProductivityParentProfileDatabaseService.list(company, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        List<DevProductivityParentProfile> actual = result.getRecords();
        verifyRecords(actual, expected);

        result = devProductivityParentProfileDatabaseService.list(company, 0, 2);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(2, result.getCount().intValue());
        actual = result.getRecords();
        verifyRecords(actual, expected.subList(3, 5));
    }
    private void testAllList(List<DevProductivityParentProfile> allExpected) throws SQLException {
        testList(allExpected);
        testListByFiltersIds(allExpected);
        testListByFiltersNames(allExpected);
        testListByOuRefIds(allExpected);
    }
    //endregion

    private void testListByFiltersNames(List<DevProductivityParentProfile> allExpected) {
        Map<String, List<DevProductivityParentProfile>> map = allExpected.stream().collect(Collectors.groupingBy(DevProductivityParentProfile::getName));
        for (String name : map.keySet()) {
            List<DevProductivityParentProfile> expected = map.get(name);
            DbListResponse<DevProductivityParentProfile> result = devProductivityParentProfileDatabaseService.listByFilter(company, 0, 100, null, List.of(name),null,null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<String> allNames = allExpected.stream().map(DevProductivityParentProfile::getName).collect(Collectors.toList());
        DbListResponse<DevProductivityParentProfile> result = devProductivityParentProfileDatabaseService.listByFilter(company, 0, 100, null, allNames, null,null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }
    private void testListByFiltersIds(List<DevProductivityParentProfile> allExpected) {
        List<UUID> ids = new ArrayList<>();
        List<DevProductivityParentProfile> expected = new ArrayList<>();
        for(int i=0; i< allExpected.size(); i++) {
            if(i%2 == 0) {
                continue;
            }
            ids.add(allExpected.get(i).getId());
            expected.add(allExpected.get(i));
        }
        DbListResponse<DevProductivityParentProfile> result = devProductivityParentProfileDatabaseService.listByFilter(company, 0, 100, ids, null, null,null);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), expected);
    }

    private void testListByOuRefIds(List<DevProductivityParentProfile> allExpected) {
        for(DevProductivityParentProfile profile : allExpected){
            if(CollectionUtils.isEmpty(profile.getAssociatedOURefIds()))
                continue;
            List<DevProductivityParentProfile> expected = List.of(profile);
            DbListResponse<DevProductivityParentProfile> result = devProductivityParentProfileDatabaseService.listByFilter(company, 0, 100, null, null,null,
                    profile.getAssociatedOURefIds().stream().map(Integer::valueOf).collect(Collectors.toList()));
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
    }

    //region Test Get
    private void testGet(DevProductivityParentProfile allExpected) throws SQLException {
        Optional<DevProductivityParentProfile> result = devProductivityParentProfileDatabaseService.get(company, allExpected.getId().toString());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isPresent());
        verifyRecord(result.get(), allExpected);
    }
    //endregion

    //region Test Update
    private DevProductivityParentProfile testUpdate(DevProductivityParentProfile actual, int i) throws Exception {
        DevProductivityParentProfile centralProfile = null;
        switch(i){
            case 1 :
                //apply central profile to ous 1,6. ou 1 already has a profile and ou 6 doesn't have a profile
                //expected output - both ou 1 and ou 6 will be associated to central profile
                centralProfile = devProductivityParentProfileDatabaseService.getDefaultDevProductivityProfile(company).get();
                devProductivityParentProfileDatabaseService.copyParentProfile(company, centralProfile, List.of("1","6"));
                verifyRecord(centralProfile.toBuilder().associatedOURefIds(List.of("1","6")).build(), devProductivityParentProfileDatabaseService.listByFilter(company, 0, 1, null, null, null, List.of(1)).getRecords().get(0));
                verifyRecord(centralProfile.toBuilder().associatedOURefIds(List.of("1","6")).build(), devProductivityParentProfileDatabaseService.listByFilter(company, 0, 1, null, null, null, List.of(6)).getRecords().get(0));
                verifyRecord(centralProfile.toBuilder().associatedOURefIds(List.of("1","6")).build(), devProductivityParentProfileDatabaseService.listByFilter(company, 0, 1, List.of(centralProfile.getId()), null, null, null).getRecords().get(0));
                break;
            case 2:
                //load central profile, change contents of central profile and apply it to ous 1,2,7.
                //ou 1,2 already have a profile and ou 7 doesn't have a profile
                //expected output - new profile created for ou 7, profiles of ou 1 and 2 are updated
                //change parent profile name
                centralProfile = devProductivityParentProfileDatabaseService.getDefaultDevProductivityProfile(company).get();
                DevProductivityParentProfile updated = centralProfile.toBuilder().name("Updated Profile").build();
                devProductivityParentProfileDatabaseService.copyParentProfile(company, updated, List.of("1","2","7"));
                verifyRecord(updated.toBuilder().associatedOURefIds(List.of("1")).build(), devProductivityParentProfileDatabaseService.listByFilter(company, 0, 1, null, null, null, List.of(1)).getRecords().get(0));
                verifyRecord(updated.toBuilder().associatedOURefIds(List.of("2")).build(), devProductivityParentProfileDatabaseService.listByFilter(company, 0, 1, null, null, null, List.of(2)).getRecords().get(0));
                verifyRecord(updated.toBuilder().associatedOURefIds(List.of("7")).build(), devProductivityParentProfileDatabaseService.listByFilter(company, 0, 1, null, null, null, List.of(7)).getRecords().get(0));

                //change sub-profile name
                updated = updated.toBuilder().subProfiles(updated.getSubProfiles()
                        .stream().map(s -> {
                            if(s.getOrder() == 1){
                                s = s.toBuilder().name("Updated "+s.getName()).build();
                            }
                            return s;
                        }).collect(Collectors.toList())).build();
                devProductivityParentProfileDatabaseService.copyParentProfile(company, updated, List.of("1"));
                verifyRecord(updated.toBuilder().associatedOURefIds(List.of("1")).build(), devProductivityParentProfileDatabaseService.listByFilter(company, 0, 1, null, null, null, List.of(1)).getRecords().get(0));

                devProductivityParentProfileDatabaseService.setTrellisEnabledOnOUMappingByOuRefId(company, Set.of(1),false);
                verifyRecord(updated.toBuilder().associatedOURefIds(List.of("1")).ouTrellisEnabledMap(Map.of(1,false)).build(), devProductivityParentProfileDatabaseService.listByFilter(company, 0, 1, null, null, null, List.of(1)).getRecords().get(0));
                //change section weights
                //change feature params
                //change categorization scheme
                break;
            case 3 :
                //Update the central profile itself
                centralProfile = devProductivityParentProfileDatabaseService.getDefaultDevProductivityProfile(company).get();
                devProductivityParentProfileDatabaseService.copyParentProfile(company, centralProfile, List.of("1","2"));
                centralProfile = devProductivityParentProfileDatabaseService.getDefaultDevProductivityProfile(company).get();
                DevProductivityParentProfile centralProfileUpdated = centralProfile.toBuilder().name("Central profile - updated")
                        .build();
                devProductivityParentProfileDatabaseService.update(company, centralProfileUpdated);
                verifyRecord(centralProfileUpdated, devProductivityParentProfileDatabaseService.getDefaultDevProductivityProfile(company).get());
            case 4:
                //Disable trellis on a OU and then select it in the copy profile
                //Expected output - trellis is automatically enabled on OU
                devProductivityParentProfileDatabaseService.setTrellisEnabledOnOUMappingByOuRefId(company,Set.of(5),false);
                DevProductivityParentProfile p = devProductivityParentProfileDatabaseService.listByFilter(company,0,1,null, null,null,List.of(5)).getRecords().get(0);
                devProductivityParentProfileDatabaseService.copyParentProfile(company,p,null);
                verifyRecord(p.toBuilder().associatedOURefIds(List.of("5")).build(), devProductivityParentProfileDatabaseService.listByFilter(company, 0, 1, null, null, null, List.of(5)).getRecords().get(0));
                break;
            case 5:
                centralProfile = devProductivityParentProfileDatabaseService.getDefaultDevProductivityProfile(company).get();
                devProductivityParentProfileDatabaseService.update(company, centralProfile.toBuilder()
                        .effortInvestmentProfileId(UUID.fromString(schemeId2))
                        .featureTicketCategoriesMap(Map.of(DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH,List.of(UUID.fromString(categoryId2)), DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH, List.of(UUID.fromString(categoryId3))))
                        .build());
                centralProfileUpdated = centralProfile.toBuilder().featureTicketCategoriesMap(Map.of(DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH,List.of(UUID.fromString(categoryId2)), DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH, List.of(UUID.fromString(categoryId3))))
                                .effortInvestmentProfileId(UUID.fromString(schemeId2)).build();
                centralProfileUpdated = devProductivityParentProfileDatabaseService.populateTicketCategoriesToFeatures(centralProfileUpdated);
                verifyRecord(centralProfileUpdated, devProductivityParentProfileDatabaseService.getDefaultDevProductivityProfile(company).get());
        }
        return actual;
    }
    private List<DevProductivityParentProfile> testUpdates(List<DevProductivityParentProfile> allExpected) throws Exception {
        List<DevProductivityParentProfile> allUpdated = new ArrayList<>();
        for(int i=0; i< allExpected.size(); i++) {
            DevProductivityParentProfile updated = testUpdate(allExpected.get(i), i+1);
            allUpdated.add(updated);
        }
        //testList(allUpdated);
        return allUpdated;
    }
    //endregion

    //region Test Delete
    private void testDelete(List<DevProductivityParentProfile> allExpected) throws SQLException {
        for(int i=0; i< allExpected.size(); i++){
            DevProductivityParentProfile current = allExpected.get(0);
            if(current.getDefaultProfile())
                continue;
            Boolean success = devProductivityParentProfileDatabaseService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            allExpected.remove(0);
            //testList(allExpected);
        }
        //testList(allExpected);
    }
    //endregion

    private void verifyRecords(List<DevProductivityParentProfile> a, List<DevProductivityParentProfile> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, DevProductivityParentProfile> actualMap = a.stream().collect(Collectors.toMap(DevProductivityParentProfile::getId, x -> x));
        Map<UUID, DevProductivityParentProfile> expectedMap = e.stream().collect(Collectors.toMap(DevProductivityParentProfile::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void verifyRecord(DevProductivityParentProfile a, DevProductivityParentProfile e) {
        a = removeIds(a);
        // Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getName(), e.getName());
        Assert.assertEquals(a.getEffortInvestmentProfileId(),e.getEffortInvestmentProfileId());
        Assertions.assertThat(MapUtils.emptyIfNull(a.getFeatureTicketCategoriesMap())).containsExactlyInAnyOrderEntriesOf(MapUtils.emptyIfNull(e.getFeatureTicketCategoriesMap()));
        Assertions.assertThat(a.getSubProfiles()).usingRecursiveComparison().ignoringActualNullFields().ignoringExpectedNullFields()
                .ignoringFields("id","createdAt","updatedAt").ignoringOverriddenEqualsForTypes(DevProductivityProfile.Feature.class)
                .isEqualTo(e.getSubProfiles());
        /*Assertions.assertThat(a.getSections()).usingRecursiveComparison().ignoringActualNullFields().ignoringExpectedNullFields()
                .ignoringFields("id").ignoringOverriddenEqualsForTypes(DevProductivityProfile.Feature.class)
                .isEqualTo(e.getSections());*/
        Assertions.assertThat(a.getAssociatedOURefIds()).hasSameElementsAs(e.getAssociatedOURefIds());
//        Assertions.assertThat(MapUtils.emptyIfNull(a.getOuTrellisEnabledMap())).containsExactlyEntriesOf(MapUtils.emptyIfNull(e.getOuTrellisEnabledMap()));
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }

    private DevProductivityParentProfile removeIds(DevProductivityParentProfile result) {
        return result.toBuilder().subProfiles(result.getSubProfiles().stream().map(p -> p.toBuilder().id(null).sections(p.getSections().stream().map(s -> s.toBuilder().id(null).features(
                                s.getFeatures().stream().map(f -> f.toBuilder().id(null).build())
                                        .collect(Collectors.toList())).build())
                        .collect(Collectors.toList())).build())
                .collect(Collectors.toList())).build();
    }

    private DevProductivityParentProfile createDevProductivityParentProfile(int i) throws IOException {
        DevProductivityParentProfile template = DevProductivityParentProfileDatabaseServiceTestUtils.loadCentralProfileTemplate();
        if(i == 2){
            template = template.toBuilder().effortInvestmentProfileId(UUID.fromString(schemeId1)).featureTicketCategoriesMap(Map.of(DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH,List.of(UUID.fromString(categoryId1)), DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH,List.of(UUID.fromString(categoryId2)))).build();
        }
        return template.toBuilder().associatedOURefIds(List.of(String.valueOf(i))).name("Test profile "+i).build();
    }

    @Test
    public void testPopulateTicketCategoriesToFeatures() throws IOException {
        DevProductivityParentProfile centralTemplate = DevProductivityParentProfileDatabaseServiceTestUtils.loadCentralProfileTemplate();
        centralTemplate = centralTemplate.toBuilder().effortInvestmentProfileId(UUID.randomUUID())
                .featureTicketCategoriesMap(Map.of(DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH,List.of(UUID.fromString(categoryId1)), DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH,List.of(UUID.fromString(categoryId2))))
                .build();
        DevProductivityParentProfile centralTemplateUpdated = devProductivityParentProfileDatabaseService.populateTicketCategoriesToFeatures(centralTemplate);
        Assert.assertNotNull(centralTemplateUpdated);
    }

}
