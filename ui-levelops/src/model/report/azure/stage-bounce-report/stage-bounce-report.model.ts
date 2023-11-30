import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { INFO_MESSAGES, PREV_REPORT_TRANSFORMER, STACKS_FILTER_STATUS } from "dashboard/constants/applications/names";
import { ALLOWED_WIDGET_DATA_SORTING, WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { SHOW_AGGREGATIONS_TAB, STACKS_SHOW_TAB } from "dashboard/constants/filter-key.mapping";

export interface AzureStageBounceReportType extends BaseAzureReportTypes {
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  valuesToFilters: any;
  stack_filters: string[];
  [INFO_MESSAGES]: any;
  tooltipMapping: any;
  requiredFilters: string[];
  [WIDGET_VALIDATION_FUNCTION]: Function;
  getTotalKey: Function;
  [STACKS_FILTER_STATUS]: Function;
  xAxisLabelTransform: Function;
  onChartClickPayload: Function;
  shouldJsonParseXAxis: Function;
}
