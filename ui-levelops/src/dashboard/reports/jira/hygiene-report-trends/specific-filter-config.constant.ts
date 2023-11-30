import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const SampleIntervalFilterConfig: LevelOpsFilter = {
  id: "interval",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Sample Interval",
  beKey: "interval",
  labelCase: "title_case",
  filterMetaData: {
    options: [
      { value: "day", label: "Daily" },
      { value: "week", label: "Weekly" },
      { value: "month", label: "Monthly" },
      { value: "quarter", label: "Quarterly" }
    ],
    selectMode: "default",
    sortOptions: false
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const VisualizationFilterConfig: LevelOpsFilter = {
  id: "visualization",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Visualization",
  beKey: "visualization",
  labelCase: "title_case",
  filterMetaData: {
    selectMode: "default",
    options: [
      { label: "Stacked Area Chart", value: "stacked_area" },
      { label: "Stacked Bar Chart", value: "stacked_bar" }
    ],
    sortOptions: false
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
