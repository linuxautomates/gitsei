package io.levelops.aggregations_shared.helpers;

import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;

import java.util.List;
import java.util.Set;

public class ScmAggUtils {

    public static boolean isChangeVolumeLessThanXLines(DbScmCommit dbScmCommit, int lines) {
        return dbScmCommit.getAdditions() + dbScmCommit.getDeletions() + dbScmCommit.getChanges() < lines;
    }

    public static boolean isRelevant(String customer, Set<String> RELEVANT_TENANT_IDS) {
        return RELEVANT_TENANT_IDS.contains(customer);
    }

    public static boolean useScmCommitsInsertV2(final List<IntegrationWhitelistEntry> scmCommitsInsertV2integrationIdWhitelist, String tenantId, String integrationId){
        return scmCommitsInsertV2integrationIdWhitelist.contains(IntegrationWhitelistEntry.builder()
                .tenantId(tenantId)
                .integrationId(integrationId)
                .build());
    }
}

