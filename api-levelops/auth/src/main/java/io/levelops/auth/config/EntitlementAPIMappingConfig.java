package io.levelops.auth.config;

import io.levelops.auth.httpmodels.EntitlementDetails;
import io.levelops.auth.httpmodels.Entitlements;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.auth.httpmodels.Entitlements.*;

@Configuration
public class EntitlementAPIMappingConfig {

    private static final List<String> SUPPORTED_ENTITLEMENTS = List.of(
            "[/v1/integrations",
            "[/v1/dashboards",
            "[/v1/jira_issues",
            "[/v1/playbooks",
            "[/v1/triage_filters",
            "[/v1/triage_rule_results",
            "[/v1/triage_rules",
            "[/v1/config-tables",
            "[/v1/qtemplates",
            "[/v1/ticket_templates",
            "[/v1/message_templates"
    );
    private static final List<String> WRITE_METHODS = List.of("[PUT]", "[DELETE]", "[PATCH]");

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    public static Map<Entitlements, Set<EntitlementDetails>> entitlementsMap = Map.of();
    public static Map<Entitlements, Set<EntitlementDetails>> readOnlyEntitlementsMap = Map.of();

    @Autowired
    public EntitlementAPIMappingConfig(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.entitlementsMap = populateEntitlementMapping();
        this.readOnlyEntitlementsMap = populateReadOnlyMapping();
    }

    private Map<Entitlements, Set<EntitlementDetails>> populateReadOnlyMapping() {

        Set<RequestMappingInfo> req = requestMappingHandlerMapping.getHandlerMethods().keySet();
        return req.stream()
                .filter(r -> SUPPORTED_ENTITLEMENTS.stream().anyMatch(api -> r.getPatternsCondition().toString().startsWith(api)) && !WRITE_METHODS.contains(r.getMethodsCondition().toString()))
                .collect(Collectors.groupingBy(r -> getReadEntitlements(r), Collectors.mapping(fun -> {
                    return EntitlementDetails.builder()
                            .api(getSanitizedName(fun.getPatternsCondition().toString()))
                            .method(getSanitizedName(fun.getMethodsCondition().toString()))
                            .build();
                }, Collectors.toSet())));
    }

    private Map<Entitlements, Set<EntitlementDetails>> populateEntitlementMapping() {

        Set<RequestMappingInfo> req = requestMappingHandlerMapping.getHandlerMethods().keySet();

        Map<Entitlements, Set<EntitlementDetails>> writeEntitlements = req.stream()
                .filter(r -> SUPPORTED_ENTITLEMENTS.stream().anyMatch(api -> r.getPatternsCondition().toString().startsWith(api)))
                .collect(Collectors.groupingBy(r -> getWriteEntitlements(r), Collectors.mapping(fun -> {
                    return EntitlementDetails.builder()
                            .api(getControllerAPI(fun))
                            .build();
                }, Collectors.toSet())));

        writeEntitlements.put(PROPELS_COUNT_5, writeEntitlements.get(PROPELS));
        writeEntitlements.put(SETTING_SCM_INTEGRATIONS_COUNT_3, writeEntitlements.get(INTEGRATIONS));
        return writeEntitlements;
    }

    private Entitlements getReadEntitlements(RequestMappingInfo r) {
        String api = getControllerAPI(r);
        String sanitizedApi = getSanitizedName(r.getPatternsCondition().toString());
        if (sanitizedApi.equals(api))
            return ALL_FEATURES;

        switch (api) {
            case "/v1/dashboards":
                return DASHBOARDS_READ;
            case "/v1/jira_issues":
                return ISSUES_READ;
            case "/v1/playbooks":
                return PROPELS_READ;
            default:
                return ALL_FEATURES;
        }
    }

    private Entitlements getWriteEntitlements(RequestMappingInfo r) {

        String api = getControllerAPI(r);

        switch (api) {
            case "/v1/integrations":
                return INTEGRATIONS;
            case "/v1/dashboards":
                return DASHBOARDS;
            case "/v1/jira_issues":
                return ISSUES;
            case "/v1/playbooks":
                return PROPELS;
            case "/v1/triage_filters":
            case "/v1/triage_rule_results":
            case "/v1/triage_rules":
                return TRIAGE;
            case "/v1/config-tables":
                return TABLES;
            case "/v1/qtemplates":
            case "/v1/ticket_templates":
            case "/v1/message_templates":
                return TEMPLATES;
            default:
                return ALL_FEATURES;
        }
    }

    private String getSanitizedName(String name) {

        name = name.replaceAll("]", "");
        name = name.replaceAll("\\[", "");

        if (name.contains("{"))
            name = name.replaceAll("\\{.*?\\}", "\\\\w+");

        return name;
    }

    private String getControllerAPI(RequestMappingInfo r) {

        String api = getSanitizedName(r.getPatternsCondition().toString());
        if (StringUtils.countMatches(api, "/") > 2) {
            api = api.substring(0, StringUtils.ordinalIndexOf(api, "/", 3));
        }
        return api;
    }


}
