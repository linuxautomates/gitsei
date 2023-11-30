import { SHOW_SETTINGS_TAB } from "dashboard/constants/filter-key.mapping";
import { ALLOWED_WIDGET_DATA_SORTING, FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { Dict } from "types/dict";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";

export interface TicketReportTrendTypes extends BaseJiraReportTypes {
  xaxis: boolean;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  [SHOW_SETTINGS_TAB]: boolean;
}
