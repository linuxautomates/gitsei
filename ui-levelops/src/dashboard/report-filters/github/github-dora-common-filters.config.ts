import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import genericApiFilterProps from "dashboard/report-filters/common/common-api-filter-props";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";

export const githubFiltersKeyMapping: Record<string, string> = {
  repo_id: "repo_ids",
  creator: "creators",
  branch: "branches",
  label: "labels",
  project: "projects",
  target_branch: "target_branches",
  source_branch: "source_branches"
};
export const scmDoraSupportedFilters = [
  "repo_id",
  "creator",
  "branch",
  "label",
  "project",
  "source_branch",
  "target_branch"
];
const scmCommonDoraFilterOptionsMapping: Record<string, string> = {
  label: "SCM Label"
};

export const commonDoraGithubFilters = scmDoraSupportedFilters.map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

export const scmDoraCommonFiltersConfig: LevelOpsFilter[] = [
  ...commonDoraGithubFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((githubFiltersKeyMapping as any)[item.key], {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      deleteSupport: true,
      label: scmCommonDoraFilterOptionsMapping[item.key] ?? item.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      supportPaginatedSelect: true,
      apiFilterProps: genericApiFilterProps,
      filterMetaData: {
        selectMode: "multiple",
        uri: "github_prs_filter_values",
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
