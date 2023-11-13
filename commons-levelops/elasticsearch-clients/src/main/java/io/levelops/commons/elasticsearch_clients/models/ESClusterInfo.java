package io.levelops.commons.elasticsearch_clients.models;


import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class ESClusterInfo {
    private final String name;

    private final List<String> ipAddresses;
    private final Integer port;

    private final String userName;
    private final String password;
    private final String sslCertPath;

    private final boolean defaultCluster;
    private final List<String> tenants;
}
