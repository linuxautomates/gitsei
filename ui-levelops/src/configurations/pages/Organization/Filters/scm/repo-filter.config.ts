import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { scmPartialFilterKeyMapping } from "dashboard/constants/filter-key.mapping";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import OrganisationFilterSelect from "../organisationFilterSelect";

const filterMetaData = {
  selectMode: "multiple",
  uri: "scm_repos",
  method: "bulk",
  payload: (args: Record<string, any>) => {
    return {
      integration_ids: get(args, "integrationIds", []),
      fields: ["repo_ids"],
      filter: { integration_ids: get(args, "integrationIds", []) }
    };
  },
  specialKey: "repo_ids",
  options: (args: any) => {
    const filterMetaData = get(args, ["filterMetaData"], {});
    const data = get(filterMetaData, ["apiConfig", "data", "records"], []);
    return data?.map((item: any) => ({
      label: item,
      value: item
    }));
  },
  sortOptions: true,
  createOption: true
} as ApiDropDownData;

export const repoFilter = baseFilterConfig("repo_ids", {
  renderComponent: OrganisationFilterSelect,
  apiContainer: APIFilterContainer,
  label: "Repo",
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  type: LevelOpsFilterTypes.API_DROPDOWN,
  labelCase: "title_case",
  deleteSupport: true,
  partialSupport: true,
  excludeSupport: true,
  partialKey: get(scmPartialFilterKeyMapping, ["repo_ids"], "repo_ids"),
  filterMetaData
});
