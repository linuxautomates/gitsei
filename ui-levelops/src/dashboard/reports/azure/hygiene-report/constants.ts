import { azureDrilldown } from "dashboard/constants/drilldown.constants";
import { azureHygieneDrilldownTranformer } from "dashboard/helpers/drilldown-transformers/jiraDrilldownTransformer";
import { FilterConfigBasedPreviewFilterConfigType } from "model/report/baseReport.constant";

export const CHART_PROPS = {
  unit: "tickets",
  barProps: [
    {
      name: "total_tickets",
      dataKey: "total_tickets"
    }
  ],
  stacked: false,
  sortBy: "total_tickets",
  chartProps: {
    barGap: 0,
    margin: { top: 20, right: 5, left: 5, bottom: 50 }
  }
};

export const REPORT_NAME = "Issue Hygiene Report";
export const URI = "issue_management_tickets_report";
export const DEFAULT_ACROSS = "project";
export const HYGIENE_URI = "issue_management_tickets_report";
export const DRILL_DOWN = {
  ...azureDrilldown,
  drilldownTransformFunction: azureHygieneDrilldownTranformer
};

export const DEFAULT_QUERY = {
  hideScore: false
};

export const FILTER_CONFIG_BASED_PREVIEW_FILTERS: FilterConfigBasedPreviewFilterConfigType[] = [
  { filter_key: "workitem_feature", valueKey: "workitem_id", labelKey: "summary" },
  { filter_key: "workitem_user_story", valueKey: "workitem_id", labelKey: "summary" }
];
