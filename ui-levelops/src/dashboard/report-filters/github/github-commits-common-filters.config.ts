import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { githubCommitsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { SCM_COMMIT_COMMON_FILTER_LABEL_MAPPING, SCM_COMMON_FILTER_KEY_MAPPING } from "dashboard/reports/scm/constant";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import genericApiFilterProps from "../common/common-api-filter-props";

const githubCommitsCommonFilters = githubCommitsSupportedFilters.values.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

const generateGitHubFilter = (item: { key: string; label: string }) =>
baseFilterConfig(SCM_COMMON_FILTER_KEY_MAPPING[item.key], {
  renderComponent: UniversalSelectFilterWrapper,
  apiContainer: APIFilterContainer,
  label: SCM_COMMIT_COMMON_FILTER_LABEL_MAPPING[item.key] ?? item.label,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  type: LevelOpsFilterTypes.API_DROPDOWN,
  labelCase: item.key !== "commit_branch" ? "title_case" : undefined,
  deleteSupport: true,
  partialSupport: true,
  excludeSupport: true,
  partialKey: item.key,
  supportPaginatedSelect: true,
  apiFilterProps: genericApiFilterProps,
  filterMetaData: {
    selectMode: "multiple",
    uri: "github_commits_filter_values",
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
});

export const githubCommitsCommonFiltersConfig: LevelOpsFilter[] = githubCommitsCommonFilters.map(generateGitHubFilter);

export const commit_branchFilterConfig: LevelOpsFilter = generateGitHubFilter({key: 'commit_branch', label: "Commit Branch"});
