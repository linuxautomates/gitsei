import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { scmPartialFilterKeyMapping } from "dashboard/constants/filter-key.mapping";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import OrganisationFilterSelect from "../organisationFilterSelect";

const scmPrsSupportedFilters = [
  "repo_id",
  "creator",
  "state",
  "source_branch",
  "assignee",
  "reviewer",
  "label",
  "approver",
  "project"
];

const githubCommonPrsFilterOptionsMapping: Record<string, string> = {
  label: "SCM Label",
  target_branch: "Destination Branch",
  repo_id: "Repo",
  source_branch: "Branch"
};

const commonPrsGithubFilters = scmPrsSupportedFilters.map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

const SCM_COMMON_FILTER_KEY_MAPPING: Record<string, string> = {
  repo_id: "repo_ids",
  creator: "creators",
  state: "states",
  source_branch: "branches",
  target_branch: "target_branches",
  assignee: "assignees",
  reviewer: "reviewers",
  label: "labels",
  project: "projects",
  committer: "committers",
  author: "authors",
  branch: "branches",
  approver: "approvers"
};

export const OUGithubPrsCommonFiltersConfig: LevelOpsFilter[] = [
  ...commonPrsGithubFilters.map((item: { key: string; label: string }) =>
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
      partialKey: get(scmPartialFilterKeyMapping, [item.key], item.key),
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
