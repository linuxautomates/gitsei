package io.levelops.api.controllers.issue_mgmt;

import io.levelops.commons.databases.models.filters.WorkItemsTimelineFilter;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkItemsControllerTest {

    @Test
    public void getWorkItemTimelineBuilder() {
        WorkItemsTimelineFilter workItemTimelineBuilder = WorkItemsController.getWorkItemTimelineFilter(DefaultListRequest.builder().build());
        DefaultObjectMapper.prettyPrint(workItemTimelineBuilder);
        assertThat(workItemTimelineBuilder).isNotNull();
    }

}