package io.levelops.workflow.converters;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.workflow.models.ui.WorkflowUiData;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkflowUiDataParserTest {

    private WorkflowUiDataParser workflowUiDataParser;

    @Before
    public void setUp() throws Exception {
        workflowUiDataParser = new WorkflowUiDataParser(DefaultObjectMapper.get());
    }

    @Test
    public void testParsing() throws IOException {
        String input = ResourceUtils.getResourceAsString("workflow/workflow-ui-data.json");
        WorkflowUiData workflowUiData = DefaultObjectMapper.get().readValue(input, WorkflowUiData.class);

        WorkflowUiDataParser.ParsedWorkflowData workflow = workflowUiDataParser.parse(workflowUiData);

        ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String parsed = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(workflow);

        System.out.println(parsed);

        assertThat(parsed).isEqualTo(ResourceUtils.getResourceAsString("workflow/parsed-workflow.json"));

    }
}