import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import {
  CSV_DRILLDOWN_TRANSFORMER,
  SHOW_AGGREGATIONS_TAB,
  SHOW_METRICS_TAB,
  SHOW_SETTINGS_TAB
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { Dict } from "types/dict";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { REPORT_KEY_IS_ENABLED } from "dashboard/reports/constants";

export interface LeadTimeTrendReportTypes extends BaseJiraReportTypes {
  [REPORT_KEY_IS_ENABLED]: boolean;
  xaxis: boolean;
  shouldJsonParseXAxis: () => boolean;
  [CSV_DRILLDOWN_TRANSFORMER]: (data: any) => any;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [FILTER_WITH_INFO_MAPPING]: any;
  [SHOW_SETTINGS_TAB]: boolean;
  [WIDGET_MIN_HEIGHT]?: string;
  [SHOW_METRICS_TAB]: boolean;
  [SHOW_AGGREGATIONS_TAB]: boolean;
  valuesToFilters: Dict<string, string>;
}
