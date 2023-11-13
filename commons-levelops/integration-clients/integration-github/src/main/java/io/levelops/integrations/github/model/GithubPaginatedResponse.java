package io.levelops.integrations.github.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubPaginatedResponse.GithubPaginatedResponseBuilder.class)
public class GithubPaginatedResponse<T> {

    String linkHeader;
    List<T> records;

}
