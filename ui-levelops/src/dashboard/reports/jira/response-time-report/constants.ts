import { jiraChartProps } from "../commonJiraReports.constants";

export const responseTimeChartProps = {
  barProps: [
    {
      name: "min",
      dataKey: "min"
    },
    {
      name: "median",
      dataKey: "median"
    },
    {
      name: "max",
      dataKey: "max"
    }
  ],
  stacked: false,
  unit: "Days",
  chartProps: jiraChartProps,
  xAxisIgnoreSortKeys: ["priority"],
  xAxisLabelKey: "additional_key"
};
