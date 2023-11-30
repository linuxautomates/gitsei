import {
  CHART_DATA_TRANSFORMERS,
  HIDE_CUSTOM_FIELDS,
  PREVIEW_DISABLED,
  PREV_REPORT_TRANSFORMER
} from "dashboard/constants/applications/names";
import {
  DefaultKeyTypes,
  DISABLE_XAXIS,
  INTERVAL_OPTIONS,
  REPORT_CSV_DOWNLOAD_CONFIG,
  REQUIRED_FILTERS_MAPPING,
  STORE_ACTION,
  TIME_RANGE_DISPLAY_FORMAT_CONFIG
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { DEFAULT_METADATA } from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { BaseAzureReportTypes } from "../baseAzureReports.constants";

export interface AzureEffortInvestmentTrendReportType extends BaseAzureReportTypes {
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [FILTER_NAME_MAPPING]: any;
  [PREVIEW_DISABLED]: boolean;
  shouldJsonParseXAxis: any;
  [DEFAULT_METADATA]: any;
  [TIME_RANGE_DISPLAY_FORMAT_CONFIG]: any;
  show_max: boolean;
  onChartClickPayload: Function;
  [INTERVAL_OPTIONS]: any;
  [STORE_ACTION]: any;
  [REQUIRED_FILTERS_MAPPING]: any;
  requiredFilters: any;
  [DISABLE_XAXIS]: any;
  [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: boolean;
  [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: any;
  [CHART_DATA_TRANSFORMERS]: any;
  [REPORT_CSV_DOWNLOAD_CONFIG]: any;
  [WIDGET_VALIDATION_FUNCTION]: any;
  [HIDE_CUSTOM_FIELDS]: any;
}
