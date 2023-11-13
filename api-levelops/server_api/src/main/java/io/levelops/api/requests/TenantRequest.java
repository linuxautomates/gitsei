package io.levelops.api.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TenantRequest.TenantRequestBuilderImpl.class)
public class TenantRequest {
    @JsonProperty("tenant_name")
    @NonNull
    String tenantName;
    @JsonProperty("user_name")
    @NonNull
    String userName;
    @JsonProperty("user_lastname")
    @NonNull
    String userLastname;
    @JsonProperty("user_email")
    @NonNull
    String userEmail;

    @JsonProperty("tenant_type")
    @NonNull
    TenantType tenantType;

    @JsonProperty("create_initech_workspace")
    Boolean createInitechWorkspace;

    @JsonCreator
    public TenantRequest(
            final @JsonProperty("tenant_name") String tenantName,
            final @JsonProperty("user_name") String userName,
            final @JsonProperty("user_lastname") String userLastname,
            final @JsonProperty("user_email") String userEmail,
            final @JsonProperty("tenant_type") TenantType tenantType,
            final @JsonProperty("create_initech_workspace") Boolean createInitechWorkspace){
        this.tenantName = tenantName;
        this.userName = userName;
        this.userLastname = userLastname;
        this.userEmail = userEmail;
        this.tenantType = tenantType;
        this.createInitechWorkspace = createInitechWorkspace;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class TenantRequestBuilderImpl extends TenantRequestBuilder {

    }
}
