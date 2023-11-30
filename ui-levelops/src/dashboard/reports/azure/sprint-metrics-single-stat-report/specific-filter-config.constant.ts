import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalInputTimeRangeWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalInputTimeRangeWrapper";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const IdealRangeFilterConfig: LevelOpsFilter = {
  id: "ideal_range",
  renderComponent: UniversalInputTimeRangeWrapper,
  label: "Ideal Range",
  beKey: "ideal_range",
  labelCase: "none",
  filterMetaData: {
    greaterThanKey: "min",
    lessThanKey: "max"
  },
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
