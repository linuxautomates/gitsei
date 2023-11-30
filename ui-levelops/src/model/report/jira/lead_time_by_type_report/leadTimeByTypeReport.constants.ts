import { PREVIEW_DISABLED, PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import {
  CSV_DRILLDOWN_TRANSFORMER,
  SHOW_SETTINGS_TAB,
  SHOW_METRICS_TAB,
  SHOW_AGGREGATIONS_TAB
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { Dict } from "types/dict";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";

export interface LeadTimeByTypeReportTypes extends BaseJiraReportTypes {
  xaxis?: boolean; // TODO: make it mandatory later, Out of scope for initial release
  shouldJsonParseXAxis: () => boolean;
  [CSV_DRILLDOWN_TRANSFORMER]: (data: any) => any;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [FILTER_WITH_INFO_MAPPING]: any;
  [SHOW_SETTINGS_TAB]: boolean;
  [SHOW_METRICS_TAB]: boolean;
  [SHOW_AGGREGATIONS_TAB]: boolean;
  [PREVIEW_DISABLED]: boolean;
  valuesToFilters: Dict<string, string>;
}
