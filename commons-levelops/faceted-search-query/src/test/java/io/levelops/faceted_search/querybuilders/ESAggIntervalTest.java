package io.levelops.faceted_search.querybuilders;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ESAggIntervalTest {
    @Test
    public void test() {
        Long epochSecondsTestCase1 = 1685923200l;
        Map<ESAggInterval, String> expectedTestCase1 = new HashMap<>();
        expectedTestCase1.put(ESAggInterval.YEAR, "2023");
        expectedTestCase1.put(ESAggInterval.QUARTER, "2-2023");
        expectedTestCase1.put(ESAggInterval.BIWEEKLY, "23-2023");
        expectedTestCase1.put(ESAggInterval.WEEK, "23-2023");
        expectedTestCase1.put(ESAggInterval.MONTH, "06-2023");
        expectedTestCase1.put(ESAggInterval.DAY, "05-06-2023");
        for(ESAggInterval e : ESAggInterval.values()) {
            if (StringUtils.isEmpty(e.getFormat())) {
                continue;
            }
            Assert.assertEquals(expectedTestCase1.get(e), e.formatEpochSeconds(epochSecondsTestCase1));
        }
    }
}