package io.levelops.commons.etl.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum JobPriority {
    HIGH(1),
    MEDIUM(2),
    LOW(3);

    private final int priority;

    public static JobPriority fromInteger(int priority) {
        switch (priority) {
            case 1:
                return JobPriority.HIGH;
            case 2:
                return JobPriority.MEDIUM;
            case 3:
                return JobPriority.LOW;
            default:
                throw new IllegalArgumentException("Priority is out of bounds: " + priority);
        }
    }
}
