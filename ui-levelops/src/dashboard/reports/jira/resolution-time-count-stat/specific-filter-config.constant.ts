import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalStatTimeRangeFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalStatTimeRangeFilterWrapper";
import { LevelOpsFilter, StatTimeRangeFilterData } from "model/filters/levelopsFilters";

export const TimeRangeTypeFilterConfig: LevelOpsFilter = {
  id: "stat_time_based",
  renderComponent: UniversalStatTimeRangeFilterWrapper,
  label: "Time Range Type",
  beKey: "stat_time_based",
  labelCase: "none",
  required: true,
  defaultValue: "issue_created",
  filterMetaData: {
    options: [
      { value: "issue_created", label: "Issue Created" },
      { value: "issue_resolved", label: "Issue Resolved" },
      { value: "issue_updated", label: "Issue Updated" }
    ],
    filterLabel: (data: any) => {
      const { allFilters } = data;
      return allFilters.across ? `${allFilters.across.replace(/_/g, " ")} in` : "";
    },
    filterKey: (data: any) => {
      const { allFilters } = data;
      return allFilters.across ? `${allFilters.across}_at` : "";
    }
  } as StatTimeRangeFilterData,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
