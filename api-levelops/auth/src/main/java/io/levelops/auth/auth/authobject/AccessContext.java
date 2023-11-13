package io.levelops.auth.auth.authobject;

import io.harness.authz.acl.client.ACLClient;
import io.harness.authz.acl.model.Principal;
import io.harness.authz.acl.model.ResourceScope;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class AccessContext {

    private Principal principal;
    private ResourceScope resourceScope;
    private ACLClient aclClient;
}
