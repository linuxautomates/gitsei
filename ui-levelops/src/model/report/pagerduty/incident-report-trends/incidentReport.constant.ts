import { BasePagerdutyReportTypes } from "../basePagerdutyReports.constant";
import { supportedFilterType } from "dashboard/constants/supported-filters.constant";

export interface IncidentReportTrendsType extends BasePagerdutyReportTypes {
  requiredFilters?: String[];
  singleSelectFilters?: String[];
  supported_filters: supportedFilterType;
}
