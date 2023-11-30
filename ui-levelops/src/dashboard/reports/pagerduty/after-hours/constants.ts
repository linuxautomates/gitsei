import { chartProps } from "dashboard/reports/commonReports.constants";

export const pagerdutyAfterHoursChartProps = {
  unit: "Minutes",
  chartProps: chartProps,
  barProps: [
    {
      name: "value",
      dataKey: "value"
    }
  ],
  stacked: false
};
