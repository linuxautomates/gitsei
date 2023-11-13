package io.levelops.integrations.coverity.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class EnrichedProjectModelTest {

    @Test
    public void deSerializeEnrichedProjectData() throws IOException {
        String inputProjects = ResourceUtils.getResourceAsString("integrations/coverity/enriched_project_data.json");
        ObjectMapper mapper = DefaultObjectMapper.get();
        EnrichedProjectData enrichedProjectData = mapper.readValue(inputProjects, EnrichedProjectData.class);
        assertThat(enrichedProjectData).isNotNull();
        assertThat(enrichedProjectData.getStream().getId()).isNotNull();
        assertThat(enrichedProjectData.getSnapshot().getSnapshotId()).isNotNull();
        assertThat(enrichedProjectData.getDefects().get(0).getCid()).isNotNull();
    }

    @Test
    public void deSerializeCoverityAttributes() throws IOException {
        String inputProjects = ResourceUtils.getResourceAsString("integrations/coverity/coverity_attributes.json");
        ObjectMapper mapper = DefaultObjectMapper.get();
        CoverityAttributes coverityAttributes = mapper.readValue(inputProjects, CoverityAttributes.class);
        assertThat(coverityAttributes).isNotNull();
        assertThat(coverityAttributes.getAttributeValueId().getName()).isNotNull();
        assertThat(coverityAttributes.getAttributeDefinitionId().getName()).isNotNull();
    }
}
