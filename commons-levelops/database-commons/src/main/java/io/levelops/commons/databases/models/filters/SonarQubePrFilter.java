package io.levelops.commons.databases.models.filters;

import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.UUID;

@Log4j2
@Value
@Builder(toBuilder = true)
public class SonarQubePrFilter {

    DISTINCT across;
    CALCULATION calculation;
    AGG_INTERVAL aggInterval;

    List<String> keys;
    List<String> branches;
    List<String> titles;
    List<String> authors;
    List<String> creators;
    List<String> committer;
    List<String> integrationIds;
    List<UUID> committerIds;
    List<UUID> creatorIds;

    ImmutablePair<Long, Long> commitCreatedRange;
    ImmutablePair<Long, Long> prCreatedRange;

    public enum DISTINCT{
        key,
        branch,
        pr_created,
        committed_at;
        public static SonarQubePrFilter.DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(SonarQubePrFilter.DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        count; // just a count of rows
        public static SonarQubePrFilter.CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(SonarQubePrFilter.CALCULATION.class, st);
        }
    }
}
