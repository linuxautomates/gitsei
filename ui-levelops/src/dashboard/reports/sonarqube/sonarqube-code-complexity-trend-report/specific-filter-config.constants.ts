import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalInputTimeRangeWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalInputTimeRangeWrapper";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const ComplexityRangeFilterConfig: LevelOpsFilter = {
  id: "complexity_score",
  renderComponent: UniversalInputTimeRangeWrapper,
  label: "Complexity Score",
  beKey: "complexity_score",
  labelCase: "none",
  filterMetaData: {
    greaterThanKey: "$gt",
    lessThanKey: "$lt"
  },
  deleteSupport: true,
  apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
