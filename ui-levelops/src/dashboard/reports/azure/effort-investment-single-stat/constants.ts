import moment from "moment";
import { RequiredFiltersType } from "../../../constants/bussiness-alignment-applications/constants";

export const DEFAULT_QUERY = {
  workitem_resolved_at: {
    // required filters and default is last month
    $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  }
};

export const REQUIRED_FILTERS = {
  [RequiredFiltersType.SCHEME_SELECTION]: true
};

export const REPORT_NAME = "Effort Investment Single Stat";
export const URI = "azure_effort_investment_tickets";
export const SUPPORTED_WIDGET_TYPES = ["stats"];
export const DISPLAY_FORMAT_KEY = "percentage";
export const EFFORT_UNIT = "azure_effort_investment_tickets";
