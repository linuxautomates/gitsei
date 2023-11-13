package io.levelops.integrations.github.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class GithubProjectModelsTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    private <T> T parseGithubProjectModel(String resourceUrl, Class<T> clazz) throws IOException {
        String data = ResourceUtils.getResourceAsString(resourceUrl);
        return MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(clazz));
    }

    @Test
    public void testCreator() throws IOException {
        GithubCreator creator = parseGithubProjectModel("integrations/github/github_creator.json", GithubCreator.class);
        Assert.assertNotNull(creator);
        Assert.assertEquals(Long.valueOf(79248562), creator.getId());
        Assert.assertEquals("User", creator.getType());
        Assert.assertEquals(false, creator.getSiteAdmin());
    }

    @Test
    public void testProject() throws IOException {
        GithubProject project = parseGithubProjectModel("integrations/github/github_project.json", GithubProject.class);
        Assert.assertNotNull(project);
        Assert.assertEquals("12475915", project.getId());
        Assert.assertEquals("helloProj", project.getName());
        Assert.assertEquals("write", project.getOrganizationPermission());
        Assert.assertEquals(true, project.getIsPrivate());
        GithubCreator creator = parseGithubProjectModel("integrations/github/github_creator.json", GithubCreator.class);
        Assert.assertEquals(creator, project.getCreator());
        Assert.assertEquals(1, project.getColumns().size());
        Assert.assertEquals(1, project.getColumns().get(0).getCards().size());
    }

    @Test
    public void testProjectColumn() throws IOException {
        GithubProjectColumn projectColumn = parseGithubProjectModel(
                "integrations/github/github_project_column.json", GithubProjectColumn.class);
        Assert.assertNotNull(projectColumn);
        Assert.assertEquals("14344020", projectColumn.getId());
        Assert.assertEquals("To do", projectColumn.getName());
        Assert.assertEquals("https://api.github.com/projects/columns/14344020", projectColumn.getUrl());
        Assert.assertEquals(1, projectColumn.getCards().size());
    }

    @Test
    public void testProjectCard() throws IOException {
        GithubProjectCard projectColumnCard = parseGithubProjectModel(
                "integrations/github/github_project_column_card.json", GithubProjectCard.class);
        Assert.assertNotNull(projectColumnCard);
        Assert.assertEquals("61249134", projectColumnCard.getId());
        Assert.assertEquals("https://api.github.com/projects/columns/cards/61249134", projectColumnCard.getUrl());
        GithubCreator creator = parseGithubProjectModel("integrations/github/github_creator.json", GithubCreator.class);
        Assert.assertEquals(creator, projectColumnCard.getCreator());
    }

}