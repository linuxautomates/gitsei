import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import genericApiFilterProps, {
  withDeleteAPIProps,
  WithSwitchFilterProps
} from "dashboard/report-filters/common/common-api-filter-props";
import {
  githubCommonscmJiraFilterOptionsMapping,
  githubFiltersKeyMapping
} from "dashboard/reports/scm/scm-jira-files-report/constant";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";

const commonscmJiraFilters = ["repo_id", "project"].map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

export const ScmJiraCommonFiltersConfig: LevelOpsFilter[] = [
  baseFilterConfig((githubFiltersKeyMapping as any)["issue_type"], {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: "Issue Type",
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "issue_type",
    supportPaginatedSelect: true,
    apiFilterProps: args => ({ withDelete: withDeleteAPIProps(args), withSwitch: WithSwitchFilterProps(args) }),
    filterMetaData: {
      selectMode: "multiple",
      uri: "jira_filter_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["issue_types"],
          filter: { integration_ids: get(args, "integrationIds", []) }
        };
      },
      specialKey: "issue_types",
      options: (args: any) => {
        const filterMetaData = get(args, ["filterMetaData"], {});
        const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
        const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === "issue_types");
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
    } as ApiDropDownData
  }),
  ...commonscmJiraFilters
    .filter((item: any) => item !== "issue_types")
    .map((item: { key: string; label: string }) =>
      baseFilterConfig((githubFiltersKeyMapping as any)[item.key], {
        renderComponent: UniversalSelectFilterWrapper,
        apiContainer: APIFilterContainer,
        label: githubCommonscmJiraFilterOptionsMapping[item.key] ?? item.label,
        tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
        type: LevelOpsFilterTypes.API_DROPDOWN,
        labelCase: "title_case",
        deleteSupport: true,
        partialSupport: true,
        excludeSupport: true,
        partialKey: item.key,
        supportPaginatedSelect: true,
        apiFilterProps: args => ({ withDelete: withDeleteAPIProps(args), withSwitch: WithSwitchFilterProps(args) }),
        filterMetaData: {
          selectMode: "multiple",
          uri: "scm_files_filter_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [item.key],
              filter: { integration_ids: get(args, "integrationIds", []) }
            };
          },
          specialKey: item.key,
          options: (args: any) => {
            const filterMetaData = get(args, ["filterMetaData"], {});
            const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
            const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === item.key);
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
        } as ApiDropDownData
      })
    )
];
