import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { HygieneFilterConfig } from "dashboard/report-filters/common/hygiene-filter.config";
import {
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import {
  JIRA_COMMON_FILTER_LABEL_MAPPING,
  JIRA_FILTER_KEY_MAPPING,
  JIRA_PARTIAL_FILTER_KEY_MAPPING
} from "dashboard/reports/jira/constant";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";

const commonJiraFilters = jiraSupportedFilters.values
  .filter((filter: string) => filter !== "project")
  .map((filter: string) => ({
    key: filter,
    label: toTitleCase(filter.replace(/_/g, " "))
  }));

export const EIJiraFiltersConfig: LevelOpsFilter[] = [
  baseFilterConfig(JIRA_FILTER_KEY_MAPPING["project"], {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: "Projects",
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "none",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "project",
    supportPaginatedSelect: true,
    apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
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
  }),
  ...commonJiraFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((JIRA_FILTER_KEY_MAPPING as any)[item.key], {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: JIRA_COMMON_FILTER_LABEL_MAPPING[item.label.toLowerCase()] ?? item.label,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: true,
      excludeSupport: true,
      partialKey: JIRA_PARTIAL_FILTER_KEY_MAPPING[item.key] ?? item.key,
      supportPaginatedSelect: true,
      apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
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
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig,
  EpicsFilterConfig,
  HygieneFilterConfig
];
