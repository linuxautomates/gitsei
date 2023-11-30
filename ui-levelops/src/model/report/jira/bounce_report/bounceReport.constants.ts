import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { VALUE_SORT_KEY, ALLOWED_WIDGET_DATA_SORTING } from "dashboard/constants/filter-name.mapping";

export interface JiraBounceReportType extends BaseJiraReportTypes {
  xaxis: boolean;
  [VALUE_SORT_KEY]: string;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
}
