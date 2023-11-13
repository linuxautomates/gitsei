package io.levelops.commons.databases.models.database.questionnaire;

import io.levelops.commons.utils.ResourceUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;

public class AnswerTest {
    @Test
    public void test() throws IOException {
        var answer =  ResourceUtils.getResourceAsObject("questionnaire_gcs/answer.json", Answer.class);
        Assertions.assertThat(answer).isNotNull();
        Assertions.assertThat(answer.getResponses()).hasSize(1);
        Assertions.assertThat(answer.getComments()).hasSize(1);
        Assertions.assertThat(answer.getComments().get(0).getUser()).isEqualTo("test1@test.test");
        Assertions.assertThat(answer.getComments().get(0).getMessage()).isEqualTo("My Test");
        Assertions.assertThat(answer.getComments().get(0).getCreatedAt()).isEqualTo(1592505967);
    }
}