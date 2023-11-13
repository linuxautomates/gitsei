package io.levelops.commons.databases.models.database.organization;

import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class DBOrgProduct {
    UUID id;
    String name;
    String description;
    Set<Integ> integrations;

    @Value
    @Builder(toBuilder = true)
    public static class Integ{
        int integrationId;
        String name;
        String type;
        Map<String, Object> filters;
    }
}
