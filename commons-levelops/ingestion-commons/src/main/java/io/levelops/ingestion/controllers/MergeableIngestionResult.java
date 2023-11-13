package io.levelops.ingestion.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface MergeableIngestionResult {

    @JsonProperty("merge_strategy")
    String getMergeStrategy();

}
