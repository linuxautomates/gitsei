import { BaseBullseyeReportTypes } from "../baseBullseyeReports.constants";

export interface BranchCoverageTrendReportTypes extends BaseBullseyeReportTypes {
  tooltipMapping: { [key: string]: string };
}
