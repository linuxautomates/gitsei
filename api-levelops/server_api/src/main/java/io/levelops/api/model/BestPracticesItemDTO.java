package io.levelops.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.BestPracticesItem;
import io.levelops.commons.databases.models.database.NotificationItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuperBuilder(toBuilder = true)
public class BestPracticesItemDTO extends NotificationItem {
    @JsonAlias({"id", "best_practices_id"})
    private String id;
    private String name;
    private BestPracticesItem.BestPracticeType type;
    private String value;
    @JsonProperty("created_at")
    private Long createdAt;
    private List<String> tags;
}