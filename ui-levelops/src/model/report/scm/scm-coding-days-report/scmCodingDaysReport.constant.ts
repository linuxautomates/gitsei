import {
  ALLOWED_WIDGET_DATA_SORTING,
  VALUE_SORT_KEY,
  WIDGET_VALIDATION_FUNCTION
} from "dashboard/constants/filter-name.mapping";
import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface SCMCodingDaysReportType extends BaseSCMReportTypes {
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  [VALUE_SORT_KEY]: string;
  [WIDGET_VALIDATION_FUNCTION]: any;
}
