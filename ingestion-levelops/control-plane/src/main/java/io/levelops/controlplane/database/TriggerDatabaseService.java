package io.levelops.controlplane.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.models.DbTriggerSettings;

import javax.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface TriggerDatabaseService extends DatabaseService<DbTrigger> {

    // region GET
    Stream<DbTrigger> streamTriggers(int pageSize);

    List<DbTrigger> getTriggers(int skip, int limit);

    List<DbTrigger> filter(int skip, int limit, @Nullable String tenantId, @Nullable String integrationId, @Nullable String triggerType);

    List<DbTrigger> getTriggersByIntegration(IntegrationKey integrationKey);

    List<DbTrigger> getTriggersByIntegrationAndType(IntegrationKey integrationKey, String triggerType);

    Optional<DbTrigger> getTriggerById(String triggerId);

    // endregion

    void createTrigger(UUID id, String tenantId, String integrationId, Boolean reserved, String triggerType, int frequency, String metadataJson, @Nullable String callbackUrl, @Nullable DbTriggerSettings settings) throws JsonProcessingException;

    boolean deleteTrigger(String triggerId);

    int deleteTriggers(String tenantId, String integrationId);

    boolean updateTriggerWithIteration(String triggerId, String iterationId, Instant iterationTs);

    boolean updateTriggerMetadata(String triggerId, Object metadata) throws JsonProcessingException;

    boolean updateTriggerFrequency(String triggerId, int frequency);
}
