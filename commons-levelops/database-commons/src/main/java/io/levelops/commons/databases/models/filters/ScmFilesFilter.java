package io.levelops.commons.databases.models.filters;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.caching.CacheHashUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
public class ScmFilesFilter {
    DISTINCT across;
    CALCULATION calculation;
    List<String> repoIds;
    List<String> projects;
    List<String> integrationIds;
    Boolean listFiles;
    String filename;
    String module;
    Long commitStartTime;
    Long commitEndTime;
    Map<String, Map<String, String>> partialMatch;
    Map<String, SortingOrder> sort;
    Set<UUID> orgProductIds;

    List<String> excludeRepoIds;
    List<String> excludeProjects;

    public enum DISTINCT {
        repo_id,
        project;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        count; // just a sum of the changes

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }
    }


    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        if (across != null)
            dataToHash.append("across=").append(across);
        if (calculation != null)
            dataToHash.append(",calculation=").append(calculation);
        if (listFiles != null)
            dataToHash.append(",listFiles=").append(listFiles);
        if (filename != null)
            dataToHash.append(",filename=").append(filename);
        if (module != null)
            dataToHash.append(",module=").append(module);
        if (commitStartTime != null)
            dataToHash.append(",commitStartTime=").append(commitStartTime);
        if (commitEndTime != null)
            dataToHash.append(",commitEndTime=").append(commitEndTime);
        if (CollectionUtils.isNotEmpty(repoIds)) {
            ArrayList<String> tempList = new ArrayList<>(repoIds);
            Collections.sort(tempList);
            dataToHash.append(",repoIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            ArrayList<String> tempList = new ArrayList<>(projects);
            Collections.sort(tempList);
            dataToHash.append(",projects=").append(String.join(",", tempList));
        }
        if(CollectionUtils.isNotEmpty(orgProductIds)) {
            ArrayList<String> tempList = orgProductIds.stream().sorted().map(UUID::toString)
                    .collect(Collectors.toCollection(ArrayList::new));
            dataToHash.append(",ordProductIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeRepoIds)) {
            ArrayList<String> tempList = new ArrayList<>(excludeRepoIds);
            Collections.sort(tempList);
            dataToHash.append(",excludeRepoIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(excludeProjects)) {
            ArrayList<String> tempList = new ArrayList<>(excludeProjects);
            Collections.sort(tempList);
            dataToHash.append(",excludeProjects=").append(String.join(",", tempList));
        }
        if (MapUtils.isNotEmpty(partialMatch)) {
            TreeSet<String> fields = new TreeSet<>(partialMatch.keySet());
            dataToHash.append(",partialMatch=(");
            for (String field : fields) {
                Map<String, String> innerMap = partialMatch.get(field);
                TreeSet<String> innerFields = new TreeSet<>(innerMap.keySet());
                dataToHash.append("(");
                for (String innerField : innerFields) {
                    dataToHash.append(innerField).append("=").append(innerMap.get(innerField)).append(",");
                }
                dataToHash.append("),");
            }
            dataToHash.append(")");
        }
        if(MapUtils.isNotEmpty(sort)) {
            TreeSet<String> fields = new TreeSet<>(sort.keySet());
            dataToHash.append(",sort=(");
            for (String field : fields) {
                dataToHash.append(field.toLowerCase(Locale.ROOT)).append("=")
                        .append(sort.get(field).toString().toLowerCase(Locale.ROOT));
            }
            dataToHash.append(")");
        }
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }
}
