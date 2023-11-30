import { BaseZendeskReportsType } from "../baseZendeskReports.constant";

export interface ZendeskHygieneTrendReportTypes extends BaseZendeskReportsType {
  hygiene_uri: string;
  hygiene_trend_uri: string;
  hygiene_types: string[];
  widget_validation_function: (payload: any) => boolean;
}
