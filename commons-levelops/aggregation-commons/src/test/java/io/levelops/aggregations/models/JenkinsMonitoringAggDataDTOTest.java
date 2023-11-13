package io.levelops.aggregations.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class JenkinsMonitoringAggDataDTOTest {
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void test() throws IOException {
        String jenkinsAggsString = ResourceUtils.getResourceAsString("models/jenkins/jenkins_aggregation_results_dto.json");
        JenkinsMonitoringAggDataDTO jenkinsMonitoringAggDataDTO = MAPPER.readValue(jenkinsAggsString, JenkinsMonitoringAggDataDTO.class);
        Assert.assertNotNull(jenkinsMonitoringAggDataDTO);
    }
}