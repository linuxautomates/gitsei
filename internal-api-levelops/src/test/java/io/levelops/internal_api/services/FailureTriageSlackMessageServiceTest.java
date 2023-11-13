package io.levelops.internal_api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.cicd.FailureTriageSlackMessage;
import io.levelops.commons.databases.services.WorkItemFailureTriageViewService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class FailureTriageSlackMessageServiceTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testConvertViewToFailureTriageSlackMessage() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("jenkins_failure_triage/failure_triage_view.json");
        List<WorkItemFailureTriageViewService.WIFailureTriageView> views = MAPPER.readValue(serialized, MAPPER.getTypeFactory().constructCollectionType(List.class, WorkItemFailureTriageViewService.WIFailureTriageView.class));
        FailureTriageSlackMessageService failureTriageSlackMessageService = new FailureTriageSlackMessageService();
        FailureTriageSlackMessage failureTriageSlackMessage = failureTriageSlackMessageService.convertViewToFailureTriageSlackMessage(views);
        Assert.assertNotNull(failureTriageSlackMessage);
    }
}