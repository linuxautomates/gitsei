import { BaseBullseyeReportTypes } from "../baseBullseyeReports.constants";

export interface CodeCoverageTrendReportTypes extends BaseBullseyeReportTypes {
  tooltipMapping: { [key: string]: string };
}
