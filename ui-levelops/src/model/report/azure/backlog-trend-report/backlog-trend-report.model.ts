import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING } from "../../../../dashboard/constants/filter-name.mapping";
import { STACKS_SHOW_TAB } from "../../../../dashboard/constants/filter-key.mapping";
import { MULTI_SERIES_REPORT_FILTERS_CONFIG, PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";

export interface AzureBacklogTrendReportType extends BaseAzureReportTypes {
  stack_filters: Record<string, any>;
  valuesToFilters: Record<string, any>;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  xAxisLabelTransform: (params: any) => any;
  onChartClickPayload: (params: any) => any;
  shouldReverseApiData: () => boolean;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [MULTI_SERIES_REPORT_FILTERS_CONFIG]: any;
}
