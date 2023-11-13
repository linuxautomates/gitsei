package io.levelops.integrations.gitlab.models;

import org.junit.Test;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class GitlabStateEventTest {
    @Test
    public void testToEvent() {
        GitlabStateEvent stateEvent = GitlabStateEvent.builder()
                .state("closed")
                .id("id")
                .resourceType("MergeRequest")
                .createdAt(new Date())
                .resourceId("resourceId")
                .user(GitlabUser.builder()
                        .name("Steph Curry")
                        .id("goat-id")
                        .build())
                .build();
        Optional<GitlabEvent> event = stateEvent.toEvent();
        assertThat(event.get().getActionName()).isEqualTo("closed");

        // if resource type is not merge request then this should return empty
        stateEvent = stateEvent.toBuilder()
                .resourceType("issue")
                .build();
        event = stateEvent.toEvent();
        assertThat(event).isEmpty();
    }

}