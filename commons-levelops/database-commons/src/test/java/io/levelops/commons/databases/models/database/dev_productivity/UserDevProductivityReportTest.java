package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

public class UserDevProductivityReportTest {
    public static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    @Test
    public void test() throws JsonProcessingException {
        ReportIntervalType i = ReportIntervalType.LAST_MONTH;
        Instant now = Instant.now();
        ImmutablePair<Long, Long> timeRange = i.getIntervalTimeRange(now).getTimeRange();
        Instant startTime = Instant.ofEpochSecond(timeRange.getLeft());
        Instant endTime = Instant.ofEpochSecond(timeRange.getRight());

        UserDevProductivityReport userDevProductivityReport = UserDevProductivityReport.builder()
                .id(UUID.randomUUID())
                .interval(i).startTime(startTime).endTime(endTime)
                .report(DevProductivityResponse.builder()
                        .interval(i).startTime(startTime.getEpochSecond()).endTime(endTime.getEpochSecond())
                        .resultTime(now.getEpochSecond())
                        .build())
                .build();
        String s = MAPPER.writeValueAsString(userDevProductivityReport);
        Assert.assertNotNull(s);
        UserDevProductivityReport actual = MAPPER.readValue(s, UserDevProductivityReport.class);
        Assert.assertEquals(userDevProductivityReport, actual);
    }
}