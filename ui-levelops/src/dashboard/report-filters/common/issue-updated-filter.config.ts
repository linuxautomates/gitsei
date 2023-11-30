import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const IssueUpdatedAtFilterConfig: LevelOpsFilter = {
  id: "issue_updated_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Issue Updated In",
  beKey: "issue_updated_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const generateIssueUpdatedAtFilterConfig: (beKey: string) => LevelOpsFilter = (beKey: string) => ({
  ...IssueUpdatedAtFilterConfig,
  beKey: beKey ?? "issue_updated_at"
});
