import { RANGE_FILTER_CHOICE } from "dashboard/constants/filter-key.mapping";
import moment from "moment";

export const DEFAULT_METADATA = {
  [RANGE_FILTER_CHOICE]: {
    workitem_created_at: {
      type: "relative",
      relative: {
        last: {
          num: 30,
          unit: "days"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};
export const issueSingleDefualtCreatedAt = {
  $lt: moment.utc().unix().toString(),
  $gt: moment.utc().unix().toString()
};
export const DEFAULT_QUERY = { agg_type: "average", workitem_created_at: issueSingleDefualtCreatedAt };
export const REPORT_NAME = "Issue Resolution Time Single Stat";
export const URI = "issue_management_resolution_time_report";
export const CHART_PROPS = {
  unit: "Days"
};
export const DEFAULT_ACROSS = "workitem_created_at";
export const COMPARE_FIELD = "median";
export const SUPPORTED_WIDGET_TYPES = ["stats"];
