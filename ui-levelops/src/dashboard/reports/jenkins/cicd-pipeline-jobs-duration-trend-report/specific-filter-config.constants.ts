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
      { value: "quarter", label: "Quarterly" },
      { value: "year", label: "Yearly" }
    ],
    selectMode: "default",
    sortOptions: false
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
