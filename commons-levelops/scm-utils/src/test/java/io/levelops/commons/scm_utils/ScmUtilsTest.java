package io.levelops.commons.scm_utils;


import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class ScmUtilsTest {
    private static final UUID JOB_ID = UUID.randomUUID();
    @Test
    public void testParseRepoIdFromScmUrl() {
        Assert.assertEquals(null, ScmUtils.parseRepoIdFromScmUrl(JOB_ID, null));
        Assert.assertEquals(null, ScmUtils.parseRepoIdFromScmUrl(JOB_ID, ""));
        Assert.assertEquals(null, ScmUtils.parseRepoIdFromScmUrl(JOB_ID, "   "));

        Assert.assertEquals("virajajgaonkar/leetcode", ScmUtils.parseRepoIdFromScmUrl(JOB_ID, "https://bitbucket.org/virajajgaonkar/leetcode.git"));
        Assert.assertEquals("virajajgaonkar/job-leetcode", ScmUtils.parseRepoIdFromScmUrl(JOB_ID, "git@bitbucket.org:virajajgaonkar/job-leetcode.git"));

        Assert.assertEquals("virajajgaonkar/leetcode.git", ScmUtils.parseRepoIdFromScmUrl(JOB_ID, "https://bitbucket.org/virajajgaonkar/leetcode.git.git"));
        Assert.assertEquals("virajajgaonkar/job-leetcode.git", ScmUtils.parseRepoIdFromScmUrl(JOB_ID, "git@bitbucket.org:virajajgaonkar/job-leetcode.git.git"));

        Assert.assertEquals("pr-demo-repo", ScmUtils.parseRepoIdFromScmUrl(JOB_ID, "https://dev.azure.com/cgn-test/project-test-11/_git/pr-demo-repo"));
        Assert.assertEquals("repo-test-7", ScmUtils.parseRepoIdFromScmUrl(JOB_ID, "https://dev.azure.com/cgn-test/project-test-7/_git/repo-test-7"));
    }
}