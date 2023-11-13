package io.levelops.commons.databases.models.filters;

import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultListRequestUtilsTest {

    @Test
    public void testParseFilterDateRange() throws BadRequestException {
        ImmutablePair<Long, Long> issueDueAt = DefaultListRequestUtils.getTimeRange(DefaultListRequest.builder()
                .filter(Map.of("issue_due_at", Map.of("$gt", "1626912000", "$lt", "1628208000")))
                .build(), "", "issue_due_at");
        Assertions.assertThat(issueDueAt.getLeft()).isEqualTo(1626912000L);
        Assertions.assertThat(issueDueAt.getRight()).isEqualTo(1628208000L);

        issueDueAt = DefaultListRequestUtils.getTimeRange(DefaultListRequest.builder()
                .filter(Map.of("issue_due_at", Map.of("$gte", "1626912000", "$lte", "1628208000")))
                .build(), "", "issue_due_at");
        Assertions.assertThat(issueDueAt.getLeft()).isEqualTo(1626911999L);
        Assertions.assertThat(issueDueAt.getRight()).isEqualTo(1628208001L);

        issueDueAt = DefaultListRequestUtils.getTimeRange(DefaultListRequest.builder()
                .filter(Map.of("issue_due_at", Map.of("$gt", "1626912000", "$lte", "1628208000")))
                .build(), "", "issue_due_at");
        Assertions.assertThat(issueDueAt.getLeft()).isEqualTo(1626912000L);
        Assertions.assertThat(issueDueAt.getRight()).isEqualTo(1628208001L);

        issueDueAt = DefaultListRequestUtils.getTimeRange(DefaultListRequest.builder()
                .filter(Map.of("issue_due_at", Map.of("$gte", "1626912000", "$lte", "1628208000", "$gt", "1526913000", "$lt", "1528209000")))
                .build(), "", "issue_due_at");
        Assertions.assertThat(issueDueAt.getLeft()).isEqualTo(1526913000L);
        Assertions.assertThat(issueDueAt.getRight()).isEqualTo(1528209000L);

        issueDueAt = DefaultListRequestUtils.getTimeRange(DefaultListRequest.builder()
                .filter(Map.of("issue_due_at", Map.of("$gte", "1626912000", "$lte", "1628208000", "$gt", "1526913000")))
                .build(), "", "issue_due_at");
        Assertions.assertThat(issueDueAt.getLeft()).isEqualTo(1526913000L);
        Assertions.assertThat(issueDueAt.getRight()).isEqualTo(1628208001L);

        // test with longs
        issueDueAt = DefaultListRequestUtils.getTimeRange(DefaultListRequest.builder()
                .filter(Map.of("issue_due_at", Map.of("$gte", 1626912000L, "$lte", 1628208000L, "$gt", 1526913000L)))
                .build(), "", "issue_due_at");
        Assertions.assertThat(issueDueAt.getLeft()).isEqualTo(1526913000L);
        Assertions.assertThat(issueDueAt.getRight()).isEqualTo(1628208001L);

        // empty
        issueDueAt = DefaultListRequestUtils.getTimeRange(DefaultListRequest.builder()
                .filter(Map.of("issue_due_at", Map.of()))
                .build(), "", "issue_due_at");
        Assertions.assertThat(issueDueAt).isNotNull();
        Assertions.assertThat(issueDueAt.getLeft()).isNull();
        Assertions.assertThat(issueDueAt.getRight()).isNull();

        issueDueAt = DefaultListRequestUtils.getTimeRange(DefaultListRequest.builder()
                .filter(Map.of())
                .build(), "", "issue_due_at");
        Assertions.assertThat(issueDueAt).isNotNull();
        Assertions.assertThat(issueDueAt.getLeft()).isNull();
        Assertions.assertThat(issueDueAt.getRight()).isNull();

        // test something wrong
        assertThatThrownBy(() -> DefaultListRequestUtils.getTimeRange(DefaultListRequest.builder()
                .filter(Map.of("issue_due_at", Map.of("$gt", Map.of("test", "test"), "$lt", 1628208000L)))
                .build(), "", "issue_due_at"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("issue_due_at field");

        // test something wrong 2
        assertThatThrownBy(() -> DefaultListRequestUtils.getTimeRange(DefaultListRequest.builder()
                .filter(Map.of("issue_due_at", 123L))
                .build(), "", "issue_due_at"))
                .isInstanceOf(ClassCastException.class);
    }

}