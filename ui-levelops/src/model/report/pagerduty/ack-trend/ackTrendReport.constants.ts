import { BasePagerdutyReportTypes } from "../basePagerdutyReports.constant";
import { supportedFilterType } from "dashboard/constants/supported-filters.constant";

export interface AckTrendReportType extends BasePagerdutyReportTypes {
  across: string[];
  supported_filters: supportedFilterType;
}
