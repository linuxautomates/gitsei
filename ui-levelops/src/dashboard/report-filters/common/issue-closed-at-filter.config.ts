import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const IssueClosedAtFilterConfig: LevelOpsFilter = {
  id: "issue_closed_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Issue Closed At",
  beKey: "issue_closed_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const generateIssueClosedAtFiltersConfig = (label: string) => {
  return {
    ...IssueClosedAtFilterConfig,
    label: label
  };
};
