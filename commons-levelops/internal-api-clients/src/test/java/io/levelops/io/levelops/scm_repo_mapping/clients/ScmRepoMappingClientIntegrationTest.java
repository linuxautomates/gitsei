package io.levelops.io.levelops.scm_repo_mapping.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.io.levelops.scm_repo_mapping.models.ScmRepoMappingResponse;
import okhttp3.OkHttpClient;
import org.junit.Test;

import static org.junit.Assert.*;

public class ScmRepoMappingClientIntegrationTest {
    @Test
    public void test() throws InternalApiClientException {
        OkHttpClient okHttpClient = new OkHttpClient();
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        ScmRepoMappingClient scmRepoMappingClient = new ScmRepoMappingClient(okHttpClient, objectMapper, "http://localhost:8080");
        var response = scmRepoMappingClient.getScmRepoMapping("sidofficial", "9");
        System.out.println(response);
    }
}