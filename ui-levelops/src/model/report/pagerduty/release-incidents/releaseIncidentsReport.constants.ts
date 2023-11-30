import { BasePagerdutyReportTypes } from "../basePagerdutyReports.constant";
import { supportedFilterType } from "../../../../dashboard/constants/supported-filters.constant";

export interface ReleaseIncidentReportType extends BasePagerdutyReportTypes {
  defaultFilters?: any;
  requiredFilters?: string[];
  across: string[];
  supported_filters: supportedFilterType[];
}
