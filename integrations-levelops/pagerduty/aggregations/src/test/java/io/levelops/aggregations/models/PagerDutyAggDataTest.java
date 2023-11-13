package io.levelops.aggregations.models;

import io.levelops.commons.utils.ResourceUtils;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

@Log4j2
public class PagerDutyAggDataTest {
    @Test
    public void test() throws IOException {
        var data = ResourceUtils.getResourceAsObject("models/pager_duty_agg_data.json", PagerDutyAggData.class);
        log.info(data);
        Assertions.assertThat(data).isNotNull();
        Assertions.assertThat(data.aggIncidents()).containsExactlyInAnyOrderEntriesOf(Map.of("i1", 13));
        Assertions.assertThat(data.aggIncidentsByUrgency()).containsExactlyInAnyOrderEntriesOf(Map.of("low", 10, "high", 3));
        Assertions.assertThat(data.aggIncidentsByPriority()).containsExactlyInAnyOrderEntriesOf(Map.of("P9", 1, "P10", 12));
        Assertions.assertThat(data.aggIncidentsByUrgencyPriority()).containsExactlyInAnyOrderEntriesOf(Map.of("low_P9", 1, "low_P10", 9, "high_P10", 3));
        Assertions.assertThat(data.aggAlerts()).containsExactlyInAnyOrderEntriesOf(Map.of("A1", 3));
        Assertions.assertThat(data.aggAlertsBySeverity()).containsExactlyInAnyOrderEntriesOf(Map.of("s1", 1, "s2", 2));
        Assertions.assertThat(data.timeSeries()).isNotNull();
        Assertions.assertThat(data.timeSeries()).hasSize(1);
        var timeSeries = data.timeSeries().iterator().next();
        log.info("Time Series: \n{}",timeSeries);
        Assertions.assertThat(timeSeries.from()).isEqualTo(1592419996);
        Assertions.assertThat(timeSeries.to()).isEqualTo(1592419997);
        Assertions.assertThat(timeSeries.byIncidentUrgency()).containsExactlyInAnyOrderEntriesOf(Map.of("low", 1, "high", 3));
        Assertions.assertThat(timeSeries.byAlertSeverity()).containsExactlyInAnyOrderEntriesOf(Map.of("info", 0, "warning", 2, "error", 1, "critical", 0));
        Assertions.assertThat(timeSeries.byIncidentAcknowledged()).isEqualTo(SimpleTimeSeriesData.builder().low(0).medium(0).high(0).build());
        Assertions.assertThat(timeSeries.byIncidentResolved()).isEqualTo(SimpleTimeSeriesData.builder().low(1).medium(6).high(8).build());
        Assertions.assertThat(timeSeries.byAlertResolved()).isEqualTo(SimpleTimeSeriesData.builder().low(1).medium(6).high(2).build());
    }
}