package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class JiraSprintModelsTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    private <T> T parseJiraSprintModel(String resourceUrl, Class<T> clazz) throws IOException {
        String data = ResourceUtils.getResourceAsString(resourceUrl);
        return MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(clazz));
    }

    @Test
    public void testBoard() throws IOException {
        JiraBoard board = parseJiraSprintModel("integrations/jira/jira_board.json", JiraBoard.class);
        Assert.assertNotNull(board);
        Assert.assertEquals("1", board.getId());
        Assert.assertEquals("DefaultScrumBoard", board.getName());
        Assert.assertEquals("https://levelops.atlassian.net/rest/agile/1.0/board/1", board.getSelf());
        Assert.assertEquals("scrum", board.getType());
    }

    @Test
    public void testSprint() throws IOException {
        JiraSprint sprint = parseJiraSprintModel("integrations/jira/jira_sprint.json", JiraSprint.class);
        Assert.assertNotNull(sprint);
        Assert.assertEquals("18", sprint.getId());
        Assert.assertEquals("https://levelops.atlassian.net/rest/agile/1.0/sprint/18", sprint.getSelf());
        Assert.assertEquals("closed", sprint.getState());
        Assert.assertEquals("Sprint Sprint 9", sprint.getName());
        Assert.assertEquals("2021-06-14T03:53:25.213Z", sprint.getStartDate());
        Assert.assertEquals("2021-06-25T18:30:00.000Z", sprint.getEndDate());
        Assert.assertEquals("2021-06-27T23:40:12.412Z", sprint.getCompleteDate());
        Assert.assertEquals("3", sprint.getOriginBoardId());
        Assert.assertEquals("Performance testing\nAnalytics engine\nMock data generators", sprint.getGoal());
    }

    @Test
    public void testBoardResult() throws IOException {
        JiraBoardResult boardResult = parseJiraSprintModel("integrations/jira/jira_board_result.json", JiraBoardResult.class);
        Assert.assertNotNull(boardResult);
        Assert.assertEquals(Integer.valueOf("5"), boardResult.getTotal());
        Assert.assertEquals(5, boardResult.getBoards().size());
    }

    @Test
    public void testSprintResult() throws IOException {
        JiraSprintResult sprintResult = parseJiraSprintModel("integrations/jira/jira_sprint_result.json", JiraSprintResult.class);
        Assert.assertNotNull(sprintResult);
        Assert.assertEquals(16, sprintResult.getSprints().size());
    }

}
