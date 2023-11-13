package io.levelops.api.services;

import io.levelops.api.model.RunbookDTO;
import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.databases.models.database.runbooks.Runbook;
import io.levelops.commons.databases.models.database.runbooks.RunbookNode;
import io.levelops.commons.databases.models.database.runbooks.RunbookNodeTemplate;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.databases.services.RunbookNodeTemplateDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ContentType;
import io.levelops.commons.utils.ResourceUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

public class RunbookDTOServiceTest {

    @Mock
    RunbookNodeTemplateDatabaseService runbookNodeTemplateDatabaseService;

    RunbookDTOService runbookDTOService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(runbookNodeTemplateDatabaseService.stream(eq("foo"), eq(List.of("trigger", "type1", "type2")), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Stream.of(RunbookNodeTemplate.builder()
                                .type("type1")
                                .nodeHandler("nodeHandler1")
                                .build(),
                        RunbookNodeTemplate.builder()
                                .type("type2")
                                .nodeHandler("nodeHandler2")
                                .build()));
        runbookDTOService = new RunbookDTOService(DefaultObjectMapper.get(), runbookNodeTemplateDatabaseService);
    }

    @Test
    public void test() throws IOException {

        RunbookDTO input = ResourceUtils.getResourceAsObject("runbooks/runbook_dto.json", RunbookDTO.class);

        Runbook rb = runbookDTOService.parseDTO("foo", input);

        DefaultObjectMapper.prettyPrint(rb);

        assertThat(rb.getName()).isEqualTo("Runbook1");
        assertThat(rb.getEnabled()).isEqualTo(true);
        assertThat(rb.getTriggerType()).isEqualTo(TriggerType.SCHEDULED);
        assertThat(rb.getTriggerData()).isEqualTo(Map.of("interval_in_min", RunbookVariable.builder()
                .name("interval_in_min")
                .type("text")
                .contentType(ContentType.fromString("string"))
                .valueType(RunbookVariable.RunbookValueType.STRING)
                .value("10")
                .build()));
        assertThat(rb.getNodes()).containsOnlyKeys("1", "2");
        assertThat(rb.getNodes().get("1")).isEqualToIgnoringGivenFields(RunbookNode.builder()
                .id("1")
                .type("type1")
                .nodeHandler("nodeHandler1")
                .name("Node 1")
                .to("2", RunbookNode.NodeTransition.option("true"))
                .inputVariable(RunbookVariable.builder()
                        .name("field2")
                        .type("multi-select")
                        .contentType(ContentType.fromString("t1"))
                        .valueType(RunbookVariable.RunbookValueType.STRING)
                        .value("user_id etc.")
                        .build())
                .inputVariable(RunbookVariable.builder()
                        .name("field1")
                        .type("multi-select")
                        .contentType(ContentType.fromString("t1"))
                        .valueType(RunbookVariable.RunbookValueType.STRING)
                        .value("user_id etc.")
                        .build())
                .build(), "uiData");
        assertThat(rb.getNodes().get("1").getUiData()).containsOnlyKeys("position", "ports");
        assertThat(rb.getNodes().get("2")).isEqualToIgnoringGivenFields(RunbookNode.builder()
                .id("2")
                .type("type2")
                .nodeHandler("nodeHandler2")
                .from("1", RunbookNode.NodeTransition.option("true"))
                .inputVariable(RunbookVariable.builder()
                        .name("condition")
                        .type("condition")
                        .valueType(RunbookVariable.RunbookValueType.NONE)
                        .build())
                .build(), "uiData");
    }

    @Test
    public void test2() throws IOException {

        RunbookDTO input = ResourceUtils.getResourceAsObject("runbooks/runbook_dto2.json", RunbookDTO.class);

        Runbook rb = runbookDTOService.parseDTO("foo", input);

        DefaultObjectMapper.prettyPrint(rb);

        assertThat(rb.getNodes().get("1").getFromNodes()).containsOnlyKeys("0", "1");

    }

    @Test
    public void toDTO() throws IOException {
        String uiData = ResourceUtils.getResourceAsString("runbooks/runbook_dto2.json");
        RunbookDTO dto = DefaultObjectMapper.get().readValue(uiData, RunbookDTO.class);

        Runbook rb = runbookDTOService.parseDTO("foo", dto);
        DefaultObjectMapper.prettyPrint(rb);

        System.out.println("* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * ");

        RunbookDTO output = runbookDTOService.toDTO(rb);
        DefaultObjectMapper.prettyPrint(output);

        assertThat(output.getUiData().getNodes()).hasSize(2);
        assertThat(output.getUiData().getLinks()).hasSize(2);
        assertThat(output.getUiData().getNodes().get("0").getInput().get("multi").getValues()).hasSize(2);

    }

    @Test
    public void testFieldData() throws IOException {
        RunbookDTO.RunbookUiData.UiFieldData uiFieldData = ResourceUtils.getResourceAsObject("runbooks/ui_field_data.json", RunbookDTO.RunbookUiData.UiFieldData.class);
        RunbookVariable runbookVariable = runbookDTOService.parseInputField(uiFieldData);
        DefaultObjectMapper.prettyPrint(runbookVariable);
    }
}