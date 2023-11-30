import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalInputTimeRangeWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalInputTimeRangeWrapper";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { METRICS_OPTIONS } from "./constants";

export const MetricFilterConfig: LevelOpsFilter = {
  id: "metric",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Metric",
  beKey: "metric",
  labelCase: "title_case",
  filterMetaData: {
    options: METRICS_OPTIONS,
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};

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
