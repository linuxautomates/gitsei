import { FE_BASED_FILTERS, PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { ALLOWED_WIDGET_DATA_SORTING, FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { Dict } from "types/dict";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";

export interface JiraBacklogTrendReportTypes extends BaseJiraReportTypes {
  xaxis: boolean;
  stack_filters: string[];
  get_custom_chart_props: (...args: any) => any;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [FE_BASED_FILTERS]: any;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
}
