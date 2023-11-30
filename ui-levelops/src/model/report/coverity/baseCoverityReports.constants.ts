import { FE_BASED_FILTERS } from "dashboard/constants/applications/names";
import { BaseReportTypes } from "../baseReport.constant";
import { Dict } from "types/dict";

export interface BaseCoverityReportsType extends BaseReportTypes {
  supportExcludeFilters?: boolean;
  [PREV_REPORT_TRANSFORMER: string]: any;
  [FE_BASED_FILTERS]: Dict<string, any>;
}
