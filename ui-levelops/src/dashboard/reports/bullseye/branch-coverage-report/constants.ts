import { bullseyeDrilldown } from "dashboard/constants/drilldown.constants";
import { chartProps } from "dashboard/reports/commonReports.constants";
import { defaultSorts } from "../commonBullseyeReports.constants";

export const bullseyeBranchCoverageReportChartTypes = {
  barProps: [
    {
      name: "Branch Coverage",
      dataKey: "condition_percentage_coverage"
    }
  ],
  unit: "Percentage (%)",
  stacked: false,
  transformFn: (data: any) => data + "%",
  chartProps: chartProps
};

export const bullseyeBranchCoverageReportFilter = {
  sort: defaultSorts.branch
};

export const bullseyeBranchCoverageReportDrilldown = { ...bullseyeDrilldown, defaultSort: defaultSorts.branch };

export const bullseyeBranchCoverageReportFilterNameMapping = {
  job_normalized_full_name: "jenkins job path" // used for global filter label
};

export const ACROSS_OPTIONS = [
  { label: "JENKINS JOB NAME", value: "job_name" },
  { label: "JENKINS JOB PATH", value: "job_normalized_full_name" },
  { label: "NAME", value: "name" },
  { label: "PROJECT", value: "project" }
];
