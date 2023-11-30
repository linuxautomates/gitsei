import { jiraChartProps } from "../commonJiraReports.constants";

export const sprintImpactOfUnestimatedTicketsChartColors = ["#A5C980", "#4197FF", "#CB98F3", "#FF4D4F"];

export const sprintImpactOfUnestimatedTicketsChartProps = {
  customColors: sprintImpactOfUnestimatedTicketsChartColors,
  showNegativeValuesAsPositive: true,
  hideTotalInTooltip: true,
  stacked: true,
  hideGrid: true,
  chartProps: {
    ...jiraChartProps,
    stackOffset: "sign",
    barProps: [
      {
        name: "Missed Points",
        dataKey: "missed_points",
        unit: "Points"
      }
    ],
    legendProps: {
      align: "left",
      iconType: "circle",
      verticalAlign: "bottom"
    },
    className: "sprint_impact_estimated_bar_chart"
  },
  unit: "Points"
};

export const METRIC_OPTIONS = [{ value: "avg_commit_to_done", label: "Avg. Done to Commit" }];
