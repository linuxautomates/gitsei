package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobRunDetails;
import org.junit.Assert;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CiCdJobRunDetailsUtils {
    public static CICDJobRunDetails createCICDJobRunDetails(CiCdJobRunDetailsDatabaseService ciCdJobRunDetailsDatabaseService, CICDJobRun cicdJobRun, String company, int i) throws SQLException {
        CICDJobRunDetails.CICDJobRunDetailsBuilder bldr = CICDJobRunDetails.builder()
                .cicdJobRunId(cicdJobRun.getId())
                .gcsPath("gcs-path-" + i);
        CICDJobRunDetails cicdJobRunDetails = bldr.build();
        String id = ciCdJobRunDetailsDatabaseService.insert(company, cicdJobRunDetails);
        Assert.assertNotNull(id);
        CICDJobRunDetails expected = cicdJobRunDetails.toBuilder().id(UUID.fromString(id)).build();
        return expected;
    }

    public static List<CICDJobRunDetails> createCICDJobRunDetailsList(CiCdJobRunDetailsDatabaseService ciCdJobRunDetailsDatabaseService, CICDJobRun cicdJobRun, String company, int n) throws SQLException {
        List<CICDJobRunDetails> ciCdJobRunDetailsList = new ArrayList<>();
        for(int i =0; i < n; i++){
            CICDJobRunDetails cicdJobRunDetails = createCICDJobRunDetails(ciCdJobRunDetailsDatabaseService, cicdJobRun, company, i);
            ciCdJobRunDetailsList.add(cicdJobRunDetails);
        }
        return ciCdJobRunDetailsList;
    }
}
