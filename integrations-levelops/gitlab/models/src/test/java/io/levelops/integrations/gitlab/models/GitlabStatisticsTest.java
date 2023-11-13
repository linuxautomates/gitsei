package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

public class GitlabStatisticsTest {
    @Test
    public void testDeserialize() throws URISyntaxException, IOException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource("gitlab/gitlab_api_statistics.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        GitlabStatistics stats = mapper.readValue(testFile, GitlabStatistics.class);
        Assert.assertNotNull(stats);
        GitlabStatistics.Statistics.Counts counts = GitlabStatistics.Statistics.Counts.builder()
                .all(13)
                .closed(1)
                .opened(12)
                .build();
        GitlabStatistics.Statistics statistics = GitlabStatistics.Statistics.builder()
                .counts(counts)
                .build();
        GitlabStatistics apiRes = GitlabStatistics.builder()
                .stats(statistics)
                .build();
        Assert.assertEquals(stats, apiRes);
    }
}
