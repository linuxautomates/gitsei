package io.levelops.ingestion.data;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.functional.IngestionFailure;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DataCollector.DataCollectorBuilder.class)
public class DataCollector<T> {
    @Singular
    List<T> records;
    @Singular
    List<IngestionFailure> ingestionFailures; // non-critical failures that are ignored by the job
    Throwable error; // critical error that failed the job
    Map<String, Object> intermediateState; // to recover from a failure if possible

    long collectCount; // how many Data<T> were collected, for debugging purposes

    @Nonnull
    public static <T> DataCollector<T> collect(@Nullable Stream<Data<T>> stream) {
        DataCollectorBuilder<T> builder = DataCollector.<T>builder();
        if (stream == null) {
            return builder.build();
        }

        try (stream) {
            long count = stream
                    .filter(Objects::nonNull)
                    .peek(tData -> {
                        if (tData instanceof IngestionData) {
                            List<T> ingestionRecords = ((IngestionData<T>) tData).getRecords();
                            List<IngestionFailure> failures = ((IngestionData<T>) tData).getIngestionFailures();
                            if (ingestionRecords != null) {
                                builder.records(ingestionRecords);
                            }
                            if (failures != null) {
                                builder.ingestionFailures(failures);
                            }
                        } else if (tData instanceof FailedData) {
                            builder.error(((FailedData<T>) tData).getError());
                            builder.intermediateState(((FailedData<T>) tData).getIntermediateState());
                        } else {
                            if (tData.isPresent()) {
                                builder.record(tData.getPayload());
                            }
                        }
                    })
                    .takeWhile(FailedData::hasNotFailed)
                    .count();
            return builder.collectCount(count).build();
        }
    }

}
