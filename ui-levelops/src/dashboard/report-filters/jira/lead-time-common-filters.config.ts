import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import genericApiFilterProps from "../common/common-api-filter-props";
import { extractFilterAPIData } from "../helper";
import { jiraSupportedFilters } from "./../../constants/supported-filters.constant";

const LEAD_TIME_FILTERS_KEY_MAPPING: Record<string, string> = {
  status: "jira_statuses",
  priority: "jira_priorities",
  issue_type: "jira_issue_types",
  assignee: "jira_assignees",
  project: "jira_projects",
  component: "jira_components",
  label: "jira_labels",
  reporter: "jira_reporters",
  fix_version: "jira_fix_versions",
  version: "jira_versions",
  resolution: "jira_resolutions",
  status_category: "jira_status_categories"
};

const leadTimeCommonFilters = jiraSupportedFilters.values
  .filter((filter: string) => filter !== "project")
  .map((filter: string) => ({
    key: filter,
    label: toTitleCase(filter.replace(/_/g, " "))
  }));

export const leadTimeCommonFiltersConfig: LevelOpsFilter[] = [
  ...leadTimeCommonFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((LEAD_TIME_FILTERS_KEY_MAPPING as any)[item.key], {
      renderComponent: UniversalSelectFilterWrapper,
      label: item.label,
      apiContainer: APIFilterContainer,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: true,
      excludeSupport: true,
      partialKey: `jira_${item.key}`,
      supportPaginatedSelect: true,
      apiFilterProps: genericApiFilterProps,
      filterMetaData: {
        selectMode: "multiple",
        uri: "jira_filter_values",
        method: "list",
        payload: (args: Record<string, any>) => {
          return {
            integration_ids: get(args, "integrationIds", []),
            fields: [item.key],
            filter: { integration_ids: get(args, "integrationIds", []) }
          };
        },
        options: (args: any) => {
          const data = extractFilterAPIData(args, item.key);
          return data
            ?.map((item: any) => ({
              label: item.additional_key ?? item.key,
              value: item.key
            }))
            .filter((item: { label: string; value: string }) => !!item.value);
        },
        specialKey: item.key,
        sortOptions: true,
        createOption: true
      } as ApiDropDownData
    })
  ),
  baseFilterConfig(LEAD_TIME_FILTERS_KEY_MAPPING["project"], {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: "Projects",
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "none",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "jira_project",
    supportPaginatedSelect: true,
    apiFilterProps: genericApiFilterProps,
    filterMetaData: {
      selectMode: "multiple",
      uri: "jiraprojects_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["project_name"],
          filter: { integration_ids: get(args, "integrationIds", []) }
        };
      },
      options: (args: any) => {
        const data = extractFilterAPIData(args, "project_name");
        return data?.map((item: any) => ({
          label: `${toTitleCase(item.additional_key)} (${item.key})`,
          value: item.key
        }));
      },
      specialKey: "project",
      sortOptions: true,
      createOption: true
    } as ApiDropDownData
  })
];
