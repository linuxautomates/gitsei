package io.levelops.integrations.github.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubTag;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class GithubClientTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSanitize() {
        assertThat(GithubClient.sanitizeUrl(null)).isEqualTo("https://api.github.com/");
        assertThat(GithubClient.sanitizeUrl("   ")).isEqualTo("https://api.github.com/");
        assertThat(GithubClient.sanitizeUrl("http://api.github.com")).isEqualTo("http://api.github.com");
        assertThat(GithubClient.sanitizeUrl(" https://api.github.com/ ")).isEqualTo("https://api.github.com/");
        assertThat(GithubClient.sanitizeUrl("api.github.com")).isEqualTo("https://api.github.com");

        assertThat(GithubClient.sanitizeUrl("http://1.2.3.4:8080")).isEqualTo("http://1.2.3.4:8080");
        assertThat(GithubClient.sanitizeUrl("https://1.2.3.4:8080")).isEqualTo("https://1.2.3.4:8080");
        assertThat(GithubClient.sanitizeUrl("1.2.3.4:8080")).isEqualTo("https://1.2.3.4:8080");
    }

    @Test
    public void testBaseUrl() {
        assertThat(GithubClient.baseUrlBuilder("https://api.github.com/").build().toString()).isEqualTo("https://api.github.com/");
        assertThat(GithubClient.baseUrlBuilder("https://api.github.com").build().toString()).isEqualTo("https://api.github.com/");
        assertThat(GithubClient.baseUrlBuilder("http://api.github.com").build().toString()).isEqualTo("https://api.github.com/");
        assertThat(GithubClient.baseUrlBuilder("http://api.github.com/a/b/c").build().toString()).isEqualTo("https://api.github.com/");
        assertThat(GithubClient.baseUrlBuilder("http://api.github.com/api/v3").build().toString()).isEqualTo("https://api.github.com/");

        assertThat(GithubClient.baseUrlBuilder("http://corp.com").build().toString()).isEqualTo("http://corp.com/api/v3");
        assertThat(GithubClient.baseUrlBuilder("http://1.2.3.4:8080").build().toString()).isEqualTo("http://1.2.3.4:8080/api/v3");
        assertThat(GithubClient.baseUrlBuilder("https://1.2.3.4:8080/api/v3").build().toString()).isEqualTo("https://1.2.3.4:8080/api/v3");
        assertThat(GithubClient.baseUrlBuilder("https://1.2.3.4:8080/a").build().toString()).isEqualTo("https://1.2.3.4:8080/a/api/v3");
        assertThat(GithubClient.baseUrlBuilder("https://1.2.3.4:8080/api/v3/b/c/api").build().toString()).isEqualTo("https://1.2.3.4:8080/api/v3/b/c/api/api/v3");
    }

    @Test
    public void testGetTags() throws IOException {
        String data = ResourceUtils.getResourceAsString("github/api/github_tag.json");
        DefaultObjectMapper.prettyPrint(data);
        GithubTag githubTag = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GithubTag.class));
        Assert.assertEquals("seecond", githubTag.getName());
        Assert.assertEquals("9b6b582296133c6c99983143bac444429947ddcd", githubTag.getCommit().getSha());
        final String commitUrl = "https://api.github.com/repos/gch71/Testing1/commits/9b6b582296133c6c99983143bac444429947ddcd";
        Assert.assertEquals(commitUrl, githubTag.getCommit().getUrl());
        Assert.assertEquals("REF_kwDOGQKIu7FyZWZzL3RhZ3Mvc2VlY29uZA", githubTag.getNodeId());
    }
}