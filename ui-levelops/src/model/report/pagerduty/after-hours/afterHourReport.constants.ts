import { BasePagerdutyReportTypes } from "../basePagerdutyReports.constant";
import { supportedFilterType } from "dashboard/constants/supported-filters.constant";

export interface AfterHoursReportType extends BasePagerdutyReportTypes {
  supported_filters: supportedFilterType;
}
