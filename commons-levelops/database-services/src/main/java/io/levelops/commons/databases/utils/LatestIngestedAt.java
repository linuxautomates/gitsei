package io.levelops.commons.databases.utils;

import lombok.Builder;
import lombok.Value;

import javax.annotation.Nullable;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class LatestIngestedAt {
    @Nullable
    Long latestIngestedAt;
    @Nullable
    Map<String, Long> latestIngestedAtByIntegrationId;
}
