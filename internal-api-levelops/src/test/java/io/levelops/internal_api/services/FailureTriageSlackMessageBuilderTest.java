package io.levelops.internal_api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.cicd.FailureTriageSlackMessage;
import io.levelops.commons.databases.services.WorkItemFailureTriageViewService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FailureTriageSlackMessageBuilderTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void testBuildInteractiveMessage() throws IOException {
        String appBaseUrl = "https://testui1.levelops.io/";
        String vanityId = "DEFAULT-604073";
        String serialized = ResourceUtils.getResourceAsString("jenkins_failure_triage/failure_triage_view.json");
        List<WorkItemFailureTriageViewService.WIFailureTriageView> views = MAPPER.readValue(serialized, MAPPER.getTypeFactory().constructCollectionType(List.class, WorkItemFailureTriageViewService.WIFailureTriageView.class));
        FailureTriageSlackMessageService failureTriageSlackMessageService = new FailureTriageSlackMessageService();
        FailureTriageSlackMessage failureTriageSlackMessage = failureTriageSlackMessageService.convertViewToFailureTriageSlackMessage(views);
        List<String> states = List.of("CLOSED","OPEN","IN_REVIEW","NEW","Not Yet Reviewed","No Deviation","Deviation","Shift To Next Month","Incomplete Evidence","Follow Up Questions","Scope For Improvement");
        List<ImmutablePair<String,String>> fileUploads = new ArrayList<>();
        FailureTriageSlackMessageBuilder failureTriageSlackMessageBuilder = new FailureTriageSlackMessageBuilder();
        String interactiveMessage = failureTriageSlackMessageBuilder.buildInteractiveMessage(appBaseUrl, vanityId, failureTriageSlackMessage, states, fileUploads);
        Assert.assertNotNull(interactiveMessage);
    }
}