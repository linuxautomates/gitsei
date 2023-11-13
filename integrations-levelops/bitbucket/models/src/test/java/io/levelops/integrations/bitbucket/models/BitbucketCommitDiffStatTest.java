package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

public class BitbucketCommitDiffStatTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException {
        File testFile =  new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("bitbucket/BB_Api_List_Commit_DiffStat.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        BitbucketPaginatedResponse<BitbucketCommitDiffStat> paginatedResponse = mapper.readValue(testFile, BitbucketPaginatedResponse.ofType(mapper, BitbucketCommitDiffStat.class));
        Assert.assertNotNull(paginatedResponse);
        List<BitbucketCommitDiffStat> diffSets = paginatedResponse.values;
        Assert.assertNotNull(diffSets);
        Assert.assertEquals(1, diffSets.size());

        BitbucketCommitDiffStat apiDiffSet = BitbucketCommitDiffStat.builder()
                .status("modified")
                .linesAdded(1)
                .linesRemoved(0)
                .type("diffstat")
                .oldFile(BitbucketCommitDiffStat.FileRef.builder().path("README.md").type("commit_file").build())
                .newFile(BitbucketCommitDiffStat.FileRef.builder().path("README.md").type("commit_file").build())
                .build();

        Assert.assertEquals(diffSets.get(0), apiDiffSet);
    }
}