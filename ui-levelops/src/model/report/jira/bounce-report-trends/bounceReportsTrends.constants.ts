import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING } from "dashboard/constants/filter-name.mapping";

export interface JiraBounceReportTrendsType extends BaseJiraReportTypes {
  xaxis: boolean;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
}
