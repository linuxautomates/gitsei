import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import {
  CSV_DRILLDOWN_TRANSFORMER,
  HIDE_REPORT,
  IGNORE_FILTER_KEYS_CONFIG,
  SHOW_SETTINGS_TAB
} from "dashboard/constants/filter-key.mapping";
import { BaseAzureReportTypes } from "../baseAzureReports.constants";

export interface AzureSprintMetricsSingleStatReportType extends BaseAzureReportTypes {
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [CSV_DRILLDOWN_TRANSFORMER]: any;
  columnWithInformation: boolean;
  columnsWithInfo: any;
  [IGNORE_FILTER_KEYS_CONFIG]: any;
  supported_widget_types: string[];
  [HIDE_REPORT]: boolean;
}
