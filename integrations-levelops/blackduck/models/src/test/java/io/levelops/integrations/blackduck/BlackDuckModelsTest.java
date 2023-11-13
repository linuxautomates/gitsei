package io.levelops.integrations.blackduck;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.blackduck.models.BlackDuckMetadata;
import io.levelops.integrations.blackduck.models.BlackDuckIssuesListResponse;
import io.levelops.integrations.blackduck.models.BlackDuckVulnerability;
import io.levelops.integrations.blackduck.models.BlackDuckProject;
import io.levelops.integrations.blackduck.models.BlackDuckProjectsListResponse;
import io.levelops.integrations.blackduck.models.BlackDuckVersionsListResponse;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class BlackDuckModelsTest {

    @Test
    public void deSerializeIssues() throws IOException {
        String inputProjects = ResourceUtils.getResourceAsString("blackduck_issues.json");
        ObjectMapper mapper = DefaultObjectMapper.get();
        BlackDuckIssuesListResponse blackDuckIssuesListResponse =
                mapper.readValue(inputProjects, BlackDuckIssuesListResponse.class);
        assertThat(blackDuckIssuesListResponse).isNotNull();
        assertThat(blackDuckIssuesListResponse.getBlackDuckIssues()).isNotEmpty();
    }

    @Test
    public void deSerializeProjectsList() throws IOException {
        String inputProjects = ResourceUtils.getResourceAsString("blackduck_projects.json");
        ObjectMapper mapper = DefaultObjectMapper.get();
        BlackDuckProjectsListResponse blackDuckProjectsListResponse =
                mapper.readValue(inputProjects, BlackDuckProjectsListResponse.class);
        assertThat(blackDuckProjectsListResponse).isNotNull();
        assertThat(blackDuckProjectsListResponse.getBlackDuckProjects()).isNotEmpty();
    }

    @Test
    public void deSerializeMetaData() throws IOException {
        String inputProjects = ResourceUtils.getResourceAsString("bd_meta.json");
        ObjectMapper mapper = DefaultObjectMapper.get();
        BlackDuckMetadata blackDuckMetadata =
                mapper.readValue(inputProjects, BlackDuckMetadata.class);
        assertThat(blackDuckMetadata).isNotNull();
        assertThat(blackDuckMetadata.getProjectHref()).isNotEmpty();
    }

    @Test
    public void deSerializeProject() throws IOException {
        String inputProjects = ResourceUtils.getResourceAsString("bd_proj.json");
        ObjectMapper mapper = DefaultObjectMapper.get();
        BlackDuckProject blackDuckProject =
                mapper.readValue(inputProjects, BlackDuckProject.class);
        assertThat(blackDuckProject).isNotNull();
        assertThat(blackDuckProject.getProjectName()).isNotEmpty();
    }

    @Test
    public void deSerializeVersions() throws IOException {
        String inputProjects = ResourceUtils.getResourceAsString("blackduck_versions.json");
        ObjectMapper mapper = DefaultObjectMapper.get();
        BlackDuckVersionsListResponse blackDuckVersionsListResponse =
                mapper.readValue(inputProjects, BlackDuckVersionsListResponse.class);
        assertThat(blackDuckVersionsListResponse).isNotNull();
        assertThat(blackDuckVersionsListResponse.getBlackDuckVersions()).isNotEmpty();
        String projectHref = blackDuckVersionsListResponse.getBlackDuckVersions().get(0).getBlackDuckMetadata().getProjectHref();

    }

    @Test
    public void deSerializeVulnerabilities() throws IOException {
        String inputProjects = ResourceUtils.getResourceAsString("blackduck_vulnerability.json");
        ObjectMapper mapper = DefaultObjectMapper.get();
        BlackDuckVulnerability blackDuckVulnerability =
                mapper.readValue(inputProjects, BlackDuckVulnerability.class);
        DefaultObjectMapper.prettyPrint(blackDuckVulnerability);
        assertThat(blackDuckVulnerability).isNotNull();
        assertThat(blackDuckVulnerability.getVulnerabilityName()).isEqualTo("CVE-2020-28500");
    }
}
