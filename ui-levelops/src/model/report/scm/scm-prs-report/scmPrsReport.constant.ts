import {
  ALLOW_KEY_FOR_STACKS,
  FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS,
  DEFAULT_METADATA
} from "dashboard/constants/filter-key.mapping";
import { BaseSCMReportTypes } from "../baseSCMReports.constant";

export interface ScmPrsReportType extends BaseSCMReportTypes {
  [FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS]: Array<string>;
  [DEFAULT_METADATA]: any;
  [ALLOW_KEY_FOR_STACKS]: boolean;
  weekStartsOnMonday: boolean;
  generateBarColors: (dataKey: string) => string;
}
