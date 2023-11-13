package io.levelops.commons.databases.services.automation_rules;

import io.levelops.commons.databases.models.database.automation_rules.AutomationRule;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRuleHit;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import org.junit.Assert;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AutomationRuleHitTestUtils {
    public static AutomationRuleHit buildAutomationRule(String company, int i, AutomationRule rule) {
        AutomationRuleHit ruleHit = AutomationRuleHit.builder()
                .objectId("object-id" + i)
                .objectType(ObjectType.JIRA_ISSUE)
                .ruleId(rule.getId())
                .count(1 + i)
                .hitContent("content-" + i)
                .context(Map.of("criterea_1", String.valueOf(i)))
                .build();
        return ruleHit;
    }

    public static AutomationRuleHit createAutomationRuleHit(AutomationRuleHitsDatabaseService automationRuleHitsDatabaseService, String company, int i,  AutomationRule rule) throws SQLException {
        AutomationRuleHit ruleHit = buildAutomationRule(company, i, rule);
        String ruleHitId = automationRuleHitsDatabaseService.insert(company, ruleHit);
        Assert.assertNotNull(ruleHitId);
        return ruleHit.toBuilder().id(UUID.fromString(ruleHitId)).build();
    }

    public static List<AutomationRuleHit> createAutomationRuleHits(AutomationRuleHitsDatabaseService automationRuleHitsDatabaseService, String company, List<AutomationRule> rules) throws SQLException {
        List<AutomationRuleHit> ruleHits = new ArrayList<>();
        for(int i=0; i<rules.size(); i++) {
            ruleHits.add(createAutomationRuleHit(automationRuleHitsDatabaseService, company, i, rules.get(i)));
        }
        return ruleHits;
    }
}
