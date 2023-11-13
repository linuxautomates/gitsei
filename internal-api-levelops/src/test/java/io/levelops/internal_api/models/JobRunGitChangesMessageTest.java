package io.levelops.internal_api.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class JobRunGitChangesMessageTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testDeSerialize() throws JsonProcessingException {
        String serialized = "{\"build_number\":61,\"commit_ids\":[\"4a4353c6d9d471826328b298fe3042c28bbe318d\",\"cc6dfc65066fd69fb065b62b734cc7c960a46e75\",\"cf64153e237ee3a34ce91989accfdafb2323e7a6\",\"3a2eeee9bf5ba2938e7aa9c861eb0611bb4056eb\",\"600b571966defafb1d124db2b4d85cae22a20f10\",\"52e295e34a16c119c403a0130fa0093fc31f5371\",\"81b426e034d67ead6db7689e694b450baf14a114\",\"2d44ef083cf86190ef0597d705edbf02e9635bc0\",\"a7c7ecbdc554fa6cf083d604582743b152180208\",\"e667282899c3e3913f707277699de633f738e957\",\"7856900458ea5d5cde55d8bc5a8266470d6a2517\",\"e57c62527ea8e28cadc23c38e9cfb69820b9e8d5\"]}";
        JobRunGitChangesMessage actual = MAPPER.readValue(serialized, JobRunGitChangesMessage.class);
        Assert.assertNotNull(actual);
        Assert.assertEquals(61, actual.getBuildNumber().intValue());
        Assert.assertEquals(12, actual.getCommitIds().size());
    }
}