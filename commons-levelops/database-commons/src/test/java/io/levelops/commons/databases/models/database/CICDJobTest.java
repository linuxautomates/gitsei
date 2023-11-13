package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class CICDJobTest {
    private final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerializeDeserialize() throws JsonProcessingException {
        CICDJob expected = CICDJob.builder()
                .id(UUID.randomUUID())
                .jobName("pipeline-1")
                .jobFullName("pipeline-1/branches/master")
                .jobNormalizedFullName("pipeline-1/master")
                .scmUrl("https://github.com/TechPrimers/jenkins-example.git")
                .scmUserId(null)
                .build();
        String serialized = MAPPER.writeValueAsString(expected);
        Assert.assertNotNull(serialized);
        CICDJob actual = MAPPER.readValue(serialized, CICDJob.class);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSanitizeScmUrl() {
        Assert.assertEquals(null, CICDJob.sanitizeScmUrl(null));
        Assert.assertEquals("", CICDJob.sanitizeScmUrl(""));

        Assert.assertEquals("git@bitbucket.org:shoppertrak/job-tracker.git", CICDJob.sanitizeScmUrl("git@bitbucket.org:shoppertrak/job-tracker.git"));
        Assert.assertEquals("git@bitbucket.org:shoppertrak/job-tracker.git", CICDJob.sanitizeScmUrl("git@bitbucket.org:shoppertrak/job-tracker.git.git"));

        Assert.assertEquals("https://bitbucket.org/virajajgaonkar/leetcode.git", CICDJob.sanitizeScmUrl("https://bitbucket.org/virajajgaonkar/leetcode.git"));
        Assert.assertEquals("https://bitbucket.org/virajajgaonkar/leetcode.git", CICDJob.sanitizeScmUrl("https://bitbucket.org/virajajgaonkar/leetcode.git.git"));

        Assert.assertEquals("git@bitbucket.git.git:shoppertrak/job-tracker.git", CICDJob.sanitizeScmUrl("git@bitbucket.git.git:shoppertrak/job-tracker.git.git"));
        Assert.assertEquals("git@bitbucket.git.git:shoppertrak/job-tracker.git", CICDJob.sanitizeScmUrl("git@bitbucket.git.git:shoppertrak/job-tracker.git"));

        Assert.assertEquals("https://bitbucket.git.git/virajajgaonkar/leetcode.git", CICDJob.sanitizeScmUrl("https://bitbucket.git.git/virajajgaonkar/leetcode.git"));
        Assert.assertEquals("https://bitbucket.git.git/virajajgaonkar/leetcode.git", CICDJob.sanitizeScmUrl("https://bitbucket.git.git/virajajgaonkar/leetcode.git.git"));
    }
}