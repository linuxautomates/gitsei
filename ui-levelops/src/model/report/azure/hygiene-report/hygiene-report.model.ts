import { PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { BaseAzureReportTypes } from "../baseAzureReports.constants";

export interface AzureIssueHygieneReportType extends BaseAzureReportTypes {
  hygiene_uri: string;
  widget_validation_function: (payload: any) => boolean;
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
}
