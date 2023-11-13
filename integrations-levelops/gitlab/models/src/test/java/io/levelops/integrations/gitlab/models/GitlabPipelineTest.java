package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;



public class GitlabPipelineTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException, ParseException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().
                getResource("gitlab/gitlab_api_pipeline.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<GitlabPipeline> response = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, GitlabPipeline.class));
        Assert.assertNotNull(response);
        Assert.assertEquals(2, response.size());
        GitlabPipeline apiRes = GitlabPipeline.builder()
                .pipelineId("47").status("pending").ref("new-pipeline")
                .sha("a91957a858320c0e17f3a0eca7cfacbff50ea29a")
                .webUrl("https://example.com/foo/bar/pipelines/47")
                .createdAt(GitlabUtils.buildDateFormatter().parse("2016-08-11T11:28:34.085Z"))
                .updatedAt(GitlabUtils.buildDateFormatter().parse("2016-08-11T11:32:35.169Z"))
                .build();
        Assert.assertEquals(response.get(0), apiRes);
    }
}
