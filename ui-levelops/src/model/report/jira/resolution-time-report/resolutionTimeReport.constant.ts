import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  VALUE_SORT_KEY,
  WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE
} from "dashboard/constants/filter-name.mapping";
import { Dict } from "types/dict";
import { IMPLICITY_INCLUDE_DRILLDOWN_FILTER } from "dashboard/constants/applications/names";

export interface ResolutionTimeReportType extends BaseJiraReportTypes {
  tooltipMapping: { [key: string]: string };
  dataKey: string[];
  xAxisLabelTransform: (params: any) => void;
  weekStartsOnMonday: boolean;
  xaxis: true;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  [VALUE_SORT_KEY]: string;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: Dict<string, boolean>;
  [WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE]: Function;
}
