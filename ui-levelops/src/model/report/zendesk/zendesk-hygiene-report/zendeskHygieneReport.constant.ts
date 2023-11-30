import { BaseZendeskReportsType } from "../baseZendeskReports.constant";

export interface ZendeskHygieneReportTypes extends BaseZendeskReportsType {
  hygiene_uri: string;
  hygiene_trend_uri: string;
  hygiene_types: string[];
  preview_disabled: boolean;
  widget_validation_function: (payload: any) => boolean;
}
