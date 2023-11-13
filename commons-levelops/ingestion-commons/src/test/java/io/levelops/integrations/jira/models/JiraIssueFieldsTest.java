package io.levelops.integrations.jira.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.assertj.core.util.Lists;
import org.junit.Test;


import static org.assertj.core.api.Assertions.assertThat;

public class JiraIssueFieldsTest {

    @Test
    public void testDescription() {
        JiraIssueFields fields = JiraIssueFields.builder()
                .description(JiraContent.builder()
                        .entries(Lists.newArrayList(
                                JiraContent.builder()
                                        .entries(Lists.newArrayList(
                                                JiraContent.builder()
                                                        .text("hello")
                                                        .build(),
                                                JiraContent.builder()
                                                        .text(null)
                                                        .build(),
                                                JiraContent.builder()
                                                        .text("world")
                                                        .build()
                                        ))
                                        .build(),
                                null,
                                JiraContent.builder()
                                        .entries(Lists.newArrayList(
                                                JiraContent.builder()
                                                        .text("abc")
                                                        .build(),
                                                JiraContent.builder()
                                                        .text(null)
                                                        .build(),
                                                JiraContent.builder()
                                                        .text("def")
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();

        assertThat(fields.description).isNotNull();
        assertThat(fields.getDescription()).isNull();
        assertThat(fields.getDescriptionText()).isEqualTo("hello\nworld\nabc\ndef");
        assertThat(fields.getDescriptionLength()).isEqualTo(19);
        String json = DefaultObjectMapper.writeAsPrettyJson(fields);
        assertThat(json).contains("\"description_text\" : \"hello\\nworld\\nabc\\ndef\"");
        assertThat(json).contains("\"description_length\" : 19");
    }
}