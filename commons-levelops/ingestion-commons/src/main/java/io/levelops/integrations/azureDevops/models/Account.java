package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Account.AccountBuilder.class)
public class Account {

    @JsonProperty("accountId")
    String accountId;

    @JsonProperty("accountUri")
    String accountUri;

    @JsonProperty("accountName")
    String accountName;

}
