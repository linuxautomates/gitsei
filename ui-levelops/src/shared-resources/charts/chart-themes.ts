import { ChartThemeOptions } from "./chart-types";
import { PriorityTypes } from "./jira-prioirty-chart/helper";

export const donutColors = ["#2967dd", "#975ee3", "#37b57e", "#ffad00", "#ff5630"];

export const sprintMetricsPercentageColors = ["#DB3C18", "#2a67dd"];
export const sprintMetricsChartColors = ["#A5C980", "#4197FF", "#CB98F3", "#FF4D4F"];

export const lineChartColors = [
  "#53bdc5",
  "#336ad5",
  "#dd8b39",
  "#ca458d",
  "#8d8bf3",
  "#78dee0",
  "#6740ab",
  "#dcbf40",
  "#c0732d",
  "#468b6e",
  "#ade86b",
  "#c9b1bd",
  "#c297b8",
  "#d4adcf",
  "#c3d350",
  "#bc7c9c",
  "#ffb2e6"
];

export const sankeyChartColors = [
  "#D8F2F3",
  "#D3E1F7",
  "#F7E8D9",
  "#F4D9E9",
  "#E8E8FD",
  "#E2F9F9",
  "#E0D9EF",
  "#F8F3DB",
  "#F2E3D6",
  "#D8E8E2",
  "#EEFAE2"
];

export const effortInvestmentDonutBarColors = [
  "#9969D6",
  "#74B2DD",
  "#AAC885",
  "#FCD491",
  "#FC91AA",
  "#DB7777",
  "#EFB884",
  "#F0E369",
  "#82D4C9",
  "#BFEEF1"
];

export const chartStaticColors = {
  axisColor: "var(--grey12)"
};

export const chartTransparentStaticColors = {
  axisColor: "rgb(255, 255, 255) transparent"
};

export const radarColors = {
  polygon: "#1B2C3C1A",
  label: "#5D6F8B"
};

//TODO: provide actual hex values.
//Any new theme added here must also be added to the type
export const chartThemes: ChartThemeOptions = {
  blue: {
    stroke: "#1C90E6",
    fill: "#1C90E626"
  },
  red: {
    stroke: "#E2392B",
    fill: "#E2392B1A"
  },
  yellow: {
    stroke: "yellow",
    fill: "lightyellow"
  }
};

export const lineChartKeys = [
  "pr_created",
  "pr_merged",
  "pr_updated",
  "pr_reviewed",
  "min",
  "median",
  "max",
  "issue_created",
  "issue_closed",
  "issue_updated",
  "first_comment",
  "total_jira_tickets",
  "total_zendesk_tickets",
  "jenkins_pipeline_jobs_duration_counts",
  "pagerduty_incident_count",
  "total_sonarQube_issues",
  "total_testrails_tests",
  "testrails_tests_estimate_min",
  "testrails_tests_estimate_median",
  "testrails_tests_estimate_max",
  "testrails_tests_estimate_forecast_min",
  "testrails_tests_estimate_forecast_median",
  "testrails_tests_estimate_forecast_max",
  "sonarQube_effort_max",
  "sonarQube_effort_min",
  "sonarQube_effort_median",
  "sonarQube_effort_sum",
  "sonarQube_debt_max",
  "sonarQube_debt_min",
  "sonarQube_debt_median",
  "sonarQube_metrics_max",
  "sonarQube_metrics_min",
  "sonarQube_metrics_median",
  "sonarQube_code_complexity_min",
  "sonarQube_code_complexity_median",
  "sonarQube_code_complexity_max",
  "zendesk_bounce_min",
  "zendesk_bounce_max",
  "zendesk_bounce_median",
  "zendesk_hops_min",
  "zendesk_hops_median",
  "zendesk_hops_max",
  "zendesk_response_time_min",
  "zendesk_response_time_max",
  "zendesk_response_time_median",
  "zendesk_resolution_time_min",
  "zendesk_resolution_time_max",
  "zendesk_resolution_time_median",
  "zendesk_requester_wait_time_min",
  "zendesk_requester_wait_time_median",
  "zendesk_requester_wait_time_max",
  "zendesk_agent_wait_time_min",
  "zendesk_agent_wait_time_median",
  "zendesk_agent_wait_time_max",
  "cicd_jobs_commits_min",
  "cicd_jobs_commits_median",
  "cicd_jobs_commits_max",
  "cicd_jobs_duration_min",
  "cicd_jobs_duration_median",
  "cicd_jobs_duration_max",
  "response_time_min",
  "response_time_median",
  "response_time_max",
  "pagerduty_incident_count",
  "function_percentage_coverage",
  "condition_percentage_coverage",
  "decision_percentage_coverage",
  "coverage_percentage",
  "duplicated_density"
];
export const barChartKeys = [
  "count",
  "total_tickets",
  "total_cases",
  "cicd_jobs_count",
  "jenkins_jobs_config_count",
  "jenkins_pipeline_job_counts"
];

export const areaChartKeys = [
  "lines_added_count",
  "lines_removed_count",
  "lines_changed_count",
  "prs_created",
  "prs_updated",
  "prs_merged",
  // "total_tickets", // TODO : add back
  "total_cases"
];

export const treeMapColors = ["#0073CF", "#33A6FF", "#66D9FF"];

export const mappedSortValues: { [x: string]: string } = {
  num_commits: "num_commits",
  changes: "total_changes",
  deletions: "total_deletions",
  additions: "total_additions"
};

export const newUXColorMapping: { [x: string]: string } = {
  "blue-primary": "#1A6BB6",
  "blue-secondary": "#096DD9",
  orange: "#DB6A18",
  red: "#B62C1A",
  yellow: "#FAAD14",
  grey: "#8A94A5",
  light_grey: "#DADDDA",
  [PriorityTypes.HIGH]: "#d46b08",
  [PriorityTypes.HIGHEST]: "#a8071a",
  [PriorityTypes.LOW]: "#d6e4ff",
  [PriorityTypes.LOWEST]: "#e8e8e8",
  [PriorityTypes.MEDIUM]: "#d48806",
  completed: "#2967dd",
  remaining: "#DADDDA",
  new: "#FFCB73"
};

export const colorPalletteShadesObj = {
  "red-2": "#FFCCC7",
  "orange-2": "#FFE7BA",
  "yellow-2": "#FFFFB8",
  "green-2": "#D9F7BE",
  "cyan-2": "#B5F5EC",
  "blue-2": "#BAE7FF",
  "pink-2": "#FFD6E7",
  "gray-2": "#FAFAFA",
  "purple-2": "#EFDBFF",
  "red-3": "#FFA39E",
  "orange-3": "#FFD591",
  "yellow-3": "#FFFB8F",
  "green-3": "#B7EB8F",
  "cyan-3": "#87E8DE",
  "blue-3": "#91D5FF",
  "pink-3": "#FFADD2",
  "gray-3": "#F5F5F5",
  "purple-3": "#D3ADF7",
  "red-4": "#FF7875",
  "orange-4": "#FFC069",
  "yellow-4": "#FFF566",
  "green-4": "#95DE64",
  "cyan-4": "#5CDBD3",
  "blue-4": "#69C0FF",
  "pink-4": "#FF85C0",
  "gray-4": "#F0F0F0",
  "purple-4": "#B37FEB",
  "red-5": "#FF4D4F",
  "orange-5": "#FFA940",
  "yellow-5": "#FFEC3D",
  "green-5": "#73D13D",
  "cyan-5": "#36CFC9",
  "blue-5": "#40A9FF",
  "pink-5": "#F759AB",
  "gray-5": "#D9D9D9",
  "purple-5": "#9254DE",
  "red-6": "#F5222D",
  "orange-6": "#FA8C16",
  "yellow-6": "#FADB14",
  "green-6": "#52C41A",
  "cyan-6": "#13C2C2",
  "blue-6": "#1890FF",
  "pink-6": "#EB2F96",
  "gray-6": "#BFBFBF",
  "purple-6": "#722ED1",
  "red-7": "#CF1322",
  "orange-7": "#D46B08",
  "yellow-7": "#D4B106",
  "green-7": "#389E0D",
  "cyan-7": "#08979C",
  "blue-7": "#096DD9",
  "pink-7": "#C41D7F",
  "gray-7": "#8C8C8C",
  "purple-7": "#531DAB",
  "red-8": "#A8071A",
  "orange-8": "#AD4E00",
  "yellow-8": "#AD8B00",
  "green-8": "#237804",
  "cyan-8": "#006D75",
  "blue-8": "#0050B3",
  "pink-8": "#9E1068",
  "gray-8": "#595959",
  "purple-8": "#391085",
  "red-9": "#820014",
  "orange-9": "#873800",
  "yellow-9": "#876800",
  "green-9": "#135200",
  "cyan-9": "#00474F",
  "blue-9": "#003A8C",
  "pink-9": "#780650",
  "gray-9": "#434343",
  "purple-9": "#22075E",
  "red-10": "#5C0011",
  "orange-10": "#612500",
  "yellow-10": "#614700",
  "green-10": "#092B00",
  "cyan-10": "#002329",
  "blue-10": "#002766",
  "pink-10": "#520339",
  "gray-10": "#262626",
  "purple-10": "#120338"
};

export const colorPalletteShades = [
  "#F2917A",
  "#EEA248",
  "#CAC035",
  "#84C67B",
  "#30C5E8",
  "#66AEEF",
  "#86A2F1",
  "#B8322E",
  "#9C4D0E",
  "#655E10",
  "#35672F",
  "#126176",
  "#2846A4",
  "#412F9D",
  "#E14331",
  "#C6670B",
  "#847B0D",
  "#3A8433",
  "#0B93A4",
  "#2959C3",
  "#6045E3",
  "#F1664B",
  "#D58A19",
  "#A99F15",
  "#4EA64B",
  "#28CED4",
  "#3886EB",
  "#5C5CEE",
  "#F4C0B2",
  "#F0D368",
  "#E4DE77",
  "#C0E0B1",
  "#87D5D7",
  "#9FD1F3",
  "#B2C9F4"
];

export const redShades: Record<string, string> = {
  [`red-2`]: "#FFCCC7",
  [`red-3`]: "#FFA39E",
  [`red-4`]: "#FF7875",
  [`red-5`]: "#FF4D4F",
  [`red-6`]: "#F5222D",
  [`red-7`]: "#CF1322",
  [`red-8`]: "#A8071A",
  [`red-9`]: "#820014",
  [`red-10`]: "#5C0011"
};

export const orangeShades: Record<string, string> = {
  ["orange-2"]: "#FFE7BA",
  ["orange-3"]: "#FFD591",
  ["orange-4"]: "#FFC069",
  ["orange-5"]: "#FFA940",
  ["orange-6"]: "#FA8C16",
  ["orange-7"]: "#D46B08",
  ["orange-8"]: "#AD4E00",
  ["orange-9"]: "#873800",
  ["orange-10"]: "#612500"
};

export const yellowShades: Record<string, string> = {
  ["yellow-2"]: "#FFFFB8",
  ["yellow-3"]: "#FFFB8F",
  ["yellow-4"]: "#FFF566",
  ["yellow-5"]: "#FFEC3D",
  ["yellow-6"]: "#FADB14",
  ["yellow-7"]: "#D4B106",
  ["yellow-8"]: "#AD8B00",
  ["yellow-9"]: "#876800",
  ["yellow-10"]: "#614700"
};

export const greenShades: Record<string, string> = {
  ["green-2"]: "#D9F7BE",
  ["green-3"]: "#B7EB8F",
  ["green-4"]: "#95DE64",
  ["green-5"]: "#73D13D",
  ["green-6"]: "#52C41A",
  ["green-7"]: "#389E0D",
  ["green-8"]: "#237804",
  ["green-9"]: "#135200",
  ["green-10"]: "#092B00"
};

export const cyanShades: Record<string, string> = {
  [`cyan-2`]: "#B5F5EC",
  [`cyan-3`]: "#87E8DE",
  [`cyan-4`]: "#5CDBD3",
  [`cyan-5`]: "#36CFC9",
  [`cyan-6`]: "#13C2C2",
  [`cyan-7`]: "#08979C",
  [`cyan-8`]: "#006D75",
  [`cyan-9`]: "#00474F",
  [`cyan-10`]: "#002329"
};

export const blueShades: Record<string, string> = {
  [`blue-2`]: "#BAE7FF",
  [`blue-3`]: "#91D5FF",
  [`blue-4`]: "#69C0FF",
  [`blue-5`]: "#40A9FF",
  [`blue-6`]: "#1890FF",
  [`blue-7`]: "#096DD9",
  [`blue-8`]: "#0050B3",
  [`blue-9`]: "#003A8C",
  [`blue-10`]: "#002766"
};

export const pinkShades: Record<string, string> = {
  [`pink-2`]: "#FFD6E7",
  [`pink-3`]: "#FFADD2",
  [`pink-4`]: "#FF85C0",
  [`pink-5`]: "#F759AB",
  [`pink-6`]: "#EB2F96",
  [`pink-7`]: "#C41D7F",
  [`pink-8`]: "#9E1068",
  [`pink-9`]: "#780650",
  [`pink-10`]: "#520339"
};

export const greyShades: Record<string, string> = {
  [`gray-2`]: "#FAFAFA",
  [`gray-3`]: "#F5F5F5",
  [`gray-4`]: "#F0F0F0",
  [`gray-5`]: "#D9D9D9",
  [`gray-6`]: "#BFBFBF",
  [`gray-7`]: "#8C8C8C",
  [`gray-8`]: "#595959",
  [`gray-9`]: "#434343",
  [`gray-10`]: "#262626"
};

export const purpleShades: Record<string, string> = {
  [`purple-2`]: "#EFDBFF",
  [`purple-3`]: "#D3ADF7",
  [`purple-4`]: "#B37FEB",
  [`purple-5`]: "#9254DE",
  [`purple-6`]: "#722ED1",
  [`purple-7`]: "#531DAB",
  [`purple-8`]: "#391085",
  [`purple-9`]: "#22075E",
  [`purple-10`]: "#120338"
};
