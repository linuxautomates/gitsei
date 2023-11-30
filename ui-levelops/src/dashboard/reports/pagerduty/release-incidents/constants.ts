import { chartProps } from "dashboard/reports/commonReports.constants";

export const pagerdutyReleaseIncidentsReportChartProps = {
  unit: "Count",
  chartProps: chartProps,
  barProps: [
    {
      name: "count",
      dataKey: "count"
    }
  ],
  stacked: true
};

export const jenkinsSupportedFilters = {
  uri: "jenkins_pipelines_jobs_filter_values",
  values: ["cicd_job_id"]
};

export const ACROSS_OPTIONS = [
  { label: "Incident Priority", value: "incident_priority" },
  { label: "Alert Severity", value: "alert_severity" },
  { label: "Incident Urgency", value: "incident_urgency" }
];
