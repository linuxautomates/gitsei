package io.levelops.ingestion.data;

import io.levelops.commons.functional.IngestionFailure;
import io.levelops.commons.utils.ListUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.function.Function;

@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class IngestionData<T> implements Data<T>  {
    Class<T> dataClass;
    T payload;
    List<T> records;
    List<IngestionFailure> ingestionFailures;

    @Override
    public boolean isPresent() {
        return CollectionUtils.isNotEmpty(records) || CollectionUtils.isNotEmpty(ingestionFailures);
    }

    public static <T> IngestionData<T> of(Class<T> dataClass, List<T> records, List<IngestionFailure> ingestionFailures) {
        return new IngestionData<>(dataClass, null, records, ingestionFailures);
    }
}
