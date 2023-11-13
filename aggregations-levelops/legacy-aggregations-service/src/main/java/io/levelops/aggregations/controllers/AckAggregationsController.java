package io.levelops.aggregations.controllers;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import io.levelops.ingestion.models.IntegrationType;

public interface AckAggregationsController<T> {
    IntegrationType getIntegrationType();

    Class<T> getMessageType();

    String getSubscriptionName();

    void doTask(T task, AckReplyConsumer consumer);
}
