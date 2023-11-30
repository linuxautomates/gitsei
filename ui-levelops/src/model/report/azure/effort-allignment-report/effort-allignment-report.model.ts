import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { PREVIEW_DISABLED, PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import {
  DefaultKeyTypes,
  DISABLE_XAXIS,
  REPORT_CSV_DOWNLOAD_CONFIG,
  REQUIRED_FILTERS_MAPPING,
  STORE_ACTION
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { DEFAULT_METADATA } from "dashboard/constants/filter-key.mapping";
import { WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";

export interface AzureEffortAlignmentReportType extends BaseAzureReportTypes {
  [PREV_REPORT_TRANSFORMER]: (data: any) => void;
  [WIDGET_MIN_HEIGHT]: string;
  requiredFilters: any;
  [REQUIRED_FILTERS_MAPPING]: any;
  [DEFAULT_METADATA]: any;
  [STORE_ACTION]: any;
  [PREVIEW_DISABLED]: boolean;
  [DISABLE_XAXIS]: boolean;
  [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: boolean;
  [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: string;
  [REPORT_CSV_DOWNLOAD_CONFIG]: any;
  [WIDGET_VALIDATION_FUNCTION]: (payload: any) => any;
}
