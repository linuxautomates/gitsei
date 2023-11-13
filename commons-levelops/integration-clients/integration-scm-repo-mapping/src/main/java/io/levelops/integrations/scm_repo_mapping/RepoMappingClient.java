package io.levelops.integrations.scm_repo_mapping;

import io.levelops.ingestion.models.IntegrationType;

import java.util.List;

public interface RepoMappingClient {
    String getIntegrationType();

    public List<String> getReposForUser(
            String tenantId,
            String integrationId,
            String username,
            List<String> orgs);
}
