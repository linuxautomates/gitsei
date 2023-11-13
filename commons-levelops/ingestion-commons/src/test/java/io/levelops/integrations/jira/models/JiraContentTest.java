package io.levelops.integrations.jira.models;

import io.levelops.commons.utils.ResourceUtils;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraContentTest {

    String descriptionV3 = "Description!\nCVE-29012\ntest\nadfasd\nasdasdas\nquote\nquoted text\n ";

    @Test
    public void deserializeDescriptionV3() throws IOException {
        JiraContent input = ResourceUtils.getResourceAsObject("integrations/jira/jira_content_v3.json", JiraContent.class);
        String text = JiraContent.generateDescriptionText(input);
        assertThat(text).isEqualTo(descriptionV3);
    }
}