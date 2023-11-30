import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { DEFAULT_METADATA, SHOW_SETTINGS_TAB, STAT_TIME_BASED_FILTER } from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { Dict } from "types/dict";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";

export interface TicketsCountsStatTypes extends BaseJiraReportTypes {
  xaxis: boolean;
  compareField: string;
  chart_click_enable: boolean;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  supported_widget_types: string[];
  [SHOW_SETTINGS_TAB]: boolean;
  [STAT_TIME_BASED_FILTER]: any;
  [WIDGET_VALIDATION_FUNCTION]: (data: any) => void;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [DEFAULT_METADATA]: any;
  mapFiltersForWidgetApi: (filter: any) => any;
}
