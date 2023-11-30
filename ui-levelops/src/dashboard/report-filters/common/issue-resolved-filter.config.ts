import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const IssueResolvedAtFilterConfig: LevelOpsFilter = {
  id: "issue_resolved_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Issue Resolved In",
  beKey: "issue_resolved_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
