package io.levelops.integrations.pagerduty.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;

public class PagerDutyUtilsTest {

    @Test
    public void test(){
        var reference = Instant.parse("1995-10-22T04:12:35+00:00");
        var pagerDutyDate = PagerDutyUtils.formatDate(Date.from(reference));

        Assertions.assertThat(pagerDutyDate).isEqualTo("1995-10-22T04:12:35Z");

        var d = 1613075266543l;
        // reference = new Date(d);
        var s = "2021-02-11T20:27:46Z";
        pagerDutyDate = PagerDutyUtils.formatDate(d);
        Assertions.assertThat(pagerDutyDate).isEqualTo(s);

    }

}