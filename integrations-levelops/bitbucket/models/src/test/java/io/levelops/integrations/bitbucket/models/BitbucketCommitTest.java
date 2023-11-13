package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.levelops.integrations.bitbucket.models.BitbucketUtils.BITBUCKET_COMMIT_DATE_FORMATTER;

public class BitbucketCommitTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException, ParseException {
        File testFile =  new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("bitbucket/BB_Api_List_Repo_Commits.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        BitbucketPaginatedResponse<BitbucketCommit> paginatedResponse = mapper.readValue(testFile, BitbucketPaginatedResponse.ofType(mapper, BitbucketCommit.class));
        Assert.assertNotNull(paginatedResponse);
        List<BitbucketCommit> commits = paginatedResponse.values;
        Assert.assertNotNull(commits);
        Assert.assertEquals(10, commits.size());

        BitbucketSummary message = BitbucketSummary.builder()
                .raw("PR Commit 9\n").markup("markdown").html("<p>PR Commit 9</p>").type("rendered").build();
        BitbucketRepoRef repoRef = BitbucketRepoRef.builder()
                .type("repository")
                .name("team-levelops-test-1")
                .fullName("levelopstest/team-levelops-test-1")
                .uuid("{65d194fb-d886-440a-ace0-f51916c69de2}").build();
        BitbucketCommit.CommitAuthor author = BitbucketCommit.CommitAuthor.builder()
                .raw("Viraj Ajgaonkar <viraj@levelops.io>").type("author").build();
        BitbucketSummary summary = BitbucketSummary.builder()
                .raw("PR Commit 9\n").markup("markdown").html("<p>PR Commit 9</p>").type("rendered").build();

        List<BitbucketCommitRef> parents = Arrays.asList(BitbucketCommitRef.builder().hash("7af92a074a798c8a82f901dd66caa64ce783be63").type("commit").build());

        BitbucketCommit apiCommit = BitbucketCommit.builder()
                .rendered(BitbucketCommit.Rendered.builder().message(message).build())
                .hash("f2d70bbcf95c2c366379b7003ec5b1300b609ba0")
                .repository(repoRef)
                .author(author)
                .summary(summary)
                .parents(parents)
                .date(BITBUCKET_COMMIT_DATE_FORMATTER.parse("2020-02-12T21:08:12+00:00"))
                .message("PR Commit 9\n")
                .type("commit")
                .links(BitbucketLinks.builder().html(BitbucketLinks.Link.builder().href("https://bitbucket.org/levelopstest/team-levelops-test-1/commits/f2d70bbcf95c2c366379b7003ec5b1300b609ba0").build()).build())
                .build();

        Assert.assertEquals(commits.get(0), apiCommit);
    }
}