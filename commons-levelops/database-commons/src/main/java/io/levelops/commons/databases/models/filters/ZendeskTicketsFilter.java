package io.levelops.commons.databases.models.filters;

import io.levelops.commons.caching.CacheHashUtils;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
public class ZendeskTicketsFilter {

    List<String> brands;
    List<String> types;
    List<String> priorities;
    List<String> statuses;
    List<String> organizations;
    List<String> requesterEmails;
    List<String> submitterEmails;
    List<String> assigneeEmails;
    Map<String, Object> age;
    List<String> integrationIds;
    Long ticketCreatedStart;
    Long ticketCreatedEnd;
    AGG_INTERVAL aggInterval;
    @NonNull
    Long ingestedAt;
    Map<String, List<String>> excludeCustomFields;
    Map<String, List<String>> customFields;
    String customAcross;
    List<String> customStacks;

    List<EXTRA_CRITERIA> extraCriteria;

    DISTINCT DISTINCT;

    CALCULATION CALCULATION;

    public enum DISTINCT {
        brand,
        type,
        priority,
        status,
        custom_field,
        organization,
        assignee,
        requester,
        submitter,
        //these are across time
        ticket_created,
        ticket_updated,
        assigned,
        solved,
        due,
        assignee_updated,
        trend;

        public static DISTINCT fromString(String across) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, across);
        }
    }

    public enum CALCULATION {
        hops,
        bounces,
        response_time,
        resolution_time,
        ticket_count,
        agent_wait_time,
        requester_wait_time,
        reopens,
        replies;

        public static CALCULATION fromString(String output) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, output);
        }
    }

    public enum EXTRA_CRITERIA {
        idle,
        no_due_date,
        no_brand,
        no_assignee,
        missed_resolution_time;

        public static EXTRA_CRITERIA fromString(String criterion) {
            return EnumUtils.getEnumIgnoreCase(EXTRA_CRITERIA.class, criterion);
        }
    }

    public String generateCacheRawString() {
        StringBuilder dataToHash = new StringBuilder();
        if (DISTINCT != null)
            dataToHash.append("across=").append(DISTINCT);
        if (CALCULATION != null)
            dataToHash.append(",calculation=").append(CALCULATION);
        if (customAcross != null)
            dataToHash.append(",customAcross=").append(customAcross);
        if (ingestedAt != null)
            dataToHash.append(",ingestedAt=").append(ingestedAt);
        if (ticketCreatedStart != null)
            dataToHash.append(",ticketCreatedStart=").append(ticketCreatedStart);
        if (ticketCreatedEnd != null)
            dataToHash.append(",ticketCreatedEnd=").append(ticketCreatedEnd);
        if (aggInterval != null)
            dataToHash.append(",aggInterval=").append(aggInterval);

        if (CollectionUtils.isNotEmpty(extraCriteria)) {
            List<String> critStr = extraCriteria.stream().map(Enum::toString).sorted().collect(Collectors.toList());
            dataToHash.append(",extraCriteria=").append(String.join(",", critStr));
        }
        if (CollectionUtils.isNotEmpty(brands)) {
            ArrayList<String> tempList = new ArrayList<>(brands);
            Collections.sort(tempList);
            dataToHash.append(",brands=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(types)) {
            ArrayList<String> tempList = new ArrayList<>(types);
            Collections.sort(tempList);
            dataToHash.append(",types=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(priorities)) {
            ArrayList<String> tempList = new ArrayList<>(priorities);
            Collections.sort(tempList);
            dataToHash.append(",priorities=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            ArrayList<String> tempList = new ArrayList<>(statuses);
            Collections.sort(tempList);
            dataToHash.append(",statuses=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(organizations)) {
            ArrayList<String> tempList = new ArrayList<>(organizations);
            Collections.sort(tempList);
            dataToHash.append(",organizations=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(requesterEmails)) {
            ArrayList<String> tempList = new ArrayList<>(requesterEmails);
            Collections.sort(tempList);
            dataToHash.append(",requesterEmails=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(submitterEmails)) {
            ArrayList<String> tempList = new ArrayList<>(submitterEmails);
            Collections.sort(tempList);
            dataToHash.append(",submitterEmails=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(assigneeEmails)) {
            ArrayList<String> tempList = new ArrayList<>(assigneeEmails);
            Collections.sort(tempList);
            dataToHash.append(",assigneeEmails=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            ArrayList<String> tempList = new ArrayList<>(integrationIds);
            Collections.sort(tempList);
            dataToHash.append(",integrationIds=").append(String.join(",", tempList));
        }
        if (CollectionUtils.isNotEmpty(customStacks)) {
            ArrayList<String> tempList = new ArrayList<>(customStacks);
            Collections.sort(tempList);
            dataToHash.append(",customStacks=").append(String.join(",", tempList));
        }
        if (MapUtils.isNotEmpty(age)) {
            dataToHash.append(",age=");
            if (age.get("$gt") != null)
                dataToHash.append(age.get("$gt").toString()).append("-");
            if (age.get("$lt") != null)
                dataToHash.append(age.get("$lt").toString());
        }
        if (MapUtils.isNotEmpty(customFields)) {
            TreeSet<String> fields = new TreeSet<>(customFields.keySet());
            dataToHash.append(",customfields=(");
            for (String field : fields) {
                List<String> data = new ArrayList<>(customFields.get(field));
                Collections.sort(data);
                dataToHash.append(field).append("=").append(String.join(",", data)).append(",");
            }
            dataToHash.append(")");
        }
        if (MapUtils.isNotEmpty(excludeCustomFields)) {
            TreeSet<String> fields = new TreeSet<>(excludeCustomFields.keySet());
            dataToHash.append(",excludeCustomFields=(");
            for (String field : fields) {
                List<String> data = new ArrayList<>(excludeCustomFields.get(field));
                Collections.sort(data);
                dataToHash.append(field).append("=").append(String.join(",", data)).append(",");
            }
            dataToHash.append(")");
        }
        return dataToHash.toString();
    }

    public String generateCacheHash() {
        return CacheHashUtils.generateCacheHash(generateCacheRawString());
    }

}
