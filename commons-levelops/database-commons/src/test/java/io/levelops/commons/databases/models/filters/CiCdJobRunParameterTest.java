package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CiCdJobRunParameterTest  {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testSerialization() throws JsonProcessingException {
        String serialized = "[{\"name\":\"region\",\"values\":[\"us1\",\"us2\"]},{\"name\":\"env\",\"values\":[\"PROD\",\"PRODEU\"]}]";
        List<CiCdJobRunParameter> parameters = MAPPER.readValue(serialized, MAPPER.getTypeFactory().constructCollectionType(List.class, CiCdJobRunParameter.class));
        Assert.assertNotNull(parameters);
        Assert.assertEquals(2, parameters.size());
        List<CiCdJobRunParameter> expected = List.of(
                CiCdJobRunParameter.builder().name("region").values(List.of("us1", "us2")).build(),
                CiCdJobRunParameter.builder().name("env").values(List.of("PROD", "PRODEU")).build()
        );
        Assert.assertEquals(parameters, expected);
    }
}