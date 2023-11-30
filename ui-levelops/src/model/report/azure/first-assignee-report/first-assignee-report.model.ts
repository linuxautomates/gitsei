import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING, VALUE_SORT_KEY } from "../../../../dashboard/constants/filter-name.mapping";
import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";

export interface AzureFirstAssigneeReportType extends BaseAzureReportTypes {
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  [VALUE_SORT_KEY]: string;
  valuesToFilters: Record<string, any>;
  xAxisLabelTransform: (params: any) => any;
  onChartClickPayload: (params: any) => any;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  showExtraInfoOnToolTip: string[];
}
