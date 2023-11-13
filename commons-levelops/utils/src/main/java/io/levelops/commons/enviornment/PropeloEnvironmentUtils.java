package io.levelops.commons.enviornment;

public class PropeloEnvironmentUtils {
    public static PropeloEnvironmentType getEnvironmentFromOauthBaseUrl(String oAuthBaseUrl) {
        switch (oAuthBaseUrl) {
            case "https://app.propelo.ai":
                return PropeloEnvironmentType.PROD;
            case "https://asia1.app.propelo.ai":
                return PropeloEnvironmentType.ASIA1;
            case "https://staging.app.propelo.ai":
                return PropeloEnvironmentType.STAGING;
            case "https://testui1.propelo.ai":
                return PropeloEnvironmentType.DEV;
            case "https://eu1.app.propelo.ai":
                return PropeloEnvironmentType.EU1;
            case "https://dev2.app.propelo.ai":
                    return PropeloEnvironmentType.DEV2;
            case "https://qa.harness.io":
                return PropeloEnvironmentType.HARNESS_QA;
            case "https://stress.harness.io":
                return PropeloEnvironmentType.HARNESS_PRE_QA;
            case "https://app.harness.io":
                return PropeloEnvironmentType.HARNESS_PROD;
            default:
                throw new IllegalArgumentException("Unable to detect environment from oAuth url: " + oAuthBaseUrl);
        }
    }


    public static PropeloEnvironmentType getEnvironmentFromAggProject(String aggProject) {
        switch (aggProject) {
            case "levelops-staging":
                return PropeloEnvironmentType.STAGING;
            case "levelops-api-and-data":
                return PropeloEnvironmentType.PROD;
            case "levelops-dev":
                return PropeloEnvironmentType.DEV;
            case "levelops-asia1":
                return PropeloEnvironmentType.ASIA1;
            case "propelo-dev2":
                return PropeloEnvironmentType.DEV2;
            case "propelo-eu1":
                return PropeloEnvironmentType.EU1;
            case "preqa-setup":
                return PropeloEnvironmentType.HARNESS_PRE_QA;
            case "qa-setup":
                return PropeloEnvironmentType.HARNESS_QA;
            case "prod-setup":
                return PropeloEnvironmentType.HARNESS_PROD;
            default:
                throw new IllegalArgumentException("Unable to detect environment from aggProject: " + aggProject);
        }
    }
}
