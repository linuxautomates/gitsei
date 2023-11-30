import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING } from "dashboard/constants/filter-name.mapping";
import { IMPLICITY_INCLUDE_DRILLDOWN_FILTER } from "../../../../dashboard/constants/applications/names";

export interface ResponseTimeReportTrendsType extends BaseJiraReportTypes {
  xaxis: false;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: { [key: string]: any };
}
