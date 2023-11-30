import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { SHOW_METRICS_TAB } from "dashboard/constants/filter-key.mapping";
import { Dict } from "types/dict";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";

export interface ResponseTimeSingleStatType extends BaseJiraReportTypes {
  xaxis: boolean;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [SHOW_METRICS_TAB]: boolean;
}
