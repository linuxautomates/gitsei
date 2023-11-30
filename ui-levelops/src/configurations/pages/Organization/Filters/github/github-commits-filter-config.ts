import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { SCM_COMMON_FILTER_KEY_MAPPING } from "dashboard/reports/scm/constant";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import OrganisationFilterSelect from "../organisationFilterSelect";

const scmCommitsSupportedFilters = ["committer", "author"];

const githubCommitsCommonFilters = scmCommitsSupportedFilters.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

const githubCommonPrsFilterOptionsMapping: Record<string, string> = {
  committer: "Committer",
  author: "Author"
};

export const OUGithubCommitsCommonFiltersConfig: LevelOpsFilter[] = [
  ...githubCommitsCommonFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((SCM_COMMON_FILTER_KEY_MAPPING as any)[item.key] ?? item.key, {
      renderComponent: OrganisationFilterSelect,
      apiContainer: APIFilterContainer,
      label: githubCommonPrsFilterOptionsMapping[item.key] ?? item.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: true,
      excludeSupport: true,
      partialKey: item.key,
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
