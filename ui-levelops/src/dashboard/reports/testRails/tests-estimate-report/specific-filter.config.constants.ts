import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { LevelOpsFilter, DropDownData } from "model/filters/levelopsFilters";
import { TESTRAILS_STACK_FILTERS_OPTIONS } from "../commonTestRailsReports.constants";

export const StackFilterConfig: LevelOpsFilter = {
  id: "stacks",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stacks",
  beKey: "stacks",
  labelCase: "title_case",
  filterMetaData: {
    clearSupport: true,
    options: TESTRAILS_STACK_FILTERS_OPTIONS,
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};
