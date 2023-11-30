import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import UniversalStatTimeRangeFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalStatTimeRangeFilterWrapper";
import { DropDownData, LevelOpsFilter, StatTimeRangeFilterData } from "model/filters/levelopsFilters";

export const TimeRangeTypeFilterConfig: LevelOpsFilter = {
  id: "stat_time_based",
  renderComponent: UniversalStatTimeRangeFilterWrapper,
  label: "Time Range Type",
  beKey: "stat_time_based",
  required: true,
  labelCase: "none",
  defaultValue: "issue_created",
  filterMetaData: {
    options: [
      { value: "issue_created", label: "Issue Created" },
      { value: "issue_resolved", label: "Issue Resolved" },
      { value: "issue_due", label: "Issue Due" },
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

export const MetricFilterConfig: LevelOpsFilter = {
  id: "metrics",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Metric",
  beKey: "metrics",
  labelCase: "title_case",
  defaultValue: "total_tickets",
  updateInWidgetMetadata: true,
  filterMetaData: {
    selectMode: "default",
    options: [
      { value: "total_tickets", label: "Number of Issues" },
      { value: "total_story_points", label: "Sum Of Story Points" }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};
