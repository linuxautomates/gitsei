import { jiraChartProps } from "../commonJiraReports.constants";

export const hopsReportChartProps = {
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
  unit: "Hops",
  chartProps: jiraChartProps,
  xAxisIgnoreSortKeys: ["priority"]
};
