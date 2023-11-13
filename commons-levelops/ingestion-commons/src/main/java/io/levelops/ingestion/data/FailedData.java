package io.levelops.ingestion.data;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * This class is used as a data placeholder that represents a failure
 * that can be retried and resumed from an intermediate state.
 *
 * FailedData wraps the actual exception and allows the data stream to not be interrupted,
 * so that the partial data is stored
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@ToString(callSuper = true)
public class FailedData<T> implements Data<T> {

    Class<T> dataClass;
    Throwable error;
    Map<String, Object> intermediateState;

    @Override
    public T getPayload() {
        return null;
    }

    @Override
    public boolean isPresent() {
        return false;
    }

    public static <T> boolean hasFailed(Data<T> data) {
        return data == null || (data instanceof FailedData);
    }

    public static <T> boolean hasNotFailed(Data<T> data) {
        return !hasFailed(data);
    }

    public static <T> FailedData<T> of(Class<T> dataClass, Throwable error, Map<String, Object> intermediateState) {
        return new FailedData<>(dataClass, error, intermediateState);
    }

    public static <T, U> FailedData<T> of(Class<T> dataClass, Throwable error, U intermediateState) {
        return new FailedData<>(dataClass, error, ParsingUtils.toJsonObject(DefaultObjectMapper.get(), intermediateState));
    }

}
