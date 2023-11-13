package io.levelops.commons.databases.models.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AcrossUniqueKey {
    String acrossKey;
    String acrossAdditionalKey;
    public static AcrossUniqueKey fromDbAggregationResultStacksWrapper(DbAggregationResultStacksWrapper input){
        return AcrossUniqueKey.builder().acrossKey(input.getAcrossKey()).acrossAdditionalKey(input.getAcrossAdditionalKey()).build();
    }
    public static AcrossUniqueKey fromDbAggregationResult(DbAggregationResult input){
        return AcrossUniqueKey.builder().acrossKey(input.getKey()).acrossAdditionalKey(input.getAdditionalKey()).build();
    }
}
