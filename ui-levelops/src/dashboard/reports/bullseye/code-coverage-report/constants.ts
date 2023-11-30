import { bullseyeDrilldown } from "dashboard/constants/drilldown.constants";
import { chartProps } from "dashboard/reports/commonReports.constants";
import { defaultSorts } from "../commonBullseyeReports.constants";

export const bullseyeCodeCoverageReportChartTypes = {
  barProps: [
    {
      name: "Code Coverage",
      dataKey: "coverage_percentage"
    }
  ],
  unit: "Percentage (%)",
  transformFn: (data: any) => data + "%",
  stacked: false,
  chartProps: chartProps
};

export const bullseyeCodeCoverageReportFilter = {
  sort: defaultSorts.code
};

export const bullseyeCodeCoverageReportDrilldown = { ...bullseyeDrilldown, defaultSort: defaultSorts.code };

export const bullseyeAcrossOptions = [
  { label: "JENKINS JOB NAME", value: "job_name" },
  { label: "JENKINS JOB PATH", value: "job_normalized_full_name" },
  { label: "NAME", value: "name" },
  { label: "PROJECT", value: "project" }
];
