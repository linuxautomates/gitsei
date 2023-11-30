import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { SAMPLE_INTERVAL } from "./constant";

export const sampleIntervalFiltersConfig: LevelOpsFilter = {
  id: "interval",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Sample Interval",
  beKey: "interval",
  required: true,
  labelCase: "title_case",
  filterMetaData: {
    options: SAMPLE_INTERVAL,
    selectMode: "default",
    sortOptions: false
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
