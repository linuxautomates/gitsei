package io.levelops.commons.databases.services.automation_rules;

import io.levelops.commons.databases.models.database.automation_rules.AutomationRule;
import io.levelops.commons.databases.models.database.automation_rules.Criterea;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import org.junit.Assert;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AutomationRuleTestUtils {
    public static AutomationRule buildAutomationRule(String company, int i) throws SQLException {
        List<Criterea> critereas = List.of(
                Criterea.builder()
                        .fieldName("issue.name")
                        .regexes(List.of("name 1 " + i, "name 2 " + i))
                        .build(),
                Criterea.builder()
                        .fieldName("issue.name")
                        .regexes(List.of("name 1 " + i, "name 2 " + i))
                        .build()
        );
        AutomationRule rule = AutomationRule.builder()
                .name("name - " + i)
                .description("description - " + i)
                .source("source - " + i)
                .owner("owner - " + i)
                .objectType(ObjectType.JIRA_ISSUE)
                .critereas(critereas)
                .build();
        return rule;
    }

    public static AutomationRule createAutomationRule(AutomationRulesDatabaseService automationRulesDatabaseService, String company, int i) throws SQLException {
        List<Criterea> critereas = List.of(
                Criterea.builder()
                        .fieldName("issue.name")
                        .regexes(List.of("name 1 " + i, "name 2 " + i))
                        .build(),
                Criterea.builder()
                        .fieldName("issue.name")
                        .regexes(List.of("name 1 " + i, "name 2 " + i))
                        .build()
        );
        AutomationRule rule = AutomationRule.builder()
                .name("name - " + i)
                .description("description - " + i)
                .source("source - " + i)
                .owner("owner - " + i)
                .objectType(ObjectType.JIRA_ISSUE)
                .critereas(critereas)
                .build();
        String ruleId = automationRulesDatabaseService.insert(company, rule);
        Assert.assertNotNull(ruleId);
        return rule.toBuilder().id(UUID.fromString(ruleId)).build();
    }

    public static List<AutomationRule> createAutomationRules(AutomationRulesDatabaseService automationRulesDatabaseService, String company, int n) throws SQLException {
        List<AutomationRule> rules = new ArrayList<>();
        for(int i=0; i<n; i++) {
            rules.add(createAutomationRule(automationRulesDatabaseService, company, i));
        }
        return rules;
    }
}
