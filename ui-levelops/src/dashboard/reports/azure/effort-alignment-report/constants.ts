import {
  EIAlignmentReportCSVColumns,
  EIAlignmentReportCSVDataTransformer,
  EIAlignmentReportCSVFiltersTransformer,
  EIDynamicURITransformer
} from "dashboard/constants/bussiness-alignment-applications/BACSVHelperTransformer";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  RequiredFiltersType
} from "dashboard/constants/bussiness-alignment-applications/constants";
import moment from "moment";

export const DEFAULT_QUERY = {
  workitem_resolved_at: {
    // required filters and default is last month
    $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  },
  [ACTIVE_WORK_UNIT_FILTER_KEY]: "active_azure_ei_ticket_count"
};

export const REPORT_CSV_DEFAULT_CONFIG = {
  widgetFiltersTransformer: EIAlignmentReportCSVFiltersTransformer,
  widgetDynamicURIGetFunc: EIDynamicURITransformer,
  widgetCSVColumnsGetFunc: EIAlignmentReportCSVColumns,
  widgetCSVDataTransform: EIAlignmentReportCSVDataTransformer
};

export const REQUIRED_FILTER_MAPPING = {
  [RequiredFiltersType.SCHEME_SELECTION]: true
};

export const REPORT_NAME = "Effort Alignment Report";
export const DEFAULT_ACROSS = "ticket_category";
export const URI = "active_azure_ei_ticket_count";
export const MIN_WIDTH = "28rem";
