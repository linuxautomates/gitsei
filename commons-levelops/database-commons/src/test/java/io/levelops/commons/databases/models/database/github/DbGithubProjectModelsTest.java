package io.levelops.commons.databases.models.database.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubProject;
import io.levelops.integrations.github.models.GithubProjectCard;
import io.levelops.integrations.github.models.GithubProjectColumn;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class DbGithubProjectModelsTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testFromProject() throws IOException {
        String data = ResourceUtils.getResourceAsString("github/github_project.json");
        GithubProject githubProject = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GithubProject.class));
        DbGithubProject dbGithubProject = DbGithubProject.fromProject(githubProject,"1");
        Assert.assertEquals("12475915", dbGithubProject.getProjectId());
        Assert.assertEquals("askcorg", dbGithubProject.getOrganization());
        Assert.assertEquals("1", dbGithubProject.getIntegrationId());
        Assert.assertEquals("askchaitanya2021", dbGithubProject.getCreator());
    }

    @Test
    public void testFromProjectColumn() throws IOException {
        String data = ResourceUtils.getResourceAsString("github/github_project_column.json");
        GithubProjectColumn githubProjectColumn = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GithubProjectColumn.class));
        DbGithubProjectColumn dbGithubProjectColumn = DbGithubProjectColumn.fromProjectColumn(githubProjectColumn,"1");
        Assert.assertEquals("14344020", dbGithubProjectColumn.getColumnId());
        Assert.assertEquals("To do", dbGithubProjectColumn.getName());
        Assert.assertEquals("1", dbGithubProjectColumn.getProjectId());
    }

    @Test
    public void testFromProjectCard() throws IOException {
        String data = ResourceUtils.getResourceAsString("github/github_project_card.json");
        GithubProjectCard githubProjectCard = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GithubProjectCard.class));
        DbGithubProjectCard dbGithubProjectCard = DbGithubProjectCard.fromProjectCard(githubProjectCard,"1");
        Assert.assertEquals("61249134", dbGithubProjectCard.getCardId());
        Assert.assertEquals("askchaitanya2021", dbGithubProjectCard.getCreator());
        Assert.assertEquals("https://api.github.com/repos/askcorg/CapsuleOne/issues/4", dbGithubProjectCard.getContentUrl());
        Assert.assertEquals("4", dbGithubProjectCard.getNumber());
        Assert.assertEquals("askcorg/CapsuleOne", dbGithubProjectCard.getRepoId());
    }

}
