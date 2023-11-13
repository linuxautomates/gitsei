package io.levelops.integrations.scm_repo_mapping;

import java.util.HashMap;
import java.util.Set;

public class RepoMappingClientRegistry {
    private final HashMap<String, RepoMappingClient> clientMap;

    public RepoMappingClientRegistry() {
        clientMap = new HashMap<>();
    }

    public void registerClient(String integrationType, RepoMappingClient client) {
        clientMap.put(integrationType, client);
    }

    public RepoMappingClient getClient(String integrationType) {
        return clientMap.get(integrationType);
    }

    public Set<String> getSupportedApps() {
        return clientMap.keySet();
    }
}
