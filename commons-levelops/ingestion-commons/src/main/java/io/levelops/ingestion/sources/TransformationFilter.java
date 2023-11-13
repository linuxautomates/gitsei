package io.levelops.ingestion.sources;

import io.levelops.commons.functional.FunctionUtils;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.exceptions.FetchException;
import lombok.Builder;
import lombok.Getter;

import java.util.function.Function;
import java.util.stream.Stream;

@Getter
public class TransformationFilter<D1, Q1 extends DataQuery, D2, Q2 extends DataQuery> implements DataFilter<D1, Q1, D2, Q2> {

    private final DataSource<D1, Q1> inputSource;
    private final Function<Data<D1>, Data<D2>> transformData;
    private final Function<Q2, Q1> transformQuery;

    @Builder
    protected TransformationFilter(DataSource<D1, Q1> inputSource, Function<Data<D1>, Data<D2>> transformData, Function<Q2, Q1> transformQuery) {
        this.inputSource = inputSource;
        this.transformData = (transformData != null)? transformData : FunctionUtils.identity();
        this.transformQuery = (transformQuery != null)? transformQuery : FunctionUtils.identity();
    }

    @Override
    public Data<D2> fetchOne(Q2 key) throws FetchException {
        return transformData.apply(inputSource.fetchOne(transformQuery.apply(key)));
    }

    @Override
    public Stream<Data<D2>> fetchMany(Q2 query) throws FetchException {
        return inputSource.fetchMany(transformQuery.apply(query)).map(transformData);
    }
}
