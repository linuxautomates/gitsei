package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.models.database.TriageRuleHit;

import org.junit.Assert;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class TriageRuleHitUtils {
    public static TriageRuleHit createTriageRuleHit(TriageRuleHitsService triageRuleHitsService, final String company, int i, UUID cicdJobRunId, UUID cicdJobRUnStageId, UUID stepId, TriageRule triageRule) throws SQLException {
        TriageRuleHit ruleHit = TriageRuleHit.builder()
                .jobRunId(cicdJobRunId.toString())
                .stageId((cicdJobRUnStageId != null) ? cicdJobRUnStageId.toString() : null)
                .stepId(stepId.toString())
                .ruleId(triageRule.getId())
                .count(i + 1)
                .hitContent("test-" + i)
                .type(TriageRuleHit.RuleHitType.JENKINS)
                .context(Map.of("step", "1"))
                .build();
        String id = triageRuleHitsService.insert(company, ruleHit);
        Assert.assertNotNull(id);
        return ruleHit.toBuilder().id(id).build();
    }
}
