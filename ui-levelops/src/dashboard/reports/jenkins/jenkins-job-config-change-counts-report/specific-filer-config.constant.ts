import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { LevelOpsFilter, DropDownData } from "model/filters/levelopsFilters";
import { STACK_OPTIONS } from "./constants";

export const StackFilterConfig: LevelOpsFilter = {
  id: "stacks",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stacks",
  beKey: "stacks",
  labelCase: "title_case",
  disabled: ({ filters }) => {
    return filters && filters.visualization && filters.visualization === IssueVisualizationTypes.DONUT_CHART;
  },
  filterMetaData: {
    clearSupport: true,
    options: STACK_OPTIONS,
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};
