package io.levelops.ingestion.sources;

import io.levelops.ingestion.models.DataQuery;

public interface DataFilter<D1, Q1 extends DataQuery, D2, Q2 extends DataQuery> extends DataSource<D2, Q2> {

    DataSource<D1, Q1> getInputSource();

}
