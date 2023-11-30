import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import HygieneWeightFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/HygieneFilter/HygieneWeights.container";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";

const hygieneTypes = [
  "IDLE",
  "POOR_DESCRIPTION",
  "NO_DUE_DATE",
  "NO_ASSIGNEE",
  "NO_COMPONENTS",
  "MISSED_RESPONSE_TIME",
  "MISSED_RESOLUTION_TIME",
  "INACTIVE_ASSIGNEES"
];

export const HygieneWeightsFiltersConfig: LevelOpsFilter = {
  id: "all_hygienes",
  renderComponent: HygieneWeightFilterContainer,
  label: "all_hygienes",
  beKey: "",
  labelCase: "title_case",
  filterMetaData: {
    options: hygieneTypes.map(type => ({ label: toTitleCase(type), value: type }))
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.WEIGHTS
};
