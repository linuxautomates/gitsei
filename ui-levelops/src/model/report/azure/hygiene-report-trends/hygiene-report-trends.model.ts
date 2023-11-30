import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { WIDGET_VALIDATION_FUNCTION } from "../../../../dashboard/constants/filter-name.mapping";
import {
  COMPARE_X_AXIS_TIMESTAMP,
  INCLUDE_INTERVAL_IN_PAYLOAD,
  LABEL_TO_TIMESTAMP,
  PREV_REPORT_TRANSFORMER
} from "../../../../dashboard/constants/applications/names";

export interface HygieneReportTrendsType extends BaseAzureReportTypes {
  hygiene_trend_uri: string;
  [LABEL_TO_TIMESTAMP]: boolean;
  [COMPARE_X_AXIS_TIMESTAMP]: boolean;
  [INCLUDE_INTERVAL_IN_PAYLOAD]: boolean;
  [WIDGET_VALIDATION_FUNCTION]: (payload: any) => boolean;
  onChartClickPayload: (params: { [key: string]: any }) => any;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
}
