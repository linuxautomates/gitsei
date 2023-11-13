package io.levelops.commons.databases.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueMgmtCustomFieldUtilsTest {

    @Test
    public void isCustomField() {
        assertThat(IssueMgmtCustomFieldUtils.isCustomField(null)).isFalse();
        assertThat(IssueMgmtCustomFieldUtils.isCustomField("")).isFalse();
        assertThat(IssueMgmtCustomFieldUtils.isCustomField("test")).isFalse();
        assertThat(IssueMgmtCustomFieldUtils.isCustomField("assignee")).isFalse();
        assertThat(IssueMgmtCustomFieldUtils.isCustomField("Custom.Field")).isTrue();
        assertThat(IssueMgmtCustomFieldUtils.isCustomField("Microsoft.VSTS.Scheduling.Effort")).isTrue();
        assertThat(IssueMgmtCustomFieldUtils.isCustomField("Microsoft.VSTS.Scheduling.DueDate")).isTrue();
        assertThat(IssueMgmtCustomFieldUtils.isCustomField("Microsoft.somethingelse")).isFalse();
        assertThat(IssueMgmtCustomFieldUtils.isCustomField("Gcc.EstimatedReleaseDate")).isTrue();
        assertThat(IssueMgmtCustomFieldUtils.isCustomField("DW.EstimatedQAReadyDate")).isTrue();
    }

}