import { bullseyeDrilldown } from "dashboard/constants/drilldown.constants";
import { chartProps } from "dashboard/reports/commonReports.constants";
import { defaultSorts } from "../commonBullseyeReports.constants";

export const bullseyeDecisionCoverageTrendReportChartTypes = {
  unit: "Percentage (%)",
  chartProps: chartProps,
  lineProps: [{ dataKey: "decision_percentage_coverage", transformer: (data: any) => data + "%" }]
};

export const bullseyeDecisionCoverageTrendReportFilter = {
  across: "trend",
  sort: defaultSorts.decision
};

export const bullseyeDecisionCoverageTrendReportDrilldown = {
  ...bullseyeDrilldown,
  defaultSort: defaultSorts.decision
};
