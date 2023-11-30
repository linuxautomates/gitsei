import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import HygieneWeightFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/HygieneFilter/HygieneWeights.container";
import UniversalTextSwitchWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTextSwitchWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
export const HygieneTypes = ["IDLE", "POOR_DESCRIPTION", "NO_CONTACT", "MISSED_RESOLUTION_TIME"];

export const HideScoreFilterConfig: LevelOpsFilter = {
  id: "hideScore",
  renderComponent: UniversalTextSwitchWrapper,
  label: "Hide Score",
  beKey: "hideScore",
  labelCase: "title_case",
  filterMetaData: {},
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const HygieneWeightsFiltersConfig: LevelOpsFilter = {
  id: "all_hygienes",
  renderComponent: HygieneWeightFilterContainer,
  label: "all_hygienes",
  beKey: "",
  labelCase: "title_case",
  filterMetaData: {
    options: HygieneTypes.map(type => ({ label: toTitleCase(type), value: type }))
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.WEIGHTS
};
