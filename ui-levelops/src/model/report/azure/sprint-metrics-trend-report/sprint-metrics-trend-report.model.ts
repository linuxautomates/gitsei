import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import {
  BAR_CHART_REF_LINE_STROKE,
  HIDE_REPORT,
  IGNORE_FILTER_KEYS_CONFIG,
  SHOW_AGGREGATIONS_TAB,
  SHOW_FILTERS_TAB,
  SHOW_SETTINGS_TAB,
  SHOW_WEIGHTS_TAB
} from "dashboard/constants/filter-key.mapping";
import { BaseAzureReportTypes } from "../baseAzureReports.constants";

export interface AzureSprintMetricsTrendReportType extends BaseAzureReportTypes {
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  columnWithInformation: boolean;
  columnsWithInfo: any;
  [IGNORE_FILTER_KEYS_CONFIG]: any;
  [HIDE_REPORT]: boolean;
  show_max: boolean;
  compareField: string;
  [BAR_CHART_REF_LINE_STROKE]: string;
}
