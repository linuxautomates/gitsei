import { ZendeskStacksReportsKey } from "dashboard/constants/helper";
import { BaseZendeskReportsType } from "../baseZendeskReports.constant";

export interface ZendeskTicketsReportTypes extends BaseZendeskReportsType {
  getTotalKey: () => string;
  getSortKey: (params: any) => string | undefined;
  getSortOrder: (params: any) => string | undefined;
  [ZendeskStacksReportsKey.ZENDESK_STACKED_KEY]: boolean;
  xAxisLabelTransform: (args: any) => string | undefined;
}
