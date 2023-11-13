package io.levelops.commons.databases.models.database.checkmarx;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.integrations.checkmarx.models.CxQuery;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.ListUtils;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbCxSastQuery.DbCxSastQueryBuilder.class)
public class DbCxSastQuery {
    private static final String EMPTY = "";
    @JsonProperty("id")
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("scan_id")
    String scanId;

    @JsonProperty("query_id")
    String queryId;

    @JsonProperty("cwe_id")
    String cweId;

    @JsonProperty("name")
    String name;

    @JsonProperty("group")
    String group;

    @JsonProperty("severity")
    String severity;

    @JsonProperty("language")
    String language;

    @JsonProperty("language_hash")
    String languageHash;

    @JsonProperty("language_change_date")
    Long languageChangeDate;

    @JsonProperty("severity_index")
    String severityIndex;

    @JsonProperty("issues")
    List<DbCxSastIssue> issues;

    @JsonProperty("categories")
    List<String> categories;

    public static DbCxSastQuery fromQuery(CxQuery source,
                                          String integrationId, Date ingestedAt) {
        List<DbCxSastIssue> dbCxSastIssues = DbCxSastIssue.fromQuery(source, integrationId, ingestedAt);

        return DbCxSastQuery.builder()
                .integrationId(integrationId)
                .queryId(MoreObjects.firstNonNull(source.getId(), "0"))
                .cweId(source.getCweId())
                .name(MoreObjects.firstNonNull(source.getName(), EMPTY))
                .group(MoreObjects.firstNonNull(source.getGroup(), EMPTY))
                .categories(ListUtils.emptyIfNull(source.getCategory()))
                .severity(MoreObjects.firstNonNull(source.getSeverity(), EMPTY))
                .language(MoreObjects.firstNonNull(source.getLanguage(), EMPTY))
                .languageHash(MoreObjects.firstNonNull(source.getLanguageHash(), EMPTY))
                .languageChangeDate(Optional.ofNullable(source.getLanguageChangeDate())
                        .map(Date::toInstant)
                        .map(Instant::toEpochMilli)
                        .orElse(new Date().toInstant().toEpochMilli()))
                .severityIndex(MoreObjects.firstNonNull(source.getSeverityIndex(), EMPTY))
                .issues(dbCxSastIssues)
                .build();
    }
}
