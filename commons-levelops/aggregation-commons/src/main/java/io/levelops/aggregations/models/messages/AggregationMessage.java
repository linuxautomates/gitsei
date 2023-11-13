package io.levelops.aggregations.models.messages;

public interface AggregationMessage {
    public String getMessageId();

    public String getCustomer();

    public String getOutputBucket();
}