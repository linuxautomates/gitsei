import { WIDGET_CONFIGURATION_PARENT_KEYS } from "constants/widgets";
import CalculationTimeFilter from "dashboard/graph-filters/components/GenericFilterComponents/DoraGenericFilter/CalculationTimeFilter";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const DoraCalculationTimeConfig: LevelOpsFilter = {
  id: "time_calculation",
  renderComponent: CalculationTimeFilter,
  label: "time calculation",
  beKey: "time_range",
  labelCase: "title_case",
  isParentTab: true,
  tab: WIDGET_CONFIGURATION_PARENT_KEYS.SETTINGS
};
