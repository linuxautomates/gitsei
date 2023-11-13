package io.levelops.api.controllers;

import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.DefaultListRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner .class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class PredictionsServiceTest {

    @Before
    public void setup() {
    }

    private static final Map<String, Long> EPPOCH_INTERVAL = new HashMap<>() {{
        put("day", 86400L * 1000L);
        put("week", 604800L * 1000L);
        put("month", 2629743L * 1000L);
        put("quarter", 2629743L * 1000L * 3L);
        put("year", 31556926L * 1000L);
    }};

    public DbAggregationResult getAggFromKeyAndDP(String across,
                                                  Double dp,
                                                  Long epochKey) {
        BiFunction<Double,
                DbAggregationResult.DbAggregationResultBuilder,
                DbAggregationResult> func = DbAggregationResult.SET_MAP.get(across);
        if (func == null) {
            return DbAggregationResult.builder().build();
        }
        return func.apply(dp, DbAggregationResult.builder().key(String.valueOf(epochKey)));
    }


    public List<DbAggregationResult> createDBAggregations(String key,
                                                          List<Double> dataPoints,
                                                          String interval){
        String str = "Jun 13 2003";
        SimpleDateFormat df = new SimpleDateFormat("MMM dd yyyy");
        Date date;
        try {
            date = df.parse(str);
        } catch (Exception e) {
            date = new Date(0);
        }
        long epoch = date.getTime();
        long epoch_interval = EPPOCH_INTERVAL.get(interval);
        List<DbAggregationResult> outList = IntStream.range(0, dataPoints.size())
                .mapToObj(i -> getAggFromKeyAndDP(key,
                        dataPoints.get(i),
                        epoch + i * epoch_interval))
                .collect(Collectors.toList());
        return outList;
    }

    @Test
    public void testOnKnownKey() {
        String across = "total_tickets";
        String interval = "day";
        List<Double> dataPoints = List.of(1.0, 2.0, 1.0, 1.5, 2.5,
                3.8, 7.5, 6.4, 3.2, 0.0, 0.0,
                1.2, 3.5, 7.2, 1.2, 2.2, 0.8,
                2.1, 2.2, 3.0, 5.0, 4.0, 5.0,
                3.2, 3.37, 22.0, 22.1, 15.0, 12.0,
                6.0, 6.5, 2.3, 3.5, 5.5, 2.3,
                2.2, 1.2, 0.7, 0.9, 2.2, 6.7
                );
        List<DbAggregationResult> aggs = createDBAggregations(across,
                dataPoints,
                interval);
        List<DbAggregationResult> newAggs = PredictionsService.postProcess(aggs,
                DefaultListRequest.builder().aggInterval(interval).build());
        assertTrue(newAggs.get(40).getBollingerAvg() == 6.25);
        assertTrue(newAggs.get(40).getBollingerStd() > 6.3077333488);
        assertTrue(newAggs.get(31).getBollingerAvg() == 6.35);
        assertTrue(newAggs.get(20).getBollingerAvg() == 2.65);
        assertTrue(newAggs.get(45).getPredictionUpperBound() != null);
        assertTrue(newAggs.get(45).getPredictionUpperBound() >
                newAggs.get(45).getPredictionLowerBound());
    }

    @Test
    public void testOnUnknownKey() {
        String across = "count";
        String interval = "week";
        List<Double> dataPoints = List.of(1.0, 2.0, 1.0, 1.5, 2.5,
                3.8, 7.5, 6.4, 3.2, 0.0, 0.0,
                1.2, 3.5, 7.2, 1.2, 2.2, 0.8,
                2.1, 2.2, 3.0, 5.0, 4.0, 5.0,
                3.2, 3.37, 22.0, 22.1, 15.0, 12.0,
                6.0, 6.5, 2.3, 3.5, 5.5, 2.3,
                2.2, 1.2, 0.7, 0.9, 2.2, 6.7
        );
        List<DbAggregationResult> aggs = createDBAggregations(across,
                dataPoints,
                interval);
        List<DbAggregationResult> newAggs = PredictionsService.postProcess(aggs,
                DefaultListRequest.builder().aggInterval(interval).build());
        assertTrue(newAggs.get(40).getBollingerAvg() == 6.25);
        assertTrue(newAggs.get(40).getBollingerStd() > 6.3077333488);
        assertTrue(newAggs.get(31).getBollingerAvg() == 6.35);
        assertTrue(newAggs.get(20).getBollingerAvg() == 2.65);
        assertTrue(newAggs.get(45).getPredictionUpperBound() != null);
        assertTrue(newAggs.get(45).getPredictionUpperBound() >
                newAggs.get(45).getPredictionLowerBound());
    }

    @Test
    public void testOnEmptyInput() {
        String across = "";
        String interval = "day";
        List<Double> dataPoints = List.of();
        List<DbAggregationResult> aggs = createDBAggregations(across,
                dataPoints,
                interval);
        List<DbAggregationResult> newAggs = PredictionsService.postProcess(aggs,
                DefaultListRequest.builder().aggInterval(interval).build());
        assertEquals(newAggs.size(), 0);
    }

    @Test
    public void testOnNullInput() {
        String interval = "day";
        List<DbAggregationResult> newAggs = PredictionsService.postProcess(null,
                DefaultListRequest.builder().aggInterval(interval).build());
        assertEquals(newAggs, null);
    }

    @Test
    public void testOnShortInput() {
        String interval = "day";
        String across = "";
        List<Double> dataPoints = List.of(1.0, 2.0, 1.0, 1.5, 2.5,
                3.8, 7.5);
        List<DbAggregationResult> aggs = createDBAggregations(across, dataPoints, interval);
        List<DbAggregationResult> newAggs = PredictionsService.postProcess(aggs,
                DefaultListRequest.builder().aggInterval(interval).build());
        assertEquals(aggs, newAggs);
    }

    @Test
    public void testOnIncreasingInput() {
        String interval = "month";
        String across = "count";
        List<Double> dataPoints = List.of(1.0, 2.0, 3.0, 4.0, 5.0,
                6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0,
                17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0, 25.0, 26.0, 27.0,
                28.0, 29.0, 30.0);
        List<DbAggregationResult> aggs = createDBAggregations(across, dataPoints, interval);
        List<DbAggregationResult> newAggs = PredictionsService.postProcess(aggs,
                DefaultListRequest.builder().aggInterval(interval).build());
        assertTrue(newAggs.get(38).getPredictionLowerBound() > 38.5);
        assertTrue(newAggs.get(38).getPredictionUpperBound() < 39.5);
    }




}
