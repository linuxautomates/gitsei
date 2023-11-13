package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class GitlabNoteTest {
    @Test
    public void testDeserialization() throws JsonProcessingException {
        String serializedString = "    {\n" +
                "        \"id\": 1534582195,\n" +
                "        \"type\": null,\n" +
                "        \"body\": \"approved this merge request\",\n" +
                "        \"attachment\": null,\n" +
                "        \"author\": {\n" +
                "            \"id\": 15387935,\n" +
                "            \"username\": \"siddharth.bidasaria\",\n" +
                "            \"name\": \"Siddharth Bidasaria\",\n" +
                "            \"state\": \"active\",\n" +
                "            \"avatar_url\": \"https://secure.gravatar.com/avatar/ea35f9bba00dc3657053870499e5b904?s=80&d=identicon\",\n" +
                "            \"web_url\": \"https://gitlab.com/siddharth.bidasaria\"\n" +
                "        },\n" +
                "        \"created_at\": \"2023-08-29T23:58:16.486Z\",\n" +
                "        \"updated_at\": \"2023-08-29T23:58:16.491Z\",\n" +
                "        \"system\": true,\n" +
                "        \"noteable_id\": 246383287,\n" +
                "        \"noteable_type\": \"MergeRequest\",\n" +
                "        \"project_id\": 48383332,\n" +
                "        \"resolvable\": false,\n" +
                "        \"confidential\": false,\n" +
                "        \"internal\": false,\n" +
                "        \"noteable_iid\": 1,\n" +
                "        \"commands_changes\": {}\n" +
                "    }";
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        GitlabNote note = objectMapper.readValue(serializedString, GitlabNote.class);
        assertThat(note.getId()).isEqualTo("1534582195");
        assertThat(note.getType()).isNull();
        assertThat(note.getBody()).isEqualTo("approved this merge request");
        assertThat(note.getAttachment()).isNull();
        assertThat(note.getCreatedAt()).isEqualTo("2023-08-29T23:58:16.486Z");
        assertThat(note.getUpdatedAt()).isEqualTo("2023-08-29T23:58:16.491Z");
        assertThat(note.getSystem()).isTrue();
        assertThat(note.getNoteableId()).isEqualTo("246383287");
        assertThat(note.getNoteableType()).isEqualTo("MergeRequest");
        assertThat(note.getProjectId()).isEqualTo("48383332");
        assertThat(note.getResolvable()).isFalse();
        assertThat(note.getConfidential()).isFalse();
        assertThat(note.getInternal()).isFalse();
        assertThat(note.getNoteableIid()).isEqualTo("1");
        assertThat(note.getAuthor().getId()).isEqualTo("15387935");
        assertThat(note.getAuthor().getUsername()).isEqualTo("siddharth.bidasaria");
        assertThat(note.getAuthor().getAuthorName()).isEqualTo("Siddharth Bidasaria");
    }

    @Test
    public void testApprovedComment() {
        GitlabNote note = GitlabNote.builder()
                .id("abcd")
                .createdAt(new Date())
                .system(true)
                .author(GitlabEvent.GitlabEventAuthor.builder()
                        .id("1234")
                        .build())
                .body("approved this merge request")
                .build();
        Optional<GitlabEvent> event = note.toEvent();
        assertThat(event).isNotEmpty();
        assertThat(event.get().getActionName()).isEqualTo("approved");

        note = note.toBuilder()
                .system(false)
                .build();
        event = note.toEvent();
        assertThat(event.get().getActionName()).isNotEqualTo("approved");

        note = note.toBuilder()
                .system(true)
                .body("custom comment")
                .build();
        event = note.toEvent();
        assertThat(event).isEmpty();
    }

}