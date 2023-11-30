import { jiraChartProps } from "../commonJiraReports.constants";
import { basicMappingType } from "../../../dashboard-types/common-types";

export const jiraBounceTrendsChartProps: basicMappingType<any> = {
  unit: "Bounces",
  chartProps: jiraChartProps
};

export const jiraBounceReportsTrendsCompositeTransform: basicMappingType<string> = {
  min: "bounce_min",
  median: "bounce_median",
  max: "bounce_max"
};

export const jiraBounceReportsTrendsFilters: basicMappingType<any> = {
  across: "trend"
};
