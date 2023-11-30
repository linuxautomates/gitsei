import { basicMappingType } from "dashboard/dashboard-types/common-types";

export const leadTimeForChangesSingleStatFilters: basicMappingType<any> = {
  across: "trend"
};

export const leadTimeForChangesSingleStatChartProps: basicMappingType<any> = {
  unit: "Days"
};

export const leadTimeForChangesSingleStatDefaultQuery: basicMappingType<any> = {
  metric: "resolve"
};

export const LEAD_TIME_CHANGES_DESCRIPTION =
  " Lead Time for Changes as per DORA metrics is defined as the amount of time it takes a commit to get into production. For the Elite performing teams, the lead time value is less than one day, the High is between one day and one week, the Medium is between one week and one month, and the Low is greater than one month.";
