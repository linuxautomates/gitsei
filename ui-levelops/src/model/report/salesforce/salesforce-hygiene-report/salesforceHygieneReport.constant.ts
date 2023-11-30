import { PREVIEW_DISABLED } from "dashboard/constants/applications/names";
import { WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { BaseSalesforceReportType } from "../baseSalesforceReports.constant";

export interface SalesforceHygieneReportType extends BaseSalesforceReportType {
  hygiene_uri: string;
  hygiene_trend_uri: string;
  hygiene_types: Array<string>;
  [PREVIEW_DISABLED]: boolean;
  [WIDGET_VALIDATION_FUNCTION]: (payload: any) => boolean;
}
