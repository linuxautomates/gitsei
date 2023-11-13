package io.levelops.commons.databases.services.dev_productivity.utils;

import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Log4j2
public class FeatureHandlerUtil {

    public static List<ImmutablePair<Long, Long>> getTimePartitionByInterval(String aggInterval, Instant lowerBound, Instant upperBound) {
        List<ImmutablePair<Long, Long>> timePartition = List.of();
        switch (aggInterval) {
            case "day":
                timePartition = io.levelops.commons.dates.DateUtils.getDailyPartition(lowerBound, upperBound);
                break;
            case "week":
                timePartition = io.levelops.commons.dates.DateUtils.getWeeklyPartition(lowerBound, upperBound);
                break;
            case "month":
                timePartition = io.levelops.commons.dates.DateUtils.getMonthlyPartition(lowerBound, upperBound);
                break;
            case "year":
                timePartition = io.levelops.commons.dates.DateUtils.getYearlyPartition(lowerBound, upperBound);
                break;
            default:
                try {
                    throw new BadRequestException("Interval not supported: " + aggInterval);
                } catch (BadRequestException e) {
                    e.printStackTrace();
                }
        }
        return timePartition;
    }

    public static boolean useEs(String company, Set<String> dbAllowedTenants, DevProductivityFilter filter) {
        Boolean forceSourceUseES = isForceSourceEs(filter);
        if(forceSourceUseES != null) {
            log.info("isUseEs forceSourceUseES={}", forceSourceUseES);
            return forceSourceUseES;
        }

        boolean isDBCompany = dbAllowedTenants.contains(company);
        log.info("isUseEs isDBCompany={}", isDBCompany);

        return !isDBCompany;
    }

    private static Boolean isForceSourceEs(DevProductivityFilter filter) {

        if(StringUtils.equals(filter.getForceSource(), "es")) {
            return true;
        }
        if(StringUtils.equals(filter.getForceSource(), "db")) {
            return false;
        }
        return null;
    }
}
