package io.levelops.api.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import lombok.Getter;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IngestedAtCachingService {

    @Getter
    private final LoadingCache<Pair<String, String>, Optional<Long>> ingestedAtCache;

    public IngestedAtCachingService(IntegrationTrackingService integrationTrackingService) {
        this.ingestedAtCache = initIngestedAtCache(integrationTrackingService);
    }

    private static LoadingCache<Pair<String, String>, Optional<Long>> initIngestedAtCache(final IntegrationTrackingService integrationTrackingService) {
        return CacheBuilder.from("maximumSize=1000,expireAfterWrite=15m").build(CacheLoader.from(pair -> {
            String company = pair.getFirst();
            String integrationId = pair.getSecond();
            return integrationTrackingService.get(company, integrationId)
                    .map(IntegrationTracker::getLatestIngestedAt);
        }));
    }

}
