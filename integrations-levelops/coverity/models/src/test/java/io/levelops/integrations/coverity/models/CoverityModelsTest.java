package io.levelops.integrations.coverity.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CoverityModelsTest {

    @Test
    public void deSerializeQuery() throws IOException {
        String inputProjects = ResourceUtils.getResourceAsString("coverity_iterative_query.json");
        ObjectMapper mapper = DefaultObjectMapper.get();
        CoverityIterativeScanQuery coverityIterativeScanQuery = mapper.readValue(inputProjects, CoverityIterativeScanQuery.class);
        assertThat(coverityIterativeScanQuery).isNotNull();
        assertThat(coverityIterativeScanQuery.getIntegrationKey().getIntegrationId()).isNotNull();
        assertThat(coverityIterativeScanQuery.getFrom()).isNotNull();
        assertThat(coverityIterativeScanQuery.getTo()).isNotNull();
    }

    @Test
    public void deSerializeEnrichedProjectData() throws IOException {
        String inputProjects = ResourceUtils.getResourceAsString("enriched_project_data.json");
        ObjectMapper mapper = DefaultObjectMapper.get();
        EnrichedProjectData enrichedProjectData = mapper.readValue(inputProjects, EnrichedProjectData.class);
        assertThat(enrichedProjectData).isNotNull();
        assertThat(enrichedProjectData.getStream().getId()).isNotNull();
        assertThat(enrichedProjectData.getSnapshot().getSnapshotId()).isNotNull();
        assertThat(enrichedProjectData.getDefects().get(0).getCid()).isNotNull();
    }
}
