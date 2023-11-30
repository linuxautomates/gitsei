import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING, VALUE_SORT_KEY } from "../../../../dashboard/constants/filter-name.mapping";
import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { ALLOW_KEY_FOR_STACKS } from "dashboard/constants/filter-key.mapping";

export interface AzureResponseTimeReportType extends BaseAzureReportTypes {
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  valuesToFilters: any;
  showExtraInfoOnToolTip: string[];
  [VALUE_SORT_KEY]: string;
  xAxisLabelTransform: (param: any) => any;
  onChartClickPayload: (param: any) => any;
  [ALLOW_KEY_FOR_STACKS]: boolean;
}
