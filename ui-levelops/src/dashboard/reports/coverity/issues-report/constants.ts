import { chartProps } from "dashboard/reports/commonReports.constants";

export const ACROSS_OPTIONS = [
  { label: "Impact", value: "impact" },
  { label: "Category", value: "category" },
  { label: "Kind", value: "kind" },
  { label: "Checker Name", value: "checker_name" },
  { label: "Component Name", value: "component_name" },
  { label: "Type", value: "type" },
  { label: "Domain", value: "domain" },
  { label: "First Detected Stream", value: "first_detected_stream" },
  { label: "Last Detected Stream", value: "last_detected_stream" },
  { label: "File", value: "file" },
  { label: "Function", value: "function" },
  { label: "First detected", value: "first_detected" },
  { label: "Last detected", value: "last_detected" }
];

export const coverutyIssuesReportChartTypes = {
  barProps: [
    {
      name: "total_defects",
      dataKey: "total_defects",
      unit: "Defects"
    }
  ],
  stacked: false,
  unit: "Defects",
  sortBy: "total_defects",
  chartProps: chartProps
};
