import { bullseyeDrilldown } from "dashboard/constants/drilldown.constants";
import { chartProps } from "dashboard/reports/commonReports.constants";
import { defaultSorts } from "../commonBullseyeReports.constants";

export const bullseyeBranchCoverageTrendReportChartTypes = {
  unit: "Percentage (%)",
  chartProps: chartProps,
  lineProps: [{ dataKey: "condition_percentage_coverage", transformer: (data: any) => data + "%" }]
};

export const bullseyeBranchCoverageTrendReportFilter = {
  across: "trend",
  sort: defaultSorts.branch
};

export const bullseyeBranchCoverageTrendReportDrilldown = { ...bullseyeDrilldown, defaultSort: defaultSorts.branch };
