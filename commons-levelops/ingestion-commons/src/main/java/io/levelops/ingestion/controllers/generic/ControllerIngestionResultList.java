package io.levelops.ingestion.controllers.generic;

import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.MergeableIngestionResult;
import lombok.Getter;

import java.util.List;

public class ControllerIngestionResultList extends ListResponse<ControllerIngestionResult> implements ControllerIngestionResult, MergeableIngestionResult {

    private final String mergeStrategy;

    public ControllerIngestionResultList(String mergeStrategy, List<ControllerIngestionResult> records) {
        super(records);
        this.mergeStrategy = mergeStrategy;
    }

    public ControllerIngestionResultList(String mergeStrategy, ControllerIngestionResult... records) {
        this(mergeStrategy, List.of(records));
    }

    public ControllerIngestionResultList(List<ControllerIngestionResult> records) {
        this(null, records);
    }

    public ControllerIngestionResultList(ControllerIngestionResult... records) {
        this(null, records);
    }

    @Override
    public String getMergeStrategy() {
        return mergeStrategy;
    }

}
