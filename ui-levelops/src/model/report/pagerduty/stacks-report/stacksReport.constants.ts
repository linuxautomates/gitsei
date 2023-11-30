import { BasePagerdutyReportTypes } from "../basePagerdutyReports.constant";
import { CustomDrillDownType } from "dashboard/constants/drilldown.constants";
import { supportedFilterType } from "dashboard/constants/supported-filters.constant";

export interface StacksReportType extends BasePagerdutyReportTypes {
  across: string[];
  drilldown: CustomDrillDownType;
  defaultStacks: string[];
  stack_filters: string[];
  supported_filters: supportedFilterType;
}
