package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class DBOrgAccessUsers {

    @JsonProperty("authorized_users")
    Set<UUID> authorizedUserList;

    @JsonProperty("unauthorized_users")
    Set<UUID> unAuthorizedUserList;

    public boolean isNotEmpty() {
        return CollectionUtils.isNotEmpty(this.getAuthorizedUserList()) || CollectionUtils.isNotEmpty(this.getUnAuthorizedUserList());
    }
}
