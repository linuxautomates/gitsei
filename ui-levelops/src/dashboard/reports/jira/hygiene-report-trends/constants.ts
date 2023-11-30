import { jiraDrilldown } from "dashboard/constants/drilldown.constants";
import { jiraHygieneDrilldownTranformer } from "dashboard/helpers/drilldown-transformers";
import { chartProps } from "dashboard/reports/commonReports.constants";

export const jiraHygieneReportsTrendChartTypes = {
  unit: "Score",
  chartProps: chartProps,
  areaProps: [],
  stackedArea: true
};

export const jiraHygieneReportsTrendFilter = {
  across: "trend"
};

export const jiraHygieneReportsTrendDefaultQueryr = {
  interval: "month",
  visualization: "stacked_area"
};
