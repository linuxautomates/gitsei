import { jiraDrilldown } from "dashboard/constants/drilldown.constants";
import { jiraHygieneDrilldownTranformer } from "dashboard/helpers/drilldown-transformers";
import { chartProps } from "dashboard/reports/commonReports.constants";

export const jiraHygieneReportChartTypes = {
  unit: "tickets",
  barProps: [
    {
      name: "total_tickets",
      dataKey: "total_tickets"
    }
  ],
  stacked: false,
  sortBy: "total_tickets",
  chartProps: chartProps
};

export const jiraHygieneReportDrilldown = {
  ...jiraDrilldown,
  drilldownTransformFunction: (data: any) => jiraHygieneDrilldownTranformer(data)
};
