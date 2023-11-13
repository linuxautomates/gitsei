package io.levelops.ingestion.merging;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.MapUtils;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.merging.strategies.StorageResultsListMergingStrategy;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class IngestionResultMergingServiceTest {

    IngestionResultMergingService ingestionResultMergingService = new IngestionResultMergingService();

    @Test
    public void test() throws IOException {
        String aJson = ResourceUtils.getResourceAsString("merging/a.json");
        String bJson = ResourceUtils.getResourceAsString("merging/b.json");
        String mergedJson = ResourceUtils.getResourceAsString("merging/merged.json");
        Map<String, Object> a = ParsingUtils.parseJsonObject(DefaultObjectMapper.get(), "a", aJson);
        Map<String, Object> b = ParsingUtils.parseJsonObject(DefaultObjectMapper.get(), "b", bJson);
        Map<String, Object> merged = ParsingUtils.parseJsonObject(DefaultObjectMapper.get(), "merged", mergedJson);

        Map<String, Object> output = ingestionResultMergingService.merge(a, b);
        assertThat(output).isEqualToComparingFieldByField(b);

        b = MapUtils.append(b, "merge_strategy", StorageResultsListMergingStrategy.NAME);
        output = ingestionResultMergingService.merge(a, b);
        DefaultObjectMapper.prettyPrint(output);
        assertThat(output).isEqualTo(merged);

    }
}