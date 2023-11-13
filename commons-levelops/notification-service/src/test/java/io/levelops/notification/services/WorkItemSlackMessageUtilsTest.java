package io.levelops.notification.services;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class WorkItemSlackMessageUtilsTest {
    @Test
    public void testBuildWorkItemAttachmentSlackCallbackId() {
        Assert.assertEquals("view_wi_text_attachment~@@@_foo~@@@_390a40fb-7cbe-475b-8772-dc8c9eda91ec~@@@_11f22de6-340d-4856-9612-1f8b4f63e764",WorkItemSlackMessageUtils.buildWorkItemAttachmentSlackCallbackId("foo", "390a40fb-7cbe-475b-8772-dc8c9eda91ec", "11f22de6-340d-4856-9612-1f8b4f63e764"));
    }

    @Test
    public void testParseQuestionnaireSlackCallbackId() {
        Optional<WorkItemSlackMessageUtils.WorkItemAttchmentMetadata> result = WorkItemSlackMessageUtils.parseQuestionnaireSlackCallbackId("view_wi_text_attachment~@@@_foo~@@@_390a40fb-7cbe-475b-8772-dc8c9eda91ec~@@@_11f22de6-340d-4856-9612-1f8b4f63e764");
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("foo", result.get().getCompany());
        Assert.assertEquals("390a40fb-7cbe-475b-8772-dc8c9eda91ec", result.get().getWorkItemId());
        Assert.assertEquals("11f22de6-340d-4856-9612-1f8b4f63e764", result.get().getUploadId());
    }
}