import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { azureIntervalReport } from "dashboard/graph-filters/components/Constants";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { LevelOpsFilter, DropDownData } from "model/filters/levelopsFilters";

export const SampleIntervalFilterConfig: LevelOpsFilter = {
  id: "interval",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Sample Interval",
  beKey: "interval",
  required: true,
  labelCase: "title_case",
  defaultValue: "week",
  filterMetaData: {
    options: azureIntervalReport,
    selectMode: "default",
    sortOptions: false
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
