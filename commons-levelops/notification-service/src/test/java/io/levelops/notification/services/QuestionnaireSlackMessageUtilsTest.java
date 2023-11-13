package io.levelops.notification.services;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class QuestionnaireSlackMessageUtilsTest {
    @Test
    public void testBuildQuestionnaireSlackCallbackId() {
        Assert.assertEquals("edit_questionnaire~@@@_foo~@@@_390a40fb-7cbe-475b-8772-dc8c9eda91ec", QuestionnaireSlackMessageUtils.buildQuestionnaireSlackCallbackId("foo", "390a40fb-7cbe-475b-8772-dc8c9eda91ec"));
    }

    @Test
    public void testParseQuestionnaireSlackCallbackId() {
        Optional<ImmutablePair<String, String>> result = QuestionnaireSlackMessageUtils.parseQuestionnaireSlackCallbackId("edit_questionnaire~@@@_foo~@@@_390a40fb-7cbe-475b-8772-dc8c9eda91ec");
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("foo", result.get().getLeft());
        Assert.assertEquals("390a40fb-7cbe-475b-8772-dc8c9eda91ec", result.get().getRight());
    }
}