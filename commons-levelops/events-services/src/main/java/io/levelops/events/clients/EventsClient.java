package io.levelops.events.clients;

import io.levelops.commons.databases.models.database.Event;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.events.models.EventsClientException;

import java.util.List;
import java.util.Map;

public interface EventsClient {
    public void emitEvent(final String company, final EventType type, final Map<String, Object> eventData) throws EventsClientException;
    public List<String> processTriggerEvent(final Event triggerEvent) throws EventsClientException;
}