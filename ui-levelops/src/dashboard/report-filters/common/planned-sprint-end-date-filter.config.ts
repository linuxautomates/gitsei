import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { dateRangeFilterValueString } from "dashboard/components/dashboard-view-page-secondary-header/helper";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const PlannedSprintEndDateFilterConfig: LevelOpsFilter = {
  id: "planned_ended_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Planned Sprint End Date",
  beKey: "planned_ended_at",
  labelCase: "title_case",
  deleteSupport: true,
  defaultValue: {
    type: "relative",
    relative: {
      last: {
        num: 30,
        unit: "days"
      },
      next: {
        unit: "today"
      }
    },
    absolute: dateRangeFilterValueString("LAST_30_DAYS")
  },
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
