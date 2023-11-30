import { allocationGoalsParameters, currentPrioritiesType } from "../types/ticketCategorization.types";

export const tabsConfig = [
  {
    label: "Basic Info",
    tab_key: "basic_info"
  },
  { label: "Categories", tab_key: "categories" },
  { label: "Allocation Goals", tab_key: "allocation_goals" }
];

export const CURRENT_PROPERTIES_DESCRIPTION =
  "Pick one or more options to identify current priorities for the teams. Below settings are used to compute resource allocation for current priorities.";

export const DEFAULT_PROFILE_DESC =
  "Default profile is used for data analysis when effort investment widgets don't explicitly specify a profile.";

export const currentPrioritiesInitialState = {
  // [currentPrioritiesType.ISSUE_IN_ACTIVE_RELEASE]: false, ---> Hiding for now
  [currentPrioritiesType.ISSUE_IN_ACTIVE_SPRINT]: false,
  [currentPrioritiesType.ISSUE_IN_IN_PROGRESS]: false,
  [currentPrioritiesType.ISSUE_IS_ASSIGNED]: false
};

export const allocationGoalsState = {
  [allocationGoalsParameters.IDEAL]: { ideal_range: { $gt: "0", $lt: "20" }, included: false },
  [allocationGoalsParameters.ACCEPTABLE]: { ideal_range: { $gt: "0", $lt: "20" }, included: false },
  [allocationGoalsParameters.POOR]: { ideal_range: { $gt: "0", $lt: "20" }, included: false }
};

export const allocationGoalsMappingColors = {
  [allocationGoalsParameters.POOR]: "#e33f3f",
  [allocationGoalsParameters.ACCEPTABLE]: "#fcb132",
  [allocationGoalsParameters.IDEAL]: "#789fe9"
};
