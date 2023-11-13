package io.levelops.commons.databases.models.database.mappings;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class ComponentProductMapping {
    private String componentType;
    private UUID componentId;
    private List<Integer> productIds;

    @Value
    @Builder(toBuilder = true)
    public static class Key {
        private String componentType;
        private UUID componentId;
        private Integer productId;
    }
}