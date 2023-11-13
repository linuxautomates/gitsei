package io.levelops.commons.databases.utils;

import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.models.DefaultListRequest;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueMgmtUtilTest {

    @Test
    public void resolveAcross() {
        assertThat(IssueMgmtUtil.resolveAcross(DefaultListRequest.builder()
                .across("Custom.Field")
                .build(), WorkItemsFilter.builder().build()).getAcross()).isEqualTo(WorkItemsFilter.DISTINCT.custom_field);
        assertThat(IssueMgmtUtil.resolveAcross(DefaultListRequest.builder()
                        .across("Microsoft.VSTS.Scheduling.Effort")
                .build(), WorkItemsFilter.builder().build()).getAcross()).isEqualTo(WorkItemsFilter.DISTINCT.custom_field);
        assertThat(IssueMgmtUtil.resolveAcross(DefaultListRequest.builder()
                .across("CodeArea")
                .build(), WorkItemsFilter.builder().build()).getAcross()).isEqualTo(WorkItemsFilter.DISTINCT.attribute);
    }
}