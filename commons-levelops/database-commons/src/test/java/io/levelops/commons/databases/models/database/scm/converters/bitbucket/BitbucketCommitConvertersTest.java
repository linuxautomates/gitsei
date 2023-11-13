package io.levelops.commons.databases.models.database.scm.converters.bitbucket;

import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BitbucketCommitConvertersTest {

    @Test
    public void testFromBitbucketCommit() throws IOException {
        BitbucketCommit bitbucketCommit = ResourceUtils.getResourceAsObject("bitbucket/bitbucket_commit.json", BitbucketCommit.class);
        DbScmCommit dbScmCommit = BitbucketCommitConverters
                .fromBitbucketCommit(bitbucketCommit, "repo_1", "project_1", "1", 1619531929L);
        DefaultObjectMapper.prettyPrint(dbScmCommit);

        DbScmCommit expected = ResourceUtils.getResourceAsObject("bitbucket/bitbucket_commit_expected.json", DbScmCommit.class);
        assertThat(dbScmCommit).isEqualTo(expected);
    }

}