package io.levelops.ingestion.services;

import io.levelops.ingestion.models.IntegrationType;
import lombok.Builder;
import lombok.Singular;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class IngestionServiceFactory {

    private final Map<IntegrationType, IngestionService> map;

    @Builder
    private IngestionServiceFactory(@Singular Set<IngestionService> ingestionServices) {
        map = new EnumMap<>(IntegrationType.class);
        ingestionServices.forEach(service -> map.put(service.getIntegrationType(), service));
    }

    public IngestionService get(IntegrationType integrationType) {
        return map.get(integrationType);
    }

}
