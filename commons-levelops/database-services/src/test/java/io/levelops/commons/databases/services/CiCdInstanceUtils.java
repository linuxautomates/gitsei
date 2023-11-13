package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.filters.CICD_TYPE;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CiCdInstanceUtils {
    @Deprecated
    public static CICDInstance createCiCdInstance(CiCdInstancesDatabaseService ciCdInstancesDatabaseService, final String company, int i) throws SQLException {
        return createCiCdInstance(ciCdInstancesDatabaseService, company, "1", i);
    }
    private static CICDInstance createCiCdInstance(CiCdInstancesDatabaseService ciCdInstancesDatabaseService, final String company, String integrationId, int i) throws SQLException {
        CICDInstance.CICDInstanceBuilder bldr = CICDInstance.builder()
                .id(UUID.randomUUID())
                .name("instance-name-" + i)
                .url("https://jenkins.dev.levelops.io/")
                .integrationId(integrationId)
                .type(CICD_TYPE.jenkins.toString());
        CICDInstance cicdInstance = bldr.build();
        String id = ciCdInstancesDatabaseService.insert(company,cicdInstance);
        return cicdInstance.toBuilder().id(UUID.fromString(id)).build();
    }
    public static CICDInstance createCiCdInstance(CiCdInstancesDatabaseService ciCdInstancesDatabaseService, final String company, Integration integration, int i) throws SQLException {
        return createCiCdInstance(ciCdInstancesDatabaseService, company, integration.getId(), i);
    }

    public static List<CICDInstance> createCiCdInstances(CiCdInstancesDatabaseService ciCdInstancesDatabaseService, final String company, Integration integration, int n) throws SQLException {
        List<CICDInstance> cicdInstances = new ArrayList<>();
        for(int i=0; i<n; i++){
            CICDInstance instance = createCiCdInstance(ciCdInstancesDatabaseService, company, integration, i);
            cicdInstances.add(instance);
        }
        return cicdInstances;
    }

    public static List<UUID> generateCiCdInstanceGuids(int n){
        List<UUID> cicdInstanceGuids = new ArrayList<>();
        for(int i=0; i<n; i++){
            cicdInstanceGuids.add(UUID.randomUUID());
        }
        return cicdInstanceGuids;
    }
}
