package io.levelops.ingestion.controllers;

import java.util.Map;

public interface IntermediateStateUpdater {
    public void updateIntermediateState(Map<String, Object> intermediateState);
    public Map<String, Object> getIntermediateState();
}
