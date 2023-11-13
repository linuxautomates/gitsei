package io.levelops.commons.databases.models.database.coverity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.integrations.coverity.models.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbCoverityStream {

    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    private Integer integrationId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("language")
    private String language;

    @JsonProperty("project")
    private String project;

    @JsonProperty("triage_store_id")
    private String triageStoreId;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSZ")
    private Date createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSZ")
    private Date updatedAt;

    public static DbCoverityStream fromStream(Stream stream, String integrationId) {
        return DbCoverityStream.builder()
                .integrationId(Integer.valueOf(integrationId))
                .name(stream.getId().get("name"))
                .language(stream.getLanguage())
                .project(stream.getPrimaryProjectId().get("name"))
                .triageStoreId(stream.getTriageStoreId().get("name"))
                .build();
    }
}
