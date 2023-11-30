import { BaseBullseyeReportTypes } from "../baseBullseyeReports.constants";

export interface FunctionCoverageTrendReportTypes extends BaseBullseyeReportTypes {
  tooltipMapping: { [key: string]: string };
}
