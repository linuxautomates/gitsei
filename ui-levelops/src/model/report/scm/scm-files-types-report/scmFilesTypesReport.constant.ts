import { FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS } from "dashboard/constants/filter-key.mapping";
import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMFilesTypesReportType extends BaseSCMReportTypes {
  [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: Array<string>;
}
