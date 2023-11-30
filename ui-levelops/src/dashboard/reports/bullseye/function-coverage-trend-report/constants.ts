import { bullseyeDrilldown } from "dashboard/constants/drilldown.constants";
import { chartProps } from "dashboard/reports/commonReports.constants";
import { defaultSorts } from "../commonBullseyeReports.constants";

export const bullseyeFunctionCoverageTrendReportChartTypes = {
  unit: "Percentage (%)",
  chartProps: chartProps,
  lineProps: [{ dataKey: "function_percentage_coverage", transformer: (data: any) => data + "%" }]
};

export const bullseyeFunctionCoverageTrendReportFilter = {
  across: "trend",
  sort: defaultSorts.function
};

export const bullseyeFunctionCoverageTrendReportDrilldown = {
  ...bullseyeDrilldown,
  defaultSort: defaultSorts.function
};
