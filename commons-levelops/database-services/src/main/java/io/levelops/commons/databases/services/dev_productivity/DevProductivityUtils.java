package io.levelops.commons.databases.services.dev_productivity;

import io.levelops.commons.databases.models.database.dev_productivity.*;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class DevProductivityUtils {

    public static List<RelativeScore> getRelativeScores(List<List<RelativeDevProductivityReport>> allReports) {

        List<RelativeScore> scoreList = new ArrayList<>();

        for(List<RelativeDevProductivityReport> reportList : allReports){

            for(RelativeDevProductivityReport report : reportList){

                Predicate<RelativeScore> predicate = report.getStartTime() == null ? r -> r.getInterval().equals(report.getInterval()) :
                        r -> r.getKey().equals(report.getStartTime().getEpochSecond());

                RelativeScore scoreForInterval = scoreList.stream().filter(r -> predicate.test(r)).findFirst().orElse(null);

                if (scoreForInterval != null) {

                    List<RelativeDevProductivityReport> reports = new ArrayList<>(scoreForInterval.getReportList());
                    reports.add(report);

                    AtomicInteger i = new AtomicInteger();
                    int index = scoreList.stream()
                            .peek(v -> i.incrementAndGet())
                            .anyMatch(r -> predicate.test(r)) ?
                            i.get() - 1 : -1;

                    scoreForInterval = scoreForInterval.toBuilder()
                            .reportList(reports)
                            .build();
                    scoreList.set(index, scoreForInterval);

                }else {

                    Long startTime = null;
                    Instant startTimeInstant = report.getStartTime();
                    if(startTimeInstant ==  null){
                        startTime = report.getInterval().getIntervalTimeRange(Instant.now()).getTimeRange().getKey();
                        startTimeInstant =   Instant.ofEpochSecond(report.getInterval().getIntervalTimeRange(Instant.now()).getTimeRange().getKey());
                    }else
                        startTime = report.getStartTime().getEpochSecond();

                    RelativeScore score = RelativeScore.builder()
                            .key(startTime)
                            .additionalKey(startTimeInstant.atZone(ZoneId.systemDefault()).getMonthValue() + "-" + startTimeInstant.atZone(ZoneId.systemDefault()).getYear() )
                            .interval(report.getInterval())
                            .reportList(List.of(report))
                            .build();
                    scoreList.add(score);

                }
            }
        }

        return scoreList;
    }
}
