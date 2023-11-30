import { optionType } from "dashboard/dashboard-types/common-types";
import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface ScmPrsSingleStatReportType extends BaseSCMReportTypes {
  compareField: string;
}
