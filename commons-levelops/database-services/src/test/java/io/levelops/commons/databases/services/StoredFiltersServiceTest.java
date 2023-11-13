package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.StoredFilter;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class StoredFiltersServiceTest {

    private static final String company = "test";
    private static final String TRIAGE_FILTER_TYPE = "triage";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static StoredFiltersService storedFiltersService;

    @BeforeClass
    public static void setup() throws SQLException {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        storedFiltersService = new StoredFiltersService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        storedFiltersService.ensureTableExistence(company);
    }

    @Test
    public void test() throws SQLException {
        List<StoredFilter> filters = testInserts();
        validateFilters(filters);
        filters = modifyInsertedFilters();
        validateFilters(filters);
    }

    private List<StoredFilter> testInserts() throws SQLException {
        List<StoredFilter> filters = new ArrayList<>();
        filters.add(testInsert("grid-view", TRIAGE_FILTER_TYPE, "this is grid-view filter", true, Map.of(
                "job_ids", List.of("8490cced-a254-45ba-866a-38b4381e8ea3", "3ce3a438-d0a9-4d6b-95f2-85d8366930fd"),
                "results", List.of("SUCCEEDED", "ABORTED")
        )));
        filters.add(testInsert("results", TRIAGE_FILTER_TYPE, "this is results filter", false, Map.of(
                "triage_rule_ids", List.of("8490cced-a254-45ba-866a-38b4381e8ea3", "3ce3a438-d0a9-4d6b-95f2-85d8366930fd"),
                "parent_job_ids", List.of("3adbb0be-c5c0-443c-ad58-ca85249f0d0e", "b549077c-628c-4a58-acf8-1d6250ad5ed7")
        )));
        return filters;
    }

    private StoredFilter testInsert(String name, String type, String description, Boolean isDefault, Map<String, Object> filter) throws SQLException {
        StoredFilter.StoredFilterBuilder filterBuilder = StoredFilter.builder()
                .name(name)
                .type(type)
                .description(description)
                .isDefault(isDefault)
                .filter(filter);
        String id = storedFiltersService.insert(company, filterBuilder.build());
        return filterBuilder.id(id).build();
    }

    private List<StoredFilter> modifyInsertedFilters() throws SQLException {
        List<StoredFilter> filters = new ArrayList<>();
        filters.add(testInsert("grid-view", TRIAGE_FILTER_TYPE, "this is grid-view filter", false, Map.of(
                "job_ids", List.of("3ce3a438-d0a9-4d6b-95f2-85d8366930fd"),
                "results", List.of("SUCCEEDED")
        )));
        filters.add(testInsert("results", TRIAGE_FILTER_TYPE,"this is results filter", false, Map.of(
                "triage_rule_ids", List.of("8490cced-a254-45ba-866a-38b4381e8ea3"),
                "parent_job_ids", List.of("b549077c-628c-4a58-acf8-1d6250ad5ed7")
        )));
        return filters;
    }

    private void verifyFiltersEqual(StoredFilter e, StoredFilter a) {
        Assert.assertEquals(e.getId(), a.getId());
        Assert.assertEquals(e.getName(), a.getName());
        Assert.assertEquals(e.getDescription(), a.getDescription());
        Assert.assertEquals(e.getIsDefault(), a.getIsDefault());
        Assert.assertEquals(e.getFilter(), a.getFilter());
    }

    private void validateFilters(List<StoredFilter> filters) throws SQLException {
        verifyFiltersEqual(filters.stream().filter(filter -> filter.getName().equals("grid-view")).findFirst().get(),
                Objects.requireNonNull(storedFiltersService.get(company, TRIAGE_FILTER_TYPE, "grid-view").orElse(null)));
        verifyFiltersEqual(filters.stream().filter(filter -> filter.getName().equals("results")).findFirst().get(),
                Objects.requireNonNull(storedFiltersService.get(company, TRIAGE_FILTER_TYPE, "results").orElse(null)));
    }

    @Test
    public void testRemoveDefault() throws SQLException {
        StoredFilter filter = StoredFilter.builder()
                .name("filter")
                .type("triage")
                .description("this is filter")
                .isDefault(true)
                .build();
        StoredFilter filter1 = StoredFilter.builder()
                .name("filter1")
                .type("triage")
                .description("this is filter1")
                .isDefault(true)
                .build();
        storedFiltersService.insert(company, filter);
        storedFiltersService.insert(company, filter1);
        filter = storedFiltersService.get(company, TRIAGE_FILTER_TYPE, filter.getName()).stream()
                .filter(f -> f.getName().equals("filter")).findFirst().get();
        filter1 = storedFiltersService.get(company, TRIAGE_FILTER_TYPE, filter1.getName()).stream()
                .filter(f -> f.getName().equals("filter1")).findFirst().get();
        Assert.assertEquals(false, filter.getIsDefault());
        Assert.assertEquals(true, filter1.getIsDefault());
    }

    @Test
    public void testList() throws SQLException {
        List<StoredFilter> filters = testInserts();
        verifyFiltersEqual(filters.stream().filter(filter -> filter.getName().equals("grid-view")).findFirst().get(),
                storedFiltersService.get(company, "triage", "grid-view").orElse(null));
        verifyFiltersEqual(filters.stream().filter(filter -> filter.getName().equals("grid-view")).findFirst().get(),
                storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), List.of("grid-view"), true, null, 0, 1).getRecords()
                        .stream().filter(triageFilter -> triageFilter.getName().equals("grid-view")).findFirst().get());
        verifyFiltersEqual(filters.stream().filter(filter -> filter.getName().equals("results")).findFirst().get(),
                storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), List.of("results"), false, null, 0, 1).getRecords()
                        .stream().filter(triageFilter -> triageFilter.getName().equals("results")).findFirst().get());
        assertThat(storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, null, 0, 1).getCount()).isEqualTo(1);
        assertThat(storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, null, 1, 1).getCount()).isEqualTo(1);
        assertThat(storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, null, 2, 1).getCount()).isEqualTo(0);
        assertThat(storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, Map.of("name", Map.of("$begins", "grid")), 0, 1)
                .getRecords().size()).isEqualTo(1);
        assertThat(storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, Map.of("name", Map.of("$begins", "re")), 0, 1)
                .getRecords().size()).isEqualTo(1);
        assertThat(storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, Map.of("name", Map.of("$begins", "invalid")), 0, 1)
                .getRecords().size()).isEqualTo(0);
        assertThat(storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, Map.of("name", Map.of("$ends", "view")), 0, 1)
                .getRecords().size()).isEqualTo(1);
        assertThat(storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, Map.of("name", Map.of("$ends", "lts")), 0, 1)
                .getRecords().size()).isEqualTo(1);
        assertThat(storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, Map.of("name", Map.of("$ends", "invalid")), 0, 1)
                .getRecords().size()).isEqualTo(0);
        assertThat(storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, Map.of("name", Map.of("$contains", "view")), 0, 1)
                .getRecords().size()).isEqualTo(1);
        assertThat(storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, Map.of("name", Map.of("$contains", "lts")), 0, 1)
                .getRecords().size()).isEqualTo(1);
        assertThat(storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, Map.of("name", Map.of("$contains", "invalid")), 0, 1)
                .getRecords().size()).isEqualTo(0);
    }

    @Test
    public void deleteTest() throws SQLException {
        List<StoredFilter> filters = testInserts();
        List<String> filterNames = filters.stream().map(StoredFilter::getName).collect(Collectors.toList());
        Assert.assertEquals(true, storedFiltersService.delete(company, "triage", filterNames.get(0)));
        List<StoredFilter> list = storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, null, 0, 5).getRecords();
        Assert.assertEquals(3, list.size());
    }

    @Test
    public void bulkDeleteTest() throws SQLException {
        List<StoredFilter> filters = testInserts();
        List<String> filterNames = filters.stream().map(StoredFilter::getName).collect(Collectors.toList());
        Assert.assertEquals(2, storedFiltersService.bulkDelete(company, "triage", filterNames));
        List<StoredFilter> list = storedFiltersService.list(company, List.of(TRIAGE_FILTER_TYPE), null, null, null, 0, 5).getRecords();
        Assert.assertEquals(0, list.size());
    }
}
