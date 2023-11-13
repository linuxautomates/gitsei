package io.levelops.api.model.dev_productivity;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
public class EffectiveOUs {
    private final List<UUID> ouIds;
    private final List<Integer> orgRefIds;
}
