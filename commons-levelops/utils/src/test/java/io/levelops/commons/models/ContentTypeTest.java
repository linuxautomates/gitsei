package io.levelops.commons.models;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentTypeTest {

    @Test
    public void test() {
        ContentType contentType = ContentType.fromString("integer");
        assertThat(contentType.getComponentType()).isEqualTo(ComponentType.NONE);
        assertThat(contentType.getComponentName()).isNull();
        assertThat(contentType.getType()).isEqualTo("integer");
        assertThat(contentType.toString()).isEqualTo("integer");
        assertThat(contentType.getJsonPath()).isNull();
        assertThat(contentType.isId()).isFalse();

        contentType = ContentType.fromString("plugin_result/praetorian/Finding");
        assertThat(contentType.getComponentType()).isEqualTo(ComponentType.PLUGIN_RESULT);
        assertThat(contentType.getComponentName()).isEqualTo("praetorian");
        assertThat(contentType.getType()).isEqualTo("finding");
        assertThat(contentType.toString()).isEqualTo("plugin_result/praetorian/finding");
        assertThat(contentType.getJsonPath()).isNull();
        assertThat(contentType.isId()).isFalse();

        contentType = ContentType.fromString("integration/jira/issues");
        assertThat(contentType.getComponentType()).isEqualTo(ComponentType.INTEGRATION);
        assertThat(contentType.getComponentName()).isEqualTo("jira");
        assertThat(contentType.getType()).isEqualTo("issues");
        assertThat(contentType.toString()).isEqualTo("integration/jira/issues");
        assertThat(contentType.getJsonPath()).isNull();
        assertThat(contentType.isId()).isFalse();

        contentType = ContentType.withComponent(ComponentType.NONE, "test", "integer");
        assertThat(contentType.toString()).isEqualTo("integer");
        assertThat(contentType.getJsonPath()).isNull();
        assertThat(contentType.isId()).isFalse();

        contentType = ContentType.withComponent(ComponentType.UNKNOWN, "test", "integer");
        assertThat(contentType.toString()).isEqualTo("integer");
        assertThat(contentType.getJsonPath()).isNull();
        assertThat(contentType.isId()).isFalse();

        assertThat( ContentType.fromString("id:integration/jira/issues").toBaseType()).isEqualTo(ContentType.fromString("integration/jira/issues"));
        assertThat( ContentType.fromString("integration/jira/issues").toIdType()).isEqualTo(ContentType.fromString("id:integration/jira/issues"));

        contentType = ContentType.fromString("id:integration/jira/issues");
        assertThat(contentType.getComponentType()).isEqualTo(ComponentType.INTEGRATION);
        assertThat(contentType.getComponentName()).isEqualTo("jira");
        assertThat(contentType.getType()).isEqualTo("issues");
        assertThat(contentType.toString()).isEqualTo("id:integration/jira/issues");
        assertThat(contentType.getJsonPath()).isNull();
        assertThat(contentType.isId()).isTrue();

        assertThat( ContentType.fromString("id:integration/jira/issues").withJsonPath("json.path")).isEqualTo(ContentType.fromString("id:integration/jira/issues.json.path"));
        assertThat( ContentType.fromString("id:integration/jira/issues.json.path").withoutJsonPath()).isEqualTo(ContentType.fromString("id:integration/jira/issues"));

        contentType = ContentType.fromString("id:integration/jira/issues.json.path");
        assertThat(contentType.getComponentType()).isEqualTo(ComponentType.INTEGRATION);
        assertThat(contentType.getComponentName()).isEqualTo("jira");
        assertThat(contentType.getType()).isEqualTo("issues");
        assertThat(contentType.toString()).isEqualTo("id:integration/jira/issues.json.path");
        assertThat(contentType.getJsonPath()).isEqualTo("json.path");
        assertThat(contentType.isId()).isTrue();

        contentType = ContentType.fromString("id:config_row");
        assertThat(contentType.toString()).isEqualTo("id:config_row");
    }

}