import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING, VALUE_SORT_KEY, WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE } from "../../../../dashboard/constants/filter-name.mapping";
import {
  IMPLICITY_INCLUDE_DRILLDOWN_FILTER,
  MULTI_SERIES_REPORT_FILTERS_CONFIG,
  PREV_REPORT_TRANSFORMER,
  TRANSFORM_LEGEND_DATAKEY
} from "dashboard/constants/applications/names";
import { FILTER_WITH_INFO_MAPPING } from "dashboard/constants/filterWithInfo.mapping";
import { FILTER_KEY_MAPPING } from "dashboard/constants/filter-key.mapping";

export interface AzureResolutionTimeReportType extends BaseAzureReportTypes {
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  valuesToFilters: Record<string, any>;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  tooltipMapping: any;
  dataKey: string[];
  xAxisLabelTransform: (param: any) => string;
  onChartClickPayload: (param: any) => any;
  weekStartsOnMonday: boolean;
  [VALUE_SORT_KEY]: string;
  [FILTER_WITH_INFO_MAPPING]: any;
  [FILTER_KEY_MAPPING]: any;
  [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: any;
  [MULTI_SERIES_REPORT_FILTERS_CONFIG]: any;
  [TRANSFORM_LEGEND_DATAKEY]?: any;
  [WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE]?: Function;
}
