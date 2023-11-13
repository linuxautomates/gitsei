package io.levelops.integrations.bitbucket.models;

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

import static io.levelops.integrations.bitbucket.models.BitbucketUtils.BITBUCKET_DATE_FORMATTER;

public class BitbucketRepositoryTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException, ParseException {
        File testFile =  new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("bitbucket/BB_Api_List_Repos.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        BitbucketPaginatedResponse<BitbucketRepository> paginatedResponse = mapper.readValue(testFile, BitbucketPaginatedResponse.ofType(mapper, BitbucketRepository.class));
        Assert.assertNotNull(paginatedResponse);
        List<BitbucketRepository> bitbucketRepos = paginatedResponse.values;
        Assert.assertNotNull(bitbucketRepos);
        Assert.assertEquals(5, bitbucketRepos.size());

        BitbucketProject project = BitbucketProject.builder()
                .key("PROJ").type("project")
                .uuid("{6b467a88-7e8d-4cd7-aec8-4bae8eedc5f1}")
                .name("Project1").build();
        BitbucketRepository.Branch branch = BitbucketRepository.Branch.builder()
                .name("master").type("branch").build();
        BitbucketUser owner = BitbucketUser.builder()
                .username("levelopstest")
                .displayName("levelopstest")
                .type("team")
                .uuid("{82a01e22-1011-4ec6-90ba-d83f4eab9595}").build();
        BitbucketRepository apiRepo = BitbucketRepository.builder()
                .scm("git")
                .website(null)
                .hasWiki(false)
                .uuid("{65d194fb-d886-440a-ace0-f51916c69de2}")
                .forkPolicy("no_public_forks")
                .name("team-levelops-test-1")
                .project(project)
                .language("")
                .createdOn(BITBUCKET_DATE_FORMATTER.parse("2020-02-11T23:02:19.653082+00:00"))
                .createdOn(BITBUCKET_DATE_FORMATTER.parse("2020-02-11T23:02:19.653+00:00"))
                .mainbranch(branch)
                .fullName("levelopstest/team-levelops-test-1")
                .hasIssues(false)
                .owner(owner)
                .updatedOn(BITBUCKET_DATE_FORMATTER.parse("2020-02-12T21:08:25.037413+00:00"))
                .updatedOn(BITBUCKET_DATE_FORMATTER.parse("2020-02-12T21:08:25.037+00:00"))
                .size(178700L)
                .type("repository")
                .slug("team-levelops-test-1")
                .isPrivate(true)
                .description("")
                .links(BitbucketLinks.builder().html(BitbucketLinks.Link.builder().href("https://bitbucket.org/levelopstest/team-levelops-test-1").build()).build())
                .build();

        Assert.assertEquals(bitbucketRepos.get(0), apiRepo);
    }
}