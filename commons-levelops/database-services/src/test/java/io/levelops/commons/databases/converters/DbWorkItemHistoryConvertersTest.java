package io.levelops.commons.databases.converters;

import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DbWorkItemHistoryConvertersTest {

    @Test
    public void testSanitizeEventList() {
        List<DbWorkItemHistory> events = List.of(DbWorkItemHistory.builder().startDate(new Timestamp(10)).endDate(null).build());
        List<DbWorkItemHistory> output = DbWorkItemHistoryConverters.sanitizeEventList(events, Instant.ofEpochMilli(1000));
        assertThat(output).containsExactly(DbWorkItemHistory.builder().startDate(new Timestamp(10)).endDate(new Timestamp(1000)).build());

        events = List.of(
                DbWorkItemHistory.builder().startDate(new Timestamp(10)).endDate(null).build(),
                DbWorkItemHistory.builder().startDate(new Timestamp(20)).endDate(null).build());
        output = DbWorkItemHistoryConverters.sanitizeEventList(events, Instant.ofEpochMilli(1000));
        assertThat(output).containsExactly(
                DbWorkItemHistory.builder().startDate(new Timestamp(10)).endDate(new Timestamp(20)).build(),
                DbWorkItemHistory.builder().startDate(new Timestamp(20)).endDate(new Timestamp(1000)).build());
    }

}