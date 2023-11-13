package io.levelops.auth.httpmodels;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class EntitlementDetails {

    private String method;
    private String api;
}
