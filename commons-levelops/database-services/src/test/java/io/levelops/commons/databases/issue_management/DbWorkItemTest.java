package io.levelops.commons.databases.issue_management;

import io.levelops.integrations.azureDevops.models.Comment;
import io.levelops.integrations.azureDevops.models.Fields;
import io.levelops.integrations.azureDevops.models.WorkItem;

import org.junit.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DbWorkItemTest {

    @Test
    public void parseDate() {
        assertThat(DbWorkItem.parseDate("2020-01-02T00:31:14.00Z")).isEqualTo(Timestamp.from(Instant.parse("2020-01-02T00:31:14.000Z")));
        assertThat(DbWorkItem.parseDate(null)).isNull();
    }

    @Test
    public void getFirstCommentAt() {
        Timestamp firstCommentAt = DbWorkItem.getFirstCommentAt(WorkItem.builder()
                .comments(List.of(Comment.builder().createdDate("2020-01-02T00:31:14.00Z").build(), Comment.builder().createdDate("2020-02-03T00:31:14.00Z").build()))
                .build());
        assertThat(firstCommentAt).isEqualTo(Timestamp.from(Instant.parse("2020-01-02T00:31:14.000Z")));

         firstCommentAt = DbWorkItem.getFirstCommentAt(WorkItem.builder()
                .comments(null)
                .build());
        assertThat(firstCommentAt).isNull();

         firstCommentAt = DbWorkItem.getFirstCommentAt(WorkItem.builder()
                .comments(List.of(Comment.builder().build()))
                .build());
        assertThat(firstCommentAt).isNull();
    }

    @Test
    public void parseStoryPoints() {
        assertThat(DbWorkItem.parseStoryPoints(Fields.builder().storyPoints(123f).build(), null)).isEqualTo(123f);
        assertThat(DbWorkItem.parseStoryPoints(Fields.builder().storyPoints(null).build(), null)).isEqualTo(0f);

        assertThat(DbWorkItem.parseStoryPoints(Fields.builder().storyPoints(123f).effort(42).build(), "effort")).isEqualTo(42f);
        assertThat(DbWorkItem.parseStoryPoints(Fields.builder().storyPoints(123f).effort(null).build(), "effort")).isEqualTo(0f);

        Fields fields = Fields.builder().storyPoints(123f).effort(42).build();
        fields.addCustomField("fieldA", 456.01f);
        fields.addCustomField("fieldB", "789.01");
        fields.addCustomField("fieldC", null);
        assertThat(DbWorkItem.parseStoryPoints(fields, "fieldA")).isEqualTo(456.01f);
        assertThat(DbWorkItem.parseStoryPoints(fields, "fieldB")).isEqualTo(789.01f);
        assertThat(DbWorkItem.parseStoryPoints(fields, "fieldC")).isEqualTo(0f);
        assertThat(DbWorkItem.parseStoryPoints(fields, "fieldD")).isEqualTo(0f);
    }
}
