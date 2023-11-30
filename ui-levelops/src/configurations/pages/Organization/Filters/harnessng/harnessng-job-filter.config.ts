import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { supportedFilterType } from "dashboard/constants/supported-filters.constant";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import OrganisationFilterSelect from "../organisationFilterSelect";
import { OUCicdJobsCommonFiltersConfig } from "../jenkins/jenkins-job-filter.config";

export const ROLLBACK_KEY = "rollback";

export const harnessJobSupportedFilters: supportedFilterType = {
    uri: "jenkins_jobs_filter_values",
    values: [ROLLBACK_KEY, "cicd_user_id", "job_status", "service", "environment", "infrastructure", "deployment_type", "repository", "branch", "tag", 'project_name', 'job_name', 'job_normalized_full_name']
};

export const HARNESS_CICD_FILTER_LABEL_MAPPING: basicMappingType<string> = {
    job_name: "Pipeline",
    rollback: "Rollback",
    cicd_user_id: "Triggered By",
    job_status: "Status",
    service: "Service Id",
    environment: "Environment Id",
    infrastructure: "Infrastructure Id",
    deployment_type: "Deployment Type",
    repository: "Repository URL",
    branch: "Branch",
    job_normalized_full_name: "Qualified Name",
    tag: "Tags",
    project_name:"Project",
};


export const HARNESS_COMMON_FILTER_KEY_MAPPING: Record<string, string> = {
    rollback: ROLLBACK_KEY,
    cicd_user_id: "cicd_user_ids",
    job_status: "job_statuses",
    service: "services",
    environment: "environments",
    infrastructure: "infrastructures",
    deployment_type: "deployment_types",
    repository: "repositories",
    branch: "branches",
    tag: "tags",
    project_name:"projects",
    job_name: "job_names",
    job_normalized_full_name: "job_normalized_full_names"
};

const harnessngJobFilters = harnessJobSupportedFilters.values.map((item: string) => ({
    label: item.replace(/_/g, " ")?.toUpperCase(),
    key: item
}));

export const OUHarnessngJobsCommonFiltersConfig: LevelOpsFilter[] = [
    ...harnessngJobFilters
        .map((item: { key: string; label: string }) =>
            baseFilterConfig(HARNESS_COMMON_FILTER_KEY_MAPPING[item.key], {
                renderComponent: OrganisationFilterSelect,
                apiContainer: APIFilterContainer,
                label: HARNESS_CICD_FILTER_LABEL_MAPPING[item.key] ?? item.label,
                tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
                type: LevelOpsFilterTypes.API_DROPDOWN,
                labelCase: "title_case",
                deleteSupport: true,
                partialSupport: false,
                partialKey: item.key,
                excludeSupport: true,
                filterMetaData: {
                    selectMode: item.key === ROLLBACK_KEY ? "default" : "multiple",
                    uri: "jenkins_jobs_filter_values",
                    method: "list",
                    payload: (args: Record<string, any>) => {
                        const additionalFilter = get(args, "additionalFilter", {});
                        return {
                            integration_ids: get(args, "integrationIds", []),
                            fields: [item.key],
                            filter: {
                                integration_ids: get(args, "integrationIds", []),
                                ...additionalFilter,
                            }
                        };
                    },
                    specialKey: item.key,
                    options: (args: any) => {
                        const data = extractFilterAPIData(args, item.key);
                        return data
                            ?.map((item: any) => ({
                                label: item.additional_key ?? item.key,
                                value: item.key
                            }))
                            .filter((item: { label: string; value: string }) => !!item.value);
                    },
                    sortOptions: true,
                    createOption: true,
                    checkValueWithApiResponse: item.key === "job_status" ? true : false,
                } as ApiDropDownData
            })
        ),
    ...OUCicdJobsCommonFiltersConfig
];

export const HARNESS_CICD_ID_TOLABEL_MAPPINGS = Object.entries(HARNESS_COMMON_FILTER_KEY_MAPPING).reduce((acc: any, currentData: string[]) => ({
    ...acc,
    [currentData[1]]: HARNESS_CICD_FILTER_LABEL_MAPPING[currentData[0]]
}), {});
