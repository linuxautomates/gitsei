package io.levelops.aggregations.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class JenkinsMonitoringAggDataTest {
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void test() throws IOException {
        String jenkinsAggsString = ResourceUtils.getResourceAsString("models/jenkins/jenkins_aggregation_results.json");
        JenkinsMonitoringAggData jenkinsMonitoringAggData = MAPPER.readValue(jenkinsAggsString, JenkinsMonitoringAggData.class);
        Assert.assertNotNull(jenkinsMonitoringAggData);
    }
}