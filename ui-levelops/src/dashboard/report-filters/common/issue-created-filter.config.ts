import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const IssueCreatedAtFilterConfig: LevelOpsFilter = {
  id: "issue_created_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Issue Created In",
  beKey: "issue_created_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const generateIssueCreatedAtFilterConfig = (
  options: Array<{ label: string; value: string | number }> | ((args: any) => Array<{ label: string; value: string }>),
  label?: string,
  beKey?: string
): LevelOpsFilter => ({
  ...IssueCreatedAtFilterConfig,
  label: label ? label : IssueCreatedAtFilterConfig.label,
  beKey: beKey ? beKey : IssueCreatedAtFilterConfig.beKey,
  filterMetaData: { ...IssueCreatedAtFilterConfig.filterMetaData, options } as DropDownData
});
