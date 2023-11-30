import { bullseyeDrilldown } from "dashboard/constants/drilldown.constants";
import { chartProps } from "dashboard/reports/commonReports.constants";
import { defaultSorts } from "../commonBullseyeReports.constants";

export const bullseyeDecisionCoverageReportChartTypes = {
  barProps: [
    {
      name: "Decision Coverage",
      dataKey: "decision_percentage_coverage"
    }
  ],
  unit: "Percentage (%)",
  stacked: false,
  transformFn: (data: any) => data + "%",
  chartProps: chartProps
};

export const bullseyeDecisionCoverageReportFilter = {
  sort: defaultSorts.decision
};

export const bullseyeDecisionCoverageReportDrilldown = { ...bullseyeDrilldown, defaultSort: defaultSorts.decision };

export const bullseyeAcrossOptions = [
  { label: "JENKINS JOB NAME", value: "job_name" },
  { label: "JENKINS JOB PATH", value: "job_normalized_full_name" },
  { label: "NAME", value: "name" },
  { label: "PROJECT", value: "project" }
];
