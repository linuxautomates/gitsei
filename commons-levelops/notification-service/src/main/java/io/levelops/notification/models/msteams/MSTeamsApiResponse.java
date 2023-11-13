package io.levelops.notification.models.msteams;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.annotation.Nullable;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
@Builder
public class MSTeamsApiResponse<T>{

    @JsonUnwrapped
    private T payload;

    @JsonProperty("value")
    private List<T> values;

    @Nullable
    MSTeamsErrorResponse error;

}