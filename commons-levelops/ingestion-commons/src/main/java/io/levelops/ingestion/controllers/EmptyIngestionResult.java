package io.levelops.ingestion.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Special Controller Ingestion Result that is used to flag emtpy results.
 * Jobs with empty results will be omitted by the Control Plane when returning trigger results.
 */
@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class EmptyIngestionResult implements ControllerIngestionResult {

    @JsonProperty("empty")
    private final boolean empty = true;

    public static boolean isEmpty(@Nullable Object controllerIngestionResult) {
        if (controllerIngestionResult == null) {
            return true;
        }
        if (controllerIngestionResult instanceof EmptyIngestionResult) {
            return true;
        }
        if (controllerIngestionResult instanceof Map) {
            Map map = (Map) controllerIngestionResult;
            return map.size() == 1 && Boolean.TRUE.equals(map.get("empty"));
        }
        return false;
    }

    public static boolean isNotEmpty(@Nullable Object controllerIngestionResult) {
        return !isEmpty(controllerIngestionResult);
    }
}
