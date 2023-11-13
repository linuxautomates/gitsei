package io.levelops.commons.databases.models.database.scm;

import io.levelops.integrations.bitbucket_server.models.BitbucketServerFile;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.time.Instant;

public class DbScmFileCommitTest {

    @Test
    public void fromBitbucketServerCommitFileTest(){

        BitbucketServerFile bitbucketServerFile = BitbucketServerFile.builder()
                .linesAdded(10)
                .linesRemoved(20)
                .build();
        DbScmFileCommit fileCommit = DbScmFileCommit.fromBitbucketServerCommitFile(bitbucketServerFile, "dummy-commit-sha1", Instant.now().getEpochSecond());
        Assertions.assertThat(fileCommit.getAddition()).isEqualTo(10);
        Assertions.assertThat(fileCommit.getDeletion()).isEqualTo(20);
        Assertions.assertThat(fileCommit.getChange()).isEqualTo(30);

        bitbucketServerFile = BitbucketServerFile.builder()
                .linesRemoved(20)
                .build();
        fileCommit = DbScmFileCommit.fromBitbucketServerCommitFile(bitbucketServerFile, "dummy-commit-sha1", Instant.now().getEpochSecond());
        Assertions.assertThat(fileCommit.getAddition()).isEqualTo(null);
        Assertions.assertThat(fileCommit.getDeletion()).isEqualTo(20);
        Assertions.assertThat(fileCommit.getChange()).isEqualTo(20);

        bitbucketServerFile = BitbucketServerFile.builder()
                .build();
        fileCommit = DbScmFileCommit.fromBitbucketServerCommitFile(bitbucketServerFile, "dummy-commit-sha1", Instant.now().getEpochSecond());
        Assertions.assertThat(fileCommit.getAddition()).isEqualTo(null);
        Assertions.assertThat(fileCommit.getDeletion()).isEqualTo(null);
        Assertions.assertThat(fileCommit.getChange()).isEqualTo(0);

    }
}
