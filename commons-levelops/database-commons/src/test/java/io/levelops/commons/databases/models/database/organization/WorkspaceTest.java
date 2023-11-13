package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.levelops.commons.jackson.DefaultObjectMapper;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Set;

public class WorkspaceTest {
    
    @Test
    public void test() throws JsonMappingException, JsonProcessingException{
        var workspace = Workspace.builder()
            .name("name")
            .description("description")
            .integrationIds(Set.of(1,2))
            .build();
        
        var w1 = DefaultObjectMapper.get().readValue("{\"name\":\"name\",\"description\":\"description\", \"integration_ids\":[1,2]}", Workspace.class);

        Assertions.assertThat(w1).isNotNull();
        Assertions.assertThat(w1.getName()).isEqualTo(workspace.getName());
        Assertions.assertThat(w1.getDescription()).isEqualTo(workspace.getDescription());
        Assertions.assertThat(w1.getIntegrationIds()).isEqualTo(workspace.getIntegrationIds());
    }
}
