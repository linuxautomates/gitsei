package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobConfigChange;
import org.junit.Assert;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CiCdJobConfigChangesUtils {
    public static CICDJobConfigChange createCICDJobConfigChange(CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService, String company, CICDJob cicdJob, int cicdUserIndex, int i) throws SQLException {
        CICDJobConfigChange cicdJobConfigChange = CICDJobConfigChange.builder()
                .cicdJobId(cicdJob.getId())
                .changeTime(Instant.now().minus(i, ChronoUnit.DAYS))
                .changeType("changed")
                .cicdUserId("user-jenkins-" + cicdUserIndex)
                .build();
        String id = ciCdJobConfigChangesDatabaseService.insert(company, cicdJobConfigChange);
        Assert.assertNotNull(id);
        CICDJobConfigChange expected = cicdJobConfigChange.toBuilder().id(UUID.fromString(id)).build();
        return expected;
    }

    public static List<CICDJobConfigChange> createCICDJobConfigChanges(CiCdJobConfigChangesDatabaseService ciCdJobConfigChangesDatabaseService, String company, CICDJob cicdJob, int cicdUserIndex, int n) throws SQLException {
        List<CICDJobConfigChange> cicdJobConfigChanges = new ArrayList<>();
        for(int i=n; i>0; i--){
            CICDJobConfigChange cicdJobConfigChange = createCICDJobConfigChange(ciCdJobConfigChangesDatabaseService, company, cicdJob, cicdUserIndex, i);
            cicdJobConfigChanges.add(cicdJobConfigChange);
        }
        return cicdJobConfigChanges;
    }
}
