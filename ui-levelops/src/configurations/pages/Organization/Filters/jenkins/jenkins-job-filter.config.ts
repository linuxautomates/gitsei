import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { supportedFilterType } from "dashboard/constants/supported-filters.constant";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import OrganisationFilterSelect from "../organisationFilterSelect";
import { getChildFilterLableName, getChildKeys } from "dashboard/report-filters/jenkins/jenkins-common-filter-props.config";

const jenkinsGithubJobSupportedFilters: supportedFilterType = {
  uri: "jenkins_jobs_filter_values",
  values: ["cicd_user_id", "job_status", "job_name", "project_name", "instance_name", "job_normalized_full_name"]
};

const cicdCommonJobSupportedFilters: supportedFilterType = {
  uri: "jenkins_jobs_filter_values",
  values: ["stage_name", "step_name"]
};

const scmCicdFilterLabelMapping: basicMappingType<string> = {
  repos: "SCM Repos",
  instance_name: "Instance Name",
  trend: "Trend",
  job_name: "Pipeline",
  cicd_user_id: "Triggered By",
  job_status: "Status",
  job_normalized_full_name: "Qualified Name",
  source_branch: "Source Branch",
  target_branch: "Destination Branch",
  project: "Project Name"
};

const cicdCommonFilterLabelMapping: basicMappingType<string> = {
  stage_name: "Stage Name",
  step_name: "Step Name",
  step_status: "Step Status",
  stage_status: "Stage Status"
};

const JENKINS_COMMON_FILTER_KEY_MAPPING: Record<string, string> = {
  cicd_user_id: "cicd_user_ids",
  job_status: "job_statuses",
  job_name: "job_names",
  project_name: "projects",
  instance_name: "instance_names",
  job_normalized_full_name: "job_normalized_full_names"
};

const CICD_COMMON_FILTER_KEY_MAPPING: Record<string, string> = {
  stage_name: "stage_name",
  step_name: "step_name",
};

const jenkinsGithubJobFilters = jenkinsGithubJobSupportedFilters.values.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

const cicdCommonJobFilters = cicdCommonJobSupportedFilters.values.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

export const jenkinsCommonChildConfig = (childKey: string) => {
  return baseFilterConfig(childKey, {
    renderComponent: OrganisationFilterSelect,
    apiContainer: APIFilterContainer,
    label: cicdCommonFilterLabelMapping[childKey],
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: false,
    excludeSupport: true,
    supportPaginatedSelect: true,
    filterMetaData: {
      selectMode: "multiple",
      uri: "jenkins_jobs_filter_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: [childKey],
          filter: {
            integration_ids: get(args, "integrationIds", []),
            ...args?.childComponentFilter
          }
        };
      },
      specialKey: childKey,
      options: (args: any) => {
        const filterMetaData = get(args, ["filterMetaData"], {});
        const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
        const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === childKey);
        if (currData) {
          return (Object.values(currData)[0] as Array<any>)
            ?.map((item: any) => ({
              label: item.additional_key ?? item.key,
              value: item.key
            }))
            .filter((item: { label: string; value: string }) => !!item.value);
        }
        return [];
      },
      sortOptions: true,
      createOption: true
    } as ApiDropDownData,
    childButtonLableName: getChildFilterLableName
  });

}

export const OUCicdJobsCommonFiltersConfig: LevelOpsFilter[] = [
  ...cicdCommonJobFilters
    .map((item: { key: string; label: string }) =>
      baseFilterConfig(CICD_COMMON_FILTER_KEY_MAPPING[item.key], {
        renderComponent: OrganisationFilterSelect,
        apiContainer: APIFilterContainer,
        label: cicdCommonFilterLabelMapping[item.key] ?? item.label,
        tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
        type: LevelOpsFilterTypes.API_DROPDOWN,
        labelCase: "title_case",
        deleteSupport: true,
        partialSupport: item.key === "job_normalized_full_names" ? true : false,
        partialKey: item.key,
        excludeSupport: true,
        childFilterKeys: getChildKeys,
        renderChildComponent: jenkinsCommonChildConfig,
        childButtonLableName: getChildFilterLableName,
        filterMetaData: {
          selectMode: "multiple",
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
          createOption: true
        } as ApiDropDownData
      })
    )
];

export const OUJenkinsJobsCommonFiltersConfig: LevelOpsFilter[] = [
  baseFilterConfig("projects", {
    renderComponent: OrganisationFilterSelect,
    apiContainer: APIFilterContainer,
    label: "Project",
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: false,
    excludeSupport: true,
    filterMetaData: {
      selectMode: "multiple",
      uri: "jenkins_jobs_filter_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        const additionalFilter = get(args, "additionalFilter", {});
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["project_name"],
          filter: {
            integration_ids: get(args, "integrationIds", []),
            ...additionalFilter,
          }
        };
      },
      specialKey: "project_name",
      options: (args: any) => {
        const data = extractFilterAPIData(args, "project_name");
        return data
          ?.map((item: any) => ({
            label: item.additional_key ?? item.key,
            value: item.key
          }))
          .filter((item: { label: string; value: string }) => !!item.value);
      },
      sortOptions: true,
      createOption: true
    } as ApiDropDownData
  }),
  ...jenkinsGithubJobFilters
    .filter(item => item.key != "project_name")
    .map((item: { key: string; label: string }) =>
      baseFilterConfig(JENKINS_COMMON_FILTER_KEY_MAPPING[item.key], {
        renderComponent: OrganisationFilterSelect,
        apiContainer: APIFilterContainer,
        label: scmCicdFilterLabelMapping[item.key] ?? item.label,
        tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
        type: LevelOpsFilterTypes.API_DROPDOWN,
        labelCase: "title_case",
        deleteSupport: true,
        partialSupport: item.key === "job_normalized_full_names" ? true : false,
        partialKey: item.key,
        excludeSupport: true,
        filterMetaData: {
          selectMode: "multiple",
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
          createOption: true
        } as ApiDropDownData
      })
    ),
  ...OUCicdJobsCommonFiltersConfig
];

export const OUDroneCircleJobsCommonFiltersConfig: LevelOpsFilter[] = [
  ...OUJenkinsJobsCommonFiltersConfig.filter(item => item.beKey !== "instance_names")
];

export const OUGithubActionJobsCommonFiltersConfig: LevelOpsFilter[] = [
  ...OUJenkinsJobsCommonFiltersConfig.filter(item => item.beKey !== "instance_names")
];
