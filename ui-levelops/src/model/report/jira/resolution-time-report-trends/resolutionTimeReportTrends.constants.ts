import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING } from "../../../../dashboard/constants/filterWithInfo.mapping";
import { IMPLICITY_INCLUDE_DRILLDOWN_FILTER } from "../../../../dashboard/constants/applications/names";

export interface ResolutionTimeReportTrendsType extends BaseJiraReportTypes {
  xaxis: false;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  [FILTER_WITH_INFO_MAPPING]: { [key: string]: any };
  [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: { [key: string]: any };
}
