package io.levelops.commons.models;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
public class RegexResult {
    Integer firstHitLineNumber;
    String firstHitContext;
    Map<String, Integer> regexCount;
    Integer totalMatches;
}
