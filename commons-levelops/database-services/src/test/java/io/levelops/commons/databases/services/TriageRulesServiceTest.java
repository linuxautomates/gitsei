package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.jackson.DefaultObjectMapper;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TriageRulesServiceTest {
    private static final String company = "test";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static TriageRulesService triageRulesService;
    private static DataSource dataSource;

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        triageRulesService = new TriageRulesService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        triageRulesService.ensureTableExistence(company);
    }

    public TriageRule testInsert(int i) throws SQLException {
        TriageRule.TriageRuleBuilder bldr = TriageRule.builder()
                .name("name-" + i)
                .owner("hello")
                .application("world")
                .description("huh")
                .metadata(Map.of("Hello", "world"))
                .regexes(List.of());
        String id = triageRulesService.insert(company, bldr.build());
        return bldr.id(id).build();
    }

    private List<TriageRule> testInserts(int n) throws SQLException {
        List<TriageRule> trs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            trs.add(testInsert(i));
        }
        return trs;
    }

    private void verifyTrs(TriageRule a, TriageRule e) {
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getName(), e.getName());
        Assert.assertEquals(a.getRegexes(), e.getRegexes());
    }

    private void verifyNotTrs(TriageRule a, TriageRule e) {
        Assert.assertNotEquals(a.getId(), e.getId());
        Assert.assertNotEquals(a.getName(), e.getName());
    }

    @Test
    public void test() throws SQLException {
        List<TriageRule> trs = testInserts(5);
        verifyTrs(trs.get(0), triageRulesService.get(company, trs.get(0).getId()).orElse(null));
        verifyTrs(trs.get(1), triageRulesService.list(company, List.of(trs.get(1).getId()),
                null, null, null, 0, 1)
                .getRecords().get(0));
        verifyTrs(trs.get(2), triageRulesService.list(company, null, null,
                null, trs.get(2).getName(), 0, 1).getRecords().get(0));
        verifyTrs(trs.get(2), triageRulesService.list(company, null, List.of("world"),
                List.of("hello"), trs.get(2).getName(), 0, 1).getRecords().get(0));
        assertThat(triageRulesService.list(company, null, List.of("world"),
                List.of("huh"), trs.get(2).getName(), 0, 1).getRecords().size()).isEqualTo(0);
        assertThat(triageRulesService.list(company, null, List.of("whu"),
                List.of("hello"), trs.get(2).getName(), 0, 1).getRecords().size()).isEqualTo(0);
        verifyNotTrs(trs.get(1), triageRulesService.list(company, null, null,
                null, trs.get(2).getName(), 0, 1).getRecords().get(0));

    }

    @Test
    public void bulkDeleteTest() throws SQLException {
        List<TriageRule> trs1 = testInserts(5);
        List<String> collect = trs1.stream().map(TriageRule::getId).collect(Collectors.toList());
        Assert.assertEquals(4,
                triageRulesService.bulkDelete(company, List.of(collect.get(0), collect.get(1), collect.get(2), collect.get(3))));
        List<TriageRule> list = triageRulesService.list(company, null, null,
                null, null, 0, 5).getRecords();
        Assert.assertEquals(1, list.size());
        DefaultObjectMapper.prettyPrint(triageRulesService.list(company, null, null,
                null, null, 0, 5).getRecords());
    }
}