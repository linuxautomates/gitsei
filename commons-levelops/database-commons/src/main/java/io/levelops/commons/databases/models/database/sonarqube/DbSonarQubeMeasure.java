package io.levelops.commons.databases.models.database.sonarqube;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.integrations.sonarqube.models.Measure;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbSonarQubeMeasure.DbSonarQubeMeasureBuilder.class)
public class DbSonarQubeMeasure {

    private static final String UNKNOWN = "_UNKNOWN_";

    @JsonProperty("id")
    String id;

    @JsonProperty("parent_id")
    String parentId;

    @JsonProperty("name")
    String name;

    @JsonProperty("value")
    String value;

    @JsonProperty("data_type")
    String dataType;

    @JsonProperty("ingested_at")
    Date ingestedAt;

    @JsonProperty("parent")
    String parent;

    @JsonProperty("repo")
    String repo;

    public static DbSonarQubeMeasure fromMeasure(Measure measure, Date ingestedAt) {
        if (measure.getValue() == null) {
            return null;
        }
        return DbSonarQubeMeasure.builder()
                .name(measure.getMetric())
                .value(measure.getValue())
                .dataType(MoreObjects.firstNonNull(measure.getDataType(), UNKNOWN))
                .ingestedAt(DateUtils.truncate(ingestedAt, Calendar.DATE))
                .build();
    }

    public static List<DbSonarQubeMeasure> addParentIdToBatch(String parentId, List<DbSonarQubeMeasure> measures) {
        return ListUtils.emptyIfNull(measures).stream()
                .map(measure -> measure.toBuilder()
                        .parentId(parentId)
                        .build())
                .collect(Collectors.toList());
    }
}
