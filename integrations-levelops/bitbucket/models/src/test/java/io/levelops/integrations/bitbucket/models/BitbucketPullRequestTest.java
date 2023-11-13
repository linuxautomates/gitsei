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

import static io.levelops.integrations.bitbucket.models.BitbucketUtils.BITBUCKET_DATE_FORMATTER;

public class BitbucketPullRequestTest  {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException, ParseException {
        File testFile =  new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("bitbucket/BB_Api_List_Repo_PRs.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        BitbucketPaginatedResponse<BitbucketPullRequest> paginatedResponse = mapper.readValue(testFile, BitbucketPaginatedResponse.ofType(mapper, BitbucketPullRequest.class));
        Assert.assertNotNull(paginatedResponse);
        List<BitbucketPullRequest> pullRequests = paginatedResponse.values;
        Assert.assertNotNull(pullRequests);
        Assert.assertEquals(5, pullRequests.size());

        BitbucketCommitRef destCommitRef = BitbucketCommitRef.builder()
                .hash("557fbcb8a1fc").type("commit").build();
        BitbucketRepoRef destRepoRef = BitbucketRepoRef.builder()
                .type("repository").name("team-levelops-test-1").fullName("levelopstest/team-levelops-test-1").uuid("{65d194fb-d886-440a-ace0-f51916c69de2}").build();
        BitbucketPullRequest.Ref dest = BitbucketPullRequest.Ref.builder()
                .commit(destCommitRef).repository(destRepoRef).branch(BitbucketPullRequest.Branch.builder().name("master").build()).build();

        BitbucketSummary summary = BitbucketSummary.builder()
                .raw("* PR Commit 1\r\n* PR Commit 2\r\n\r\n‌").markup("markdown").html("<ul>\n<li>PR Commit 1</li>\n<li>PR Commit 2</li>\n</ul>\n<p>‌</p>").type("rendered").build();

        BitbucketCommitRef srcCommitRef = BitbucketCommitRef.builder()
                .hash("4a16e7782e49").type("commit").build();
        BitbucketRepoRef srcRepoRef = BitbucketRepoRef.builder()
                .type("repository").name("team-levelops-test-1").fullName("levelopstest/team-levelops-test-1").uuid("{65d194fb-d886-440a-ace0-f51916c69de2}").build();
        BitbucketPullRequest.Ref src = BitbucketPullRequest.Ref.builder()
                .commit(srcCommitRef).repository(srcRepoRef).branch(BitbucketPullRequest.Branch.builder().name("pr-branch-1").build()).build();

        BitbucketUser author = BitbucketUser.builder()
                .displayName("App Account App Admin").uuid("{4e696f9a-19a3-4a33-b801-f28101d2542b}").nickname("App Account App Admin").type("user").accountId("5e41c580c8ec310c955a97b2")
                .build();

        BitbucketCommitRef mergeCommit = BitbucketCommitRef.builder()
                .hash("46418c4dead5").type("commit").build();

        BitbucketUser closedBy = BitbucketUser.builder()
                .displayName("App Account App Admin").uuid("{4e696f9a-19a3-4a33-b801-f28101d2542b}").nickname("App Account App Admin").type("user").accountId("5e41c580c8ec310c955a97b2")
                .build();

        /*
        {
          "role": "PARTICIPANT",
          "participated_on": "2020-02-12T00:38:01.958881+00:00",
          "type": "participant",
          "approved": true,
          "user": {
            "display_name": "App Account App Admin",
            "uuid": "{4e696f9a-19a3-4a33-b801-f28101d2542b}",
            "nickname": "App Account App Admin",
            "type": "user",
            "account_id": "5e41c580c8ec310c955a97b2"
          }
        }
         */

        BitbucketPullRequest.Participant participant = BitbucketPullRequest.Participant.builder()
                .role("PARTICIPANT").participatedOn("2020-02-12T00:38:01.958881+00:00").type("participant").approved(true).user(closedBy).build();

        BitbucketPullRequest apiPr = BitbucketPullRequest.builder()
                .description("* PR Commit 1\r\n* PR Commit 2\r\n\r\n‌")
                .title("Pr branch 1")
                .closeSourceBranch(true)
                .type("pullrequest")
                .id(1)
                .destination(dest)
                .createdOn(BITBUCKET_DATE_FORMATTER.parse("2020-02-12T00:37:38.433950+00:00"))
                .createdOn(BITBUCKET_DATE_FORMATTER.parse("2020-02-12T00:37:38.433+00:00"))
                .summary(summary)
                .source(src)
                .commentCount(0)
                .state("MERGED")
                .taskCount(0)
                .reason("")
                .updatedOn(BITBUCKET_DATE_FORMATTER.parse("2020-02-12T00:38:43.411199+00:00"))
                .updatedOn(BITBUCKET_DATE_FORMATTER.parse("2020-02-12T00:38:43.411+00:00"))
                .author(author)
                .mergeCommit(mergeCommit)
                .closedBy(closedBy)
                .participants(Arrays.asList(participant))
                .build();

        Assert.assertEquals(pullRequests.get(pullRequests.size()-1), apiPr);
    }

}