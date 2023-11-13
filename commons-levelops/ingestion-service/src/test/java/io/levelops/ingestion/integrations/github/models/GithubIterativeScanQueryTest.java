package io.levelops.ingestion.integrations.github.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class GithubIterativeScanQueryTest {
    @Test
    public void test() throws JsonProcessingException {
        String data = "{\"to\": \"2022-05-03T00:00:00+0000\", \"from\": \"2022-04-01T00:00:00+0000\", \"integration_key\": {\"tenant_id\": \"ibotta\", \"integration_id\": \"3\"}, \"should_fetch_tags\": false, \"should_fetch_repos\": true, \"should_fetch_all_cards\": false}";
        GithubIterativeScanQuery q = DefaultObjectMapper.get().readValue(data, GithubIterativeScanQuery.class);
        Assert.assertNotNull(q);
    }

}