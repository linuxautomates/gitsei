package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import org.junit.Assert;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CiCdJobUtils {
    public static CICDJob createCICDJob(CiCdJobsDatabaseService ciCdJobsDatabaseService, String company, int i, CICDInstance cicdInstance) throws SQLException {
        String jobName = "jobname-" + i;
        String branchName = "branch-name-" + i;
        CICDJob.CICDJobBuilder bldr = CICDJob.builder()
                .jobName(jobName)
                .projectName("project-"+ i)
                .jobFullName(jobName + "/branches/" + branchName)
                .jobNormalizedFullName(jobName + "/" + branchName)
                .branchName(branchName)
                .moduleName("module-name-" + i)
                .scmUrl("url-" + i)
                .scmUserId("user-git-" + i);
        if(cicdInstance != null){
            bldr.cicdInstanceId(cicdInstance.getId());
        }

        CICDJob cicdJob = bldr.build();
        String id = ciCdJobsDatabaseService.insert(company, cicdJob);
        Assert.assertNotNull(id);
        CICDJob expected = cicdJob.toBuilder().id(UUID.fromString(id)).build();
        return expected;
    }

    public static List<CICDJob> createCICDJobs(CiCdJobsDatabaseService ciCdJobsDatabaseService, String company, CICDInstance cicdInstance, int n) throws SQLException {
        List<CICDJob> cicdJobs = new ArrayList<>();
        for(int i=0; i < n; i++) {
            CICDJob cicdJob = createCICDJob(ciCdJobsDatabaseService, company, i, cicdInstance);
            cicdJobs.add(cicdJob);
        }
        return cicdJobs;
    }
}
