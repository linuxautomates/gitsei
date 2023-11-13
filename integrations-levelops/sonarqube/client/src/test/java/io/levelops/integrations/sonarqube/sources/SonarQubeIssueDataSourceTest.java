package io.levelops.integrations.sonarqube.sources;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarQubeIssueDataSourceTest {
    @Test
    public void testChunkCollection() {
        List<Integer> list = List.of(1,2,3,4,5,6,7,8,9,10);
        var chunked = SonarQubeIssueDataSource.chunkCollection(list, 3);
        assertThat(chunked).containsExactly(List.of(1,2,3), List.of(4,5,6), List.of(7,8,9), List.of(10));
    }

}