package io.levelops.aggregations.controllers;

import io.levelops.ingestion.models.IntegrationType;

public interface AggregationsController<T> {
    public IntegrationType getIntegrationType();
    public Class<T> getMessageType();
    public String getSubscriptionName();
    public void doTask(T task);
}