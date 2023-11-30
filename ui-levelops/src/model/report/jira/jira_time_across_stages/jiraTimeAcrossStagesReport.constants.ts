import {
  IMPLICITY_INCLUDE_DRILLDOWN_FILTER,
  TIME_FILTER_RANGE_CHOICE_MAPPER
} from "dashboard/constants/applications/names";
import { PARTIAL_FILTER_KEY, SHOW_SETTINGS_TAB } from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { Dict } from "types/dict";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";

export interface JiraTimeAcrossStagesReportTypes extends BaseJiraReportTypes {
  xaxis: boolean;
  dataKey: string;
  [TIME_FILTER_RANGE_CHOICE_MAPPER]: { issue_resolved_at: string };
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [FILTER_WITH_INFO_MAPPING]: any;
  [SHOW_SETTINGS_TAB]: boolean;
  [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: { [key: string]: any };
}
