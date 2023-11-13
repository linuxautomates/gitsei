package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.organization.DBOrgProduct;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScmAggServiceFileTypeUtils {

    public static DBOrgProduct getProductWithNoIntegAndFilter() {
        return DBOrgProduct.builder()
                .name("Sample 1")
                .description("This is a sample product")
                .integrations(Set.of(DBOrgProduct.Integ.builder().integrationId(1).build()))
                .build();
    }

    public static DBOrgProduct getProductWithInteg() {
        return DBOrgProduct.builder()
                .name("Sample 2")
                .description("This is a sample product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .name("github test")
                                .type("github")
                                .integrationId(1)
                                .build()
                ))
                .build();
    }

    public static DBOrgProduct getProductWithIntegAndFilter() {
        return DBOrgProduct.builder()
                .name("Sample 3")
                .description("This is a sample product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("github test")
                                .type("github")
                                .filters(Map.of("committers", List.of("f88da5c2-fb7b-44dc-af4e-b1fcb0b8c09d")))
                                .build()
                ))
                .build();
    }

    public static DBOrgProduct getProductWithIntegAndOneFilter() {
        return DBOrgProduct.builder()
                .name("Sample 3")
                .description("This is a sample product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("github test")
                                .type("github")
                                .filters(Map.of( "repo_ids", List.of("levelops/ui-levelops")))
                                .build()
                ))
                .build();
    }

}
