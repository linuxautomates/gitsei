import { bullseyeDrilldown } from "dashboard/constants/drilldown.constants";
import { chartProps } from "dashboard/reports/commonReports.constants";
import { defaultSorts } from "../commonBullseyeReports.constants";

export const bullseyeCodeCoverageTrendReportChartTypes = {
  unit: "Percentage (%)",
  chartProps: chartProps,
  lineProps: [{ dataKey: "coverage_percentage", transformer: (data: any) => data + "%" }]
};

export const bullseyeCodeCoverageTrendReportFilter = {
  across: "trend",
  sort: defaultSorts.code
};

export const bullseyeCodeCoverageTrendReportDrilldown = { ...bullseyeDrilldown, defaultSort: defaultSorts.code };
