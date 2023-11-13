package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class StoredFilterTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialization() throws IOException {
        String data = ResourceUtils.getResourceAsString("triage/triage_filter.json");
        StoredFilter storedFilter = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(StoredFilter.class));
        Assert.assertNotNull(storedFilter);
        Assert.assertEquals("078e82be-2c3b-47ba-855d-f62270c67e7e", storedFilter.getId());
        Assert.assertEquals("grid-view", storedFilter.getName());
        Assert.assertEquals(Map.of(
                "job_ids", List.of("8490cced-a254-45ba-866a-38b4381e8ea3", "3ce3a438-d0a9-4d6b-95f2-85d8366930fd"),
                "results", List.of("SUCCEEDED", "ABORTED")
        ), storedFilter.getFilter());
        Assert.assertEquals(Long.valueOf("1622817057"), storedFilter.getCreatedAt());
        Assert.assertEquals(Long.valueOf("1622817057"), storedFilter.getUpdatedAt());
    }

    @Test
    public void testPartialSerialization() throws IOException {
        String data = ResourceUtils.getResourceAsString("triage/triage_filter_partial.json");
        StoredFilter storedFilter = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(StoredFilter.class));
        Assert.assertNotNull(storedFilter);
        Assert.assertEquals("grid-view", storedFilter.getName());
        Assert.assertEquals(Map.of(
                "job_ids", List.of("8490cced-a254-45ba-866a-38b4381e8ea3", "3ce3a438-d0a9-4d6b-95f2-85d8366930fd"),
                "results", List.of("SUCCEEDED", "ABORTED")
        ), storedFilter.getFilter());
    }

}
