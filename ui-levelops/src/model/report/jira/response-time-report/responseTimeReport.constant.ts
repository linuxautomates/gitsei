import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING, VALUE_SORT_KEY } from "dashboard/constants/filter-name.mapping";
import { IMPLICITY_INCLUDE_DRILLDOWN_FILTER } from "../../../../dashboard/constants/applications/names";

export interface ResponseTimeReportType extends BaseJiraReportTypes {
  defaultFilterKey: string;
  showExtraInfoOnToolTip: string[];
  xAxisLabelTransform: (params: any) => void;
  [VALUE_SORT_KEY]: string;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  xaxis: boolean;
  [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: { [key: string]: any };
}
