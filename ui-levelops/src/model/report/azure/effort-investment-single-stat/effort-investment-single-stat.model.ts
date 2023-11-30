import { BaseAzureReportTypes } from "../baseAzureReports.constants";
import { DEFAULT_METADATA } from "../../../../dashboard/constants/filter-key.mapping";
import {
  DefaultKeyTypes,
  REQUIRED_FILTERS_MAPPING,
  STORE_ACTION
} from "../../../../dashboard/constants/bussiness-alignment-applications/constants";
import { HIDE_CUSTOM_FIELDS, PREV_REPORT_TRANSFORMER } from "../../../../dashboard/constants/applications/names";
import { WIDGET_VALIDATION_FUNCTION } from "../../../../dashboard/constants/filter-name.mapping";

export interface AzureEffortInvestmentSingleStatType extends BaseAzureReportTypes {
  supported_widget_types: string[];
  [DEFAULT_METADATA]: Record<string, any>;
  [REQUIRED_FILTERS_MAPPING]: Record<string, any>;
  [DefaultKeyTypes.DEFAULT_DISPLAY_FORMAT_KEY]: string;
  [DefaultKeyTypes.DEFAULT_EFFORT_UNIT]: string;
  [DefaultKeyTypes.DEFAULT_SCHEME_KEY]: boolean;
  [STORE_ACTION]: Function;
  [WIDGET_VALIDATION_FUNCTION]: Function;
  [HIDE_CUSTOM_FIELDS]: (arg: any) => boolean;
}
