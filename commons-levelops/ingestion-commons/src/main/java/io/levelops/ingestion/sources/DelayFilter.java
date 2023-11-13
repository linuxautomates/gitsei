package io.levelops.ingestion.sources;

import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.exceptions.FetchException;
import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Getter
@Builder
public class DelayFilter<D, Q extends DataQuery> implements DataFilter<D, Q, D, Q> {

    private final DataSource<D, Q> inputSource;
    private final int delay;
    private TimeUnit delayUnit;

    @Override
    public Data<D> fetchOne(Q key) throws FetchException {
        sleep();
        return inputSource.fetchOne(key);
    }

    @Override
    public Stream<Data<D>> fetchMany(Q query) throws FetchException {
        sleep();
        return inputSource.fetchMany(query);
    }

    private void sleep() {
        try {
            delayUnit.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
