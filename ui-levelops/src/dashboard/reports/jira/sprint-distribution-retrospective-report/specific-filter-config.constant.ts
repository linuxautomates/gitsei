import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const PercentileFilterConfig: LevelOpsFilter = {
  id: "percentiles",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Percentile",
  beKey: "percentiles",
  labelCase: "title_case",
  filterMetaData: {
    options: [
      { label: "25", value: 25 },
      { label: "50", value: 50 },
      { label: "75", value: 75 },
      { label: "100", value: 100 }
    ],
    selectMode: "multiple"
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
