import {
  COMPARE_X_AXIS_TIMESTAMP,
  FE_BASED_FILTERS,
  INCLUDE_INTERVAL_IN_PAYLOAD,
  LABEL_TO_TIMESTAMP
} from "dashboard/constants/applications/names";
import {
  PARTIAL_FILTER_KEY,
  SHOW_AGGREGATIONS_TAB,
  SHOW_METRICS_TAB,
  SHOW_SETTINGS_TAB
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { Dict } from "types/dict";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";

export interface HygieneReportsTrendTypes extends BaseJiraReportTypes {
  xaxis: boolean;
  hygiene_uri: string;
  hygiene_trend_uri: string;
  hygiene_types: string[];
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [WIDGET_VALIDATION_FUNCTION]: (data: any) => void;
  [COMPARE_X_AXIS_TIMESTAMP]: boolean;
  [LABEL_TO_TIMESTAMP]: boolean;
  [INCLUDE_INTERVAL_IN_PAYLOAD]: boolean;
}
