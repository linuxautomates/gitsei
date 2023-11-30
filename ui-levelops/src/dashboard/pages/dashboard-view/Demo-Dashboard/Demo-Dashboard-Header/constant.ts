import { optionType } from "dashboard/dashboard-types/common-types";

export const ORG_TREE_LIST_UUID = "ORG_TREE_LIST";
export const OU_DEMO_DASHBOARD_SEARCH_LIST_ID = "OU_DEMO_DASHBOARD_SEARCH_LIST_ID";
export enum DemoDashboards {
  PLANNING = "planning"
}
export enum DemoDashboardsTimeRanges {
  QUATER_1 = "jan22_mar22",
  QUATER_2 = "apr22_jun22"
}

export enum DemoEffortInvestmentProfileValues {
  INITIATIVES = "initiatives",
  BUSINESS_ALIGNMENT = "busines_alignment_categories",
  SOFTWARE_CAPITAL = "software_capitalization"
}

export enum DemoEffortInvestmentProfileLabel {
  INITIATIVES = "Initiatives",
  BUSINESS_ALIGNMENT = "Business Alignment Categories",
  SOFTWARE_CAPITAL = "Software capitalization"
}

export const DEMO_DASHBOARD_ICON_MAPPING: Record<DemoDashboards, string> = {
  [DemoDashboards.PLANNING]: "calendar"
};

export const DEMO_DASHBOARD_TIME_RANGE_OPTIONS: Array<optionType> = [
  {
    label: "Jan 2022 - Mar 2022",
    value: DemoDashboardsTimeRanges.QUATER_1
  },
  {
    label: "Apr 2022 - Jun 2022",
    value: DemoDashboardsTimeRanges.QUATER_2
  }
];

export const DEMO_EFFORT_INVESTMENT_PROFILE_OPTIONS: Array<optionType> = [
  {
    label: DemoEffortInvestmentProfileLabel.INITIATIVES,
    value: DemoEffortInvestmentProfileValues.INITIATIVES
  },
  {
    label: DemoEffortInvestmentProfileLabel.BUSINESS_ALIGNMENT,
    value: DemoEffortInvestmentProfileValues.BUSINESS_ALIGNMENT
  },
  {
    label: DemoEffortInvestmentProfileLabel.SOFTWARE_CAPITAL,
    value: DemoEffortInvestmentProfileValues.SOFTWARE_CAPITAL
  }
];
