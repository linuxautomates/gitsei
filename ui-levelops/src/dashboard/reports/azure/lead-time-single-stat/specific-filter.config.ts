import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalOUFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalOUFilterWrapper";
import { LevelOpsFilter, DropDownData, OUFilterData, OUFilterByApplicationType } from "model/filters/levelopsFilters";
import { githubOptions } from "./constant";

export const GithubFiltersConfig: LevelOpsFilter = {
  id: "github",
  renderComponent: UniversalOUFilterWrapper,
  label: "Github",
  beKey: "ou_user_filter_designation",
  labelCase: "none",
  defaultValue: "committer",
  required: true,
  filterMetaData: {
    filtersByApplications: {
      github: {
        options: githubOptions
      }
    },
    selectMode: "multiple"
  },
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
