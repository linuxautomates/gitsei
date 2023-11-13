package io.levelops.ingestion.sources;

import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.data.VoidQuery;
import io.levelops.ingestion.exceptions.FetchException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TransformationFilterTest {

    @Test
    public void test() throws FetchException {
        TransformationFilter<Integer, VoidQuery, String, VoidQuery> transformationFilter = TransformationFilter.<Integer, VoidQuery, String, VoidQuery>builder()
                .inputSource(TestDataSource.<Integer>builder()
                        .dataClass(Integer.class)
                        .dataEntries(BasicData.ofMany(Integer.class, 10, 20, 30))
                        .build())
                .transformData(integer -> BasicData.of(String.class, String.format("(%d)", integer.getPayload())))
                .build();
        assertThat(transformationFilter.fetchMany(null).map(Data::getPayload))
                .containsExactly("(10)", "(20)", "(30)");
    }
}