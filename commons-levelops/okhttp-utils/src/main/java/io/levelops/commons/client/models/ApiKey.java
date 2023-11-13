package io.levelops.commons.client.models;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ApiKey {
    private String userName;
    private String apiKey;
}
