package io.levelops.integrations.github.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class GithubApiCommitTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testSerialization() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("github/api/github_pr_merge_commit.json");
        GithubApiCommit githubApiCommit = MAPPER.readValue(serialized, GithubApiCommit.class);
        Assert.assertEquals(2, githubApiCommit.getFiles().size());
        Assert.assertEquals("@@ -1508,4 +1508,11 @@ private static void assertCountAggResults(DbListResponse<DbAggregationResult> ag\n     }\n \n     // endregion\n+\n+    //Line 1\n+\n+\n+    //Line 2\n+\n+\n }", githubApiCommit.getFiles().get(0).getPatch());
        Assert.assertEquals("@@ -395,4 +395,7 @@ public void test2() throws SQLException, JsonProcessingException {\n         Assert.assertNotNull(views);\n \n     }\n+\n+    //Line 3\n+\n }\n\\ No newline at end of file", githubApiCommit.getFiles().get(1).getPatch());
    }
}