package io.levelops.api.controllers;

import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.DefaultListRequest;
import lombok.extern.log4j.Log4j2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;
import timeseries.TimePeriod;
import timeseries.TimeSeries;
import timeseries.models.Forecast;
import timeseries.models.arima.Arima;
import timeseries.models.arima.ArimaOrder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Log4j2
@SuppressWarnings("unused")
public class PredictionsService {

    private static final Integer WINDOW_SIZE = 20;
    private static final Integer PREDICTION_WINDOW = 10;
    private static final Integer ARIMA_p = WINDOW_SIZE;
    private static final Integer ARIMA_d = 0;
    private static final Integer ARIMA_q = WINDOW_SIZE;
    private static final Integer ARIMA_P = 7;
    private static final Integer ARIMA_D = 0;
    private static final Integer ARIMA_Q = 7;

    public static final String TIME_ZONE = "America/Los_Angeles";

    public static final Map<String, DateTimeFormatter> DATE_FORMAT_MAP = new HashMap<>() {{
        put("day", DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        put("week", DateTimeFormatter.ofPattern("ww-yyyy"));
        put("month", DateTimeFormatter.ofPattern("MM-yyyy"));
        put("quarter", DateTimeFormatter.ofPattern("QQ-yyyy"));
        put("year", DateTimeFormatter.ofPattern("yyyy"));
    }};

    private static Double getValFromKey(DbAggregationResult agg, String key) {
        Function<DbAggregationResult, Double> getFunc = DbAggregationResult.GET_MAP.get(key);
        if (getFunc == null) {
            return agg.getTotal().doubleValue();
        }
        return getFunc.apply(agg);
    }

    private static List<Tuple3<Double,Double,Double>> predict(List<Double> predictionInputs,
                                                              int prediction_window) {
        if (predictionInputs.size() < ARIMA_p) {
            return new ArrayList<>();
        }
        double[] input = new double[predictionInputs.size()];
        for (int i = 0; i < predictionInputs.size(); i ++) {
            input[i] = (double)predictionInputs.get(i);
        }
        TimeSeries tSeries = new TimeSeries(TimePeriod.oneDay(),
                OffsetDateTime.now().minusDays((long)predictionInputs.size()),
                input);
        Arima arimaModel = Arima.model(tSeries,
                ArimaOrder.order(ARIMA_p, ARIMA_d, ARIMA_q),
                TimePeriod.oneWeek(),
                Arima.FittingStrategy.CSS);

        Forecast forecast = arimaModel.forecast(prediction_window, 0.3);

        List<Double> midForecast = forecast.forecast().asList();
        List<Double>  upperForecast = forecast.upperPredictionValues().asList();
        List<Double>  lowerForecast = forecast.lowerPredictionValues().asList();
        List<Tuple3<Double, Double, Double>> out = IntStream.range(0, prediction_window)
                .mapToObj(i -> Tuples.of(lowerForecast.get(i), midForecast.get(i), upperForecast.get(i)))
                .collect(Collectors.toList());
        return out;

    }

    private static List<Double> getWindow(List<Double> vals,
                                 int start,
                                 int windowSize) {
        return start >= windowSize ?
                vals.subList(start - windowSize, start) :
                List.of(0.0);
    }

    private static double getAvg(List<Double> window) {
        return window.stream()
                .mapToDouble(d -> d)
                .average()
                .orElse(0.0);
    }

    private static double getStd(List<Double> window, double avg) {
        return Math.sqrt(window.stream()
                    .mapToDouble(d -> d)
                    .map(d -> (d - avg) * (d - avg))
                    .average()
                    .orElse(0.0));
    }

    private static String scanForAcross(DbAggregationResult aggResult) {
        for (String across : DbAggregationResult.PREDICTIONS_FIELDS) {
            if (DbAggregationResult.GET_MAP.get(across).apply(aggResult) != null) {
                return across;
            }
        }
        return null;
    }

    private static List<String> getPredictionKeys(List<DbAggregationResult> aggResult,
                                                  int predictionWindow) {
        String key1 = aggResult.get(aggResult.size() - 1).getKey();
        String key2 = aggResult.get(aggResult.size() - 2).getKey();
        Long diff = Long.valueOf(key1) - Long.valueOf(key2);
        return IntStream.range(1, predictionWindow + 1)
                .mapToObj(i -> String.valueOf(Long.valueOf(key1) + (diff * i)))
                .collect(Collectors.toList());
    }

    private static List<String> getAdditionalKeys(List<String> keys,
                                     DefaultListRequest filter){
        DateTimeFormatter format = DATE_FORMAT_MAP.get(filter.getAggInterval());
        if (format == null) {
            format = DATE_FORMAT_MAP.get("day");
        }
        format = format.withZone(ZoneId.of(TIME_ZONE));
        final DateTimeFormatter formatter = format;
        return keys.stream().map(
                k -> formatter.format(Instant.ofEpochMilli(Long.valueOf(k)))
        ).collect(Collectors.toList());
    }


    //Can't use streams because 'getLastN' is a window function and there is
    //No automated API for that in stream land as near as I can tell.
    //This function performs Bollinger Bounds and Arima Predictions on incoming datasets
    public static List<DbAggregationResult> postProcess(List<DbAggregationResult> aggResult,
                                                        DefaultListRequest filter) {
        try {
            String across;
            if (aggResult != null && aggResult.size() >= WINDOW_SIZE) {
                across = scanForAcross(aggResult.get(0));
            } else {
                return aggResult;
            }

            if (across == null) {
                return aggResult;
            }

            List<Double> vals = aggResult.stream()
                    .map(agg -> (Double)getValFromKey(agg, across))
                    .collect(Collectors.toList());

            List<DbAggregationResult> out = IntStream.range(0, aggResult.size())
                    .mapToObj(i -> {
                        List<Double> window = getWindow(vals, i, WINDOW_SIZE);
                        double avg = getAvg(window);
                        double std = getStd(window, avg);
                        return aggResult.get(i).toBuilder()
                                .bollingerAvg(avg)
                                .bollingerStd(std)
                                .build();
                    })
                    .collect(Collectors.toList());

            List<Double> predictionInputs = vals;
            List<String> keys = getPredictionKeys(aggResult, PREDICTION_WINDOW);
            List<String> additionalKeys = getAdditionalKeys(keys, filter);
            List<Tuple3<Double, Double, Double>> p_vals =
                    predict(predictionInputs, PREDICTION_WINDOW);
            List<DbAggregationResult> predictions = IntStream.range(0, p_vals.size())
                    .mapToObj(i -> DbAggregationResult.builder()
                            .predictionLowerBound(p_vals.get(i).getT1())
                            .prediction(p_vals.get(i).getT2())
                            .predictionUpperBound(p_vals.get(i).getT3())
                            .key(keys.get(i))
                            .additionalKey(additionalKeys.get(i))
                            .build())
                    .collect(Collectors.toList());

            out.addAll(predictions);
            return out;
        } catch (Exception e) {
            log.warn("Failure in JiraIssuesController post process: " + e.toString());
            return aggResult;
        }
    }


}
