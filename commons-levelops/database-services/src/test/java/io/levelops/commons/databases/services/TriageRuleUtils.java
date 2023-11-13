package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.TriageRule;
import org.junit.Assert;

import java.sql.SQLException;
import java.util.List;

public class TriageRuleUtils {
    public static TriageRule createTriageRule(final TriageRulesService triageRulesService, final String company, int i) throws SQLException {
        TriageRule.TriageRuleBuilder bldr = TriageRule.builder()
                .name("name-" + i)
                .application("hello")
                .owner("world")
                .description("")
                .regexes(List.of());
        String id = triageRulesService.insert(company, bldr.build());
        Assert.assertNotNull(id);
        return bldr.id(id).build();
    }
}
