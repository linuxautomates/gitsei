import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { AZURE_FILTER_KEY_MAPPING } from "dashboard/reports/azure/constant";
import { SCM_COMMON_FILTER_KEY_MAPPING } from "dashboard/reports/scm/constant";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";
import { Dict } from "types/dict";
import { toTitleCase } from "utils/stringUtils";
import { OUGithubCommitsCommonFiltersConfig } from "../github/github-commits-filter-config";
import { OUGithubPrsCommonFiltersConfig } from "../github/github-prs-filter-config";
import { OUJenkinsJobsCommonFiltersConfig } from "../jenkins/jenkins-job-filter.config";
import OrganisationFilterSelect from "../organisationFilterSelect";

const commonAzureFilters = issueManagementSupportedFilters?.values.map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

const issueManagementCommonFilterOptionsMapping: Dict<string, string> = {
  workitem_version: "Affects Version",
  workitem_parent_workitem_id: "Parent Workitem id",
  workitem_sprint_full_names: "Azure Iteration"
};

const azureAttributionFilters = [
  { key: "teams", label: "Teams" },
  { key: "code_area", label: "Code Area" }
];
const azureIterationFilters = [{ key: "workitem_sprint_full_names", label: "Azure Iteration" }];

export const OUAzureProjectFiltersConfig: LevelOpsFilter[] = [
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
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["project_name"],
          filter: {
            integration_ids: get(args, "integrationIds", []),
            cicd_integration_ids: get(args, "integrationIds", [])
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
  })
];

export const OUAzureIMCommonFiltersConfig: LevelOpsFilter[] = [
  baseFilterConfig((AZURE_FILTER_KEY_MAPPING as any)["workitem_priority"], {
    renderComponent: OrganisationFilterSelect,
    apiContainer: APIFilterContainer,
    label: "Workitem Priority",
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "workitem_priority",
    filterMetaData: {
      selectMode: "multiple",
      uri: "issue_management_workitem_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["priority"],
          filter: { integration_ids: get(args, "integrationIds", []) }
        };
      },
      specialKey: "workitem_priority",
      options: (args: any) => {
        const data = extractFilterAPIData(args, "priority");
        const newData = get(data, ["records"], []);
        return (newData as Array<any>)?.map((item: any) => ({
          label: get(staticPriorties, [item.key], item.key),
          value: item.key
        }));
      },
      sortOptions: true,
      createOption: true
    } as ApiDropDownData
  }),
  ...commonAzureFilters
    .filter((item: { key: string; label: string }) => item.key !== "workitem_priority")
    .map((item: { key: string; label: string }) =>
      baseFilterConfig((AZURE_FILTER_KEY_MAPPING as any)[item.key], {
        renderComponent: OrganisationFilterSelect,
        apiContainer: APIFilterContainer,
        label: (issueManagementCommonFilterOptionsMapping as any)[item.key] ?? item.label,
        tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
        type: LevelOpsFilterTypes.DROPDOWN,
        labelCase: "title_case",
        deleteSupport: true,
        partialSupport: true,
        excludeSupport: true,
        partialKey: item.key,
        filterMetaData: {
          selectMode: "multiple",
          uri: "issue_management_workitem_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            const newKey =
              item.key !== "workitem_type" && item.key.slice(0, 9) === "workitem_" ? item.key.slice(9) : item.key;
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [newKey],
              filter: { integration_ids: get(args, "integrationIds", []) }
            };
          },
          options: (args: any) => {
            const newKey =
              item.key !== "workitem_type" && item.key.slice(0, 9) === "workitem_" ? item.key.slice(9) : item.key;
            const data = extractFilterAPIData(args, newKey);
            const newData = get(data, ["records"], []);
            if (newData) {
              return (newData as Array<any>)?.map((item: any) => ({
                label: item.additional_key ?? item.key,
                value: item.key
              }));
            }
            return [];
          },
          specialKey: item.key,
          sortOptions: true,
          createOption: true
        } as ApiDropDownData
      })
    ),
  ...azureAttributionFilters.map((item: any) =>
    baseFilterConfig(item.key, {
      renderComponent: OrganisationFilterSelect,
      apiContainer: APIFilterContainer,
      label: (issueManagementCommonFilterOptionsMapping as any)[item.key] ?? item.label,
      type: LevelOpsFilterTypes.DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: true,
      excludeSupport: true,
      partialKey: item.key,
      filterMetaData: {
        selectMode: "multiple",
        uri: "issue_management_attributes_values",
        method: "list",
        payload: (args: Record<string, any>) => {
          const newKey =
            item.key !== "workitem_type" && item.key.slice(0, 9) === "workitem_" ? item.key.slice(9) : item.key;
          return {
            integration_ids: get(args, "integrationIds", []),
            fields: [newKey],
            filter: { integration_ids: get(args, "integrationIds", []) }
          };
        },
        options: (args: any) => {
          const newKey =
            item.key !== "workitem_type" && item.key.slice(0, 9) === "workitem_" ? item.key.slice(9) : item.key;
          const data = extractFilterAPIData(args, newKey);
          const newData = get(data, ["records"], []);
          if (newData) {
            return (newData as Array<any>)?.map((item: any) => ({
              label: item.additional_key ?? item.key,
              value: item.key
            }));
          }
          return [];
        },
        specialKey: item.key,
        sortOptions: true,
        createOption: true
      } as ApiDropDownData
    })
  ),
  ...azureIterationFilters.map((item: any) =>
    baseFilterConfig(item.key, {
      renderComponent: OrganisationFilterSelect,
      apiContainer: APIFilterContainer,
      label: (issueManagementCommonFilterOptionsMapping as any)[item.key] ?? item.label,
      type: LevelOpsFilterTypes.DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: true,
      excludeSupport: true,
      partialKey: item.key,
      filterMetaData: {
        selectMode: "multiple",
        uri: "issue_management_sprint_filters",
        method: "list",
        payload: (args: Record<string, any>) => {
          return {
            filter: { integration_ids: get(args, "integrationIds", []) },
            page: 0,
            page_size: 1000
          };
        },
        options: (args: any) => {
          const filterMetaData = get(args, ["filterMetaData"], {});
          const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
          const newData = filterApiData?.map((item: any) => ({ key: `${item?.parent_sprint}\\${item?.name}` }));
          if (newData) {
            return (newData as Array<any>)?.map((item: any) => ({
              label: item.additional_key ?? item.key,
              value: item.key
            }));
          }
          return [];
        },
        specialKey: item.key,
        sortOptions: true,
        createOption: true
      } as ApiDropDownData
    })
  ),
  ...OUAzureProjectFiltersConfig
];

export const OUAzureCICDCommonFiltersWithoutProjectConfig: LevelOpsFilter[] = [
  ...OUJenkinsJobsCommonFiltersConfig.filter((filter: LevelOpsFilter) => filter.id !== "projects")
];

export const OUAzureCICDCommonFiltersConfig: LevelOpsFilter[] = [
  ...OUAzureProjectFiltersConfig,
  ...OUJenkinsJobsCommonFiltersConfig.filter(
    (filter: LevelOpsFilter) => !["projects", "instance_names"].includes(filter.id)
  )
];

export const OUAzureSCMCommonFiltersConfig: LevelOpsFilter[] = [
  ...OUGithubPrsCommonFiltersConfig.filter(
    (filter: LevelOpsFilter) => filter.id !== SCM_COMMON_FILTER_KEY_MAPPING["project"] ?? "project"
  ),
  ...OUGithubCommitsCommonFiltersConfig
];

export const OUAzureCommonFiltersConfig: LevelOpsFilter[] = [
  ...OUAzureIMCommonFiltersConfig,
  ...OUAzureSCMCommonFiltersConfig,
  ...OUAzureCICDCommonFiltersWithoutProjectConfig
];
