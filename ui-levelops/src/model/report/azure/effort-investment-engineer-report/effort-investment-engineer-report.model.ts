import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import {
  DefaultKeyTypes,
  DISABLE_XAXIS,
  REPORT_CSV_DOWNLOAD_CONFIG,
  REQUIRED_FILTERS_MAPPING,
  WIDGET_MIN_HEIGHT
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { DEFAULT_METADATA } from "dashboard/constants/filter-key.mapping";
import { HIDE_CUSTOM_FIELDS, PREVIEW_DISABLED, PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";

export interface AzureEffortInvestmentEngineerReportType extends BaseAzureReportTypes {
  requiredFilters: string[];
  [WIDGET_VALIDATION_FUNCTION]: Function;
  [DEFAULT_METADATA]: any;
  [WIDGET_MIN_HEIGHT]: string;
  [PREVIEW_DISABLED]: boolean;
  [REQUIRED_FILTERS_MAPPING]: any;
  [DISABLE_XAXIS]: boolean;
  [REPORT_CSV_DOWNLOAD_CONFIG]: any;
  [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: boolean;
  [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: string;
  [HIDE_CUSTOM_FIELDS]: Function;
}
