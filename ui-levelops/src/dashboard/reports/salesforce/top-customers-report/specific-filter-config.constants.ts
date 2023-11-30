import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalInputRangeTypeFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalInputRangeTypeFilter";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { get } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const TimeFilterConfig: LevelOpsFilter = {
  id: "age",
  renderComponent: UniversalInputRangeTypeFilterWrapper,
  label: "Time (In Days)",
  beKey: "age",
  labelCase: "none",
  defaultValue: 30,
  filterMetaData: {
    type: "number",
    mapFilterValueForBE: (current: any) => ({ $lt: current })
  },
  getMappedValue: (args: any) => {
    const { allFilters } = args;
    const ageValue = get(allFilters, ["age"], undefined);
    if (ageValue) return get(ageValue, ["$lt"], 30);
    return undefined;
  },
  apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
  helpText: "Press Enter to update",
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
