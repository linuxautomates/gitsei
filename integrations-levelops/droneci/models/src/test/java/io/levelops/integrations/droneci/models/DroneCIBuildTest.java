package io.levelops.integrations.droneci.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DroneCIBuildTest {

    @Test
    public void testDeserialize() throws URISyntaxException, IOException {
        File testFile = new File(Objects.requireNonNull(this.getClass().getClassLoader().
                getResource("droneci/droneci_api_build.json")).toURI());
        ObjectMapper mapper = DefaultObjectMapper.get();
        List<DroneCIBuild> response = mapper.readValue(testFile,
                mapper.getTypeFactory().constructCollectionType(List.class, DroneCIBuild.class));
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.size());

        DroneCIBuild apiRes = DroneCIBuild.builder()
                .id(3273L)
                .repoId(682L)
                .trigger("test-owner")
                .number(1L)
                .status("success")
                .event("custom")
                .action("")
                .link("https://github.com/test-owner/test1/commit/cf883e4e49ce970d57a4ecd6cdae430a0f0f055e")
                .timestamp(0L)
                .message("Update .drone.yml")
                .before("cf883e4e49ce970d57a4ecd6cdae430a0f0f055e")
                .after("cf883e4e49ce970d57a4ecd6cdae430a0f0f055e")
                .ref("refs/heads/main")
                .sourceRepo("")
                .source("main")
                .target("main")
                .authorLogin("test-owner")
                .authorName("test-owner")
                .authorEmail("104614802+test-owner@users.noreply.github.com")
                .authorAvatar("https://avatars.githubusercontent.com/u/104614802?v=4")
                .sender("test-owner")
                .started(1652421877L)
                .finished(1652421879L)
                .created(1652421877L)
                .updated(1652421877L)
                .version(3L)
                .stages(getStages())
                .build();

        Assert.assertEquals(response.get(0), apiRes);
    }

    public List<DroneCIBuildStage> getStages() {
        List<DroneCIBuildStage> stages = new ArrayList<>();
        DroneCIBuildStage stage = DroneCIBuildStage.builder()
                .id(3272L)
                .repoId(682L)
                .buildId(3273L)
                .number(1L)
                .name("default")
                .kind("pipeline")
                .type("docker")
                .status("success")
                .error("")
                .errIgnore(false)
                .exitCode(0L)
                .machine("runner")
                .os("linux")
                .arch("amd64")
                .started(1652421877L)
                .stopped(1652421879L)
                .created(1652421877L)
                .updated(1652421879L)
                .version(4L)
                .onSuccess(true)
                .onFailure(false)
                .steps(getSteps())
                .build();
        stages.add(stage);
        return stages;
    }

    public List<DroneCIBuildStep> getSteps() {
        List<DroneCIBuildStep> steps = new ArrayList<>();
        List<String> dependsOn = new ArrayList<>();
        dependsOn.add("clone");
        DroneCIBuildStep step = DroneCIBuildStep.builder()
                .id(13063L)
                .stepId(3272L)
                .number(1L)
                .name("clone")
                .status("success")
                .exitCode(0L)
                .started(1652421878L)
                .stopped(1652421879L)
                .version(4L)
                .image("drone/git:latest")
                .dependsOn(dependsOn)
                .build();
        steps.add(step);
        return steps;
    }
}
