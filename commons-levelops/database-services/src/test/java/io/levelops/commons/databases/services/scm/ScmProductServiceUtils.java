package io.levelops.commons.databases.services.scm;

import io.levelops.commons.databases.models.database.organization.DBOrgProduct;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScmProductServiceUtils {

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

    public static DBOrgProduct getProductWithIntegAndTwoFilters() {
        return DBOrgProduct.builder()
                .name("Sample 4")
                .description("This is a sample product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("github test")
                                .type("github")
                                .filters(Map.of( "committers", List.of("f88da5c2-fb7b-44dc-af4e-b1fcb0b8c09d"),
                                        "assignees", List.of("jasonodonnell", "viraj-levelops")))
                                .build()
                ))
                .build();
    }

    public static DBOrgProduct getProductWithIntegAndTwoFilters(List<String> assignees, List<String> committers) {
        return DBOrgProduct.builder()
                .name("Sample 4")
                .description("This is a sample product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("github test")
                                .type("github")
                                .filters(Map.of( "committers", committers,
                                        "assignees", assignees))
                                .build()
                ))
                .build();
    }

    public static List<DBOrgProduct> getTwoProductWithIntegAndFilter() {
        return List.of(DBOrgProduct.builder()
                        .name("Sample 5")
                        .description("This is a sample product 3")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .integrationId(1)
                                        .name("github test")
                                        .type("github")
                                        .filters(Map.of("assignees", List.of("jasonodonnell", "viraj-levelops"),
                                                "modules", List.of("src")))
                                        .build()
                        ))
                        .build(),
                DBOrgProduct.builder()
                        .name("Sample 6")
                        .description("This is a sample product to test")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .integrationId(2)
                                        .name("github test 2")
                                        .type("github")
                                        .filters(Map.of("committers", List.of("f88da5c2-fb7b-44dc-af4e-b1fcb0b8c09d"),
                                                "assignees", List.of("jasonodonnell", "viraj-levelops"), "modules", List.of("src")))
                                        .build()
                        ))
                        .build()

        );
    }

    public static List<DBOrgProduct> getTwoProductWithIntegAndFilter(List<String> assignees) {
        return List.of(DBOrgProduct.builder()
                        .name("Sample 5")
                        .description("This is a sample product 3")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .integrationId(1)
                                        .name("github test")
                                        .type("github")
                                        .filters(Map.of("assignees", assignees,
                                                "modules", List.of("src")))
                                        .build()
                        ))
                        .build(),
                DBOrgProduct.builder()
                        .name("Sample 6")
                        .description("This is a sample product to test")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .integrationId(2)
                                        .name("github test 2")
                                        .type("github")
                                        .filters(Map.of("committers", List.of("f88da5c2-fb7b-44dc-af4e-b1fcb0b8c09d"),
                                                "assignees", List.of("jasonodonnell", "viraj-levelops"), "modules", List.of("src")))
                                        .build()
                        ))
                        .build()

        );
    }

    public static List<DBOrgProduct> getTwoProductWithIntegAndFilters() {
        return List.of(DBOrgProduct.builder()
                        .name("Sample 7")
                        .description("This is a sample product")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .integrationId(1)
                                        .name("github test")
                                        .type("github")
                                        .filters(Map.of( "authors",
                                                List.of(" 232f1b96-04a3-457f-b32a-dd67ac819d65", "f88da5c2-fb7b-44dc-af4e-b1fcb0b8c09d"),
                                                "assignees", List.of("jasonodonnell", "viraj-levelops"), "modules", List.of("src")))
                                        .build()

                        ))
                        .build()

        );
    }

    public static List<DBOrgProduct> getTwoProductWithIntegAndFilters(List<String> assignees, List<String> authors) {
        return List.of(DBOrgProduct.builder()
                .name("Sample 7")
                .description("This is a sample product")
                .integrations(Set.of(
                        DBOrgProduct.Integ.builder()
                                .integrationId(1)
                                .name("github test")
                                .type("github")
                                .filters(Map.of( "authors", authors,
                                        "assignees", assignees, "modules", List.of("src")))
                                .build()

                ))
                .build()

        );
    }

    public static List<DBOrgProduct> getTwoProductWithTwoIntegAndNoFilters() {
        return List.of(DBOrgProduct.builder()
                        .name("Sample 9")
                        .description("This is a sample product")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .name("github test")
                                        .type("github")
                                        .integrationId(1)
                                        .build(),
                                DBOrgProduct.Integ.builder()
                                        .name("github test 2")
                                        .type("github")
                                        .integrationId(2)
                                        .build()
                        ))
                        .build(),
                DBOrgProduct.builder()
                        .name("Sample 10")
                        .description("This is a sample product to test")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .integrationId(1)
                                        .name("github test")
                                        .type("github")
                                        .build(),
                                DBOrgProduct.Integ.builder()
                                        .integrationId(2)
                                        .name("github test")
                                        .type("github")
                                        .build()
                        ))
                        .build()

        );
    }

    public static List<DBOrgProduct> getTwoProductWithTwoIntegAndOneFilter() {
        return List.of(DBOrgProduct.builder()
                        .name("Sample 11")
                        .description("This is a sample product")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .integrationId(1)
                                        .name("github test")
                                        .type("github")
                                        .filters(Map.of("projects", List.of("levelops/ui-levelops"),
                                                "assignees", List.of("jasonodonnell", "viraj-levelops"), "modules", List.of("src")))
                                        .build()
                        ))
                        .build(),
                DBOrgProduct.builder()
                        .name("Sample 13")
                        .description("This is a sample product to test")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .integrationId(2)
                                        .name("github test")
                                        .type("github")
                                        .filters(Map.of("projects", List.of("levelops/ui-levelops"), "modules", List.of("src")))
                                        .build()
                        ))
                        .build()

        );
    }

    public static List<DBOrgProduct> getTwoProductWithTwoIntegAndTwoFilter() {
        return List.of(DBOrgProduct.builder()
                        .name("Sample 14")
                        .description("This is a sample product")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .integrationId(1)
                                        .name("github test")
                                        .type("github")
                                        .filters(Map.of("creators", List.of("790dc94e-88ac-44c7-bf5c-a2953deb6796",
                                                "64cfe79a-5c9e-4b84-8712-cea93041e5dd"),
                                                "assignees", List.of("jasonodonnell", "viraj-levelops"), "modules", List.of("src")))
                                        .build()
                        ))
                        .build(),
                DBOrgProduct.builder()
                        .name("Sample 15")
                        .description("This is a sample product to test")
                        .integrations(Set.of(
                                DBOrgProduct.Integ.builder()
                                        .integrationId(1)
                                        .name("github test 2")
                                        .type("github")
                                        .filters(Map.of("assignees", List.of("790dc94e-88ac-44c7-bf5c-a2953deb6796", "viraj-levelops")))
                                        .build(),
                                DBOrgProduct.Integ.builder()
                                        .integrationId(2)
                                        .name("github test 3")
                                        .type("github")
                                        .filters(Map.of("projects", List.of("levelops/ui-levelops"), "integration_ids", List.of("2"), "modules", List.of("src")))
                                        .build()
                        ))
                        .build()

        );
    }
}
