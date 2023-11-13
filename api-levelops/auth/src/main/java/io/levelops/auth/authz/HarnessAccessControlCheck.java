package io.levelops.auth.authz;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.METHOD) // Specifies where the annotation can be applied (e.g., method, class, field)
@Retention(RetentionPolicy.RUNTIME)
public @interface HarnessAccessControlCheck {
    ResourceType resourceType();
    Permission permission();
}
