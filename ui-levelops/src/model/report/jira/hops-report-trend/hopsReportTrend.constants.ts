import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING, FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { FE_BASED_FILTERS } from "dashboard/constants/applications/names";
import { Dict } from "types/dict";
import { PARTIAL_FILTER_KEY } from "dashboard/constants/filter-key.mapping";

export interface HopsReportTrendType extends BaseJiraReportTypes {
  xaxis: boolean;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
}
