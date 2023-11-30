import { bullseyeDrilldown } from "dashboard/constants/drilldown.constants";
import { chartProps } from "dashboard/reports/commonReports.constants";
import { defaultSorts } from "../commonBullseyeReports.constants";

export const bullseyeFunctionCoverageReportChartTypes = {
  barProps: [
    {
      name: "Function Coverage",
      dataKey: "function_percentage_coverage"
    }
  ],
  unit: "Percentage (%)",
  stacked: false,
  transformFn: (data: any) => data + "%",
  chartProps: chartProps
};

export const bullseyeFunctionCoverageReportFilter = {
  sort: defaultSorts.function
};

export const bullseyeFunctionCoverageReportDrilldown = { ...bullseyeDrilldown, defaultSort: defaultSorts.function };

export const bullseyeFunctionCoverageReportFilterNameMapping = {
  job_normalized_full_names: "jenkins job path" // used for global filter label
};

export const bullseyeAcrossOptions = [
  { label: "JENKINS JOB NAME", value: "job_name" },
  { label: "JENKINS JOB PATH", value: "job_normalized_full_name" },
  { label: "NAME", value: "name" },
  { label: "PROJECT", value: "project" }
];
