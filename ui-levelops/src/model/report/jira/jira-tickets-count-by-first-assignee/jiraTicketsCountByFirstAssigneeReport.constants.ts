import { FE_BASED_FILTERS } from "dashboard/constants/applications/names";
import { PARTIAL_FILTER_KEY, SHOW_SETTINGS_TAB } from "dashboard/constants/filter-key.mapping";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  VALUE_SORT_KEY
} from "dashboard/constants/filter-name.mapping";
import { Dict } from "types/dict";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";

export interface JiraTicketsCountByFirstAssigneeReportTypes extends BaseJiraReportTypes {
  xaxis: boolean;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  [VALUE_SORT_KEY]: string;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [SHOW_SETTINGS_TAB]: boolean;
}
