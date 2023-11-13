package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.utils.ScmDiffParserUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ChangeVolumeStats;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.gitlab.models.GitlabChange;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabProject;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class ScmDiffParserUtilsTest {

    private static final ObjectMapper m = DefaultObjectMapper.get();

    @Test
    public void testGitlabDiffParser() {
        String diff = "@@ -0,0 +1 @@\n+This is a sample file \n";
        GitlabChange gitlabChange = GitlabChange.builder().diff(diff).oldPath("This.txt").newPath("This.txt").build();
        ChangeVolumeStats diffStats = ScmDiffParserUtils.fromGitlabDiff(gitlabChange);
        Assertions.assertThat(diffStats.getAdditions()).isEqualTo(1);
        Assertions.assertThat(diffStats.getDeletions()).isEqualTo(0);
        Assertions.assertThat(diffStats.getChanges()).isEqualTo(1);

        diff = "@@ -1,3 +1,7 @@\n run-test:\n   script:\n-    -echo \"Hello World\"\n+    - echo \"Hello World\"\n+\n+job1:\n+  script:\n+    - echo \"Hello from the 2nd method\"\n";
        gitlabChange = GitlabChange.builder().diff(diff).oldPath("This.txt").newPath("This.txt").build();
        diffStats = ScmDiffParserUtils.fromGitlabDiff(gitlabChange);
        Assertions.assertThat(diffStats.getAdditions()).isEqualTo(5);
        Assertions.assertThat(diffStats.getDeletions()).isEqualTo(1);
        Assertions.assertThat(diffStats.getChanges()).isEqualTo(6);

        diff = "--- a/doc/update/5.4-to-6.0.md\n+++ b/doc/update/5.4-to-6.0.md\n@@ -71,6 +71,8 @@\n sudo -u git -H bundle exec rake migrate_keys RAILS_ENV=production\n sudo -u git -H bundle exec rake migrate_inline_notes RAILS_ENV=production\n \n+sudo -u git -H bundle exec rake gitlab:assets:compile RAILS_ENV=production\n+\n ```\n \n ### 6. Update config files";
        gitlabChange = GitlabChange.builder().diff(diff).oldPath("This.txt").newPath("This.txt").build();
        diffStats = ScmDiffParserUtils.fromGitlabDiff(gitlabChange);
        Assertions.assertThat(diffStats.getAdditions()).isEqualTo(2);
        Assertions.assertThat(diffStats.getDeletions()).isEqualTo(0);
        Assertions.assertThat(diffStats.getChanges()).isEqualTo(2);
    }

    @Test
    public void test() throws IOException {
        String input = ResourceUtils.getResourceAsString("gitlab/gitlab_commits.json");
        PaginatedResponse<GitlabProject> response = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, GitlabProject.class));
        GitlabCommit commit = response.getResponse().getRecords().get(0).getCommits().get(0);
        DbScmCommit.fromGitlabCommit(commit, "my-project", "1");
        List<DbScmFile> dbScmFiles = DbScmFile.fromGitlabCommit(commit, "my-project", "1");
        DbScmFile dbScmFile = dbScmFiles.get(0);
        DbScmFileCommit dbScmFileCommit = dbScmFile.getFileCommits().get(0);
        Assertions.assertThat(dbScmFileCommit.getAddition()).isEqualTo(90);
        Assertions.assertThat(dbScmFileCommit.getDeletion()).isEqualTo(0);
        Assertions.assertThat(dbScmFileCommit.getChange()).isEqualTo(90);
    }
}
