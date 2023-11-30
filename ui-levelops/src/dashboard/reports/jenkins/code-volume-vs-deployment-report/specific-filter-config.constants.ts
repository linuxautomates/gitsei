import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { LevelOpsFilter, DropDownData } from "model/filters/levelopsFilters";

export const IntervalFilterConfig: LevelOpsFilter = {
  id: "interval",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Interval",
  beKey: "interval",
  labelCase: "title_case",
  filterMetaData: {
    options: [
      { label: "Weekly", value: "week" },
      { label: "Bi-Weekly", value: "biweekly" },
      { label: "Monthly", value: "month" }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
