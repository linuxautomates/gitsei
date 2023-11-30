import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  VALUE_SORT_KEY
} from "dashboard/constants/filter-name.mapping";
import { BaseReportTypes } from "../../baseReport.constant";
import { FE_BASED_FILTERS } from "../../../../dashboard/constants/applications/names";

export interface TimeToResolveReportType extends BaseReportTypes {
  xaxis: boolean;
  across: string[];
  stack_filters: string[];
  acknowledgeUri: string;
  appendAcrossOptions: { label: string; value: string }[];
  [FILTER_NAME_MAPPING]: { [key: string]: string };
  [ALLOWED_WIDGET_DATA_SORTING]: boolean;
  [VALUE_SORT_KEY]: string;
  defaultSort: any[];
  tooltipMapping: { [key: string]: string };
  valuesToFilters: { [key: string]: string };
  xAxisLabelTransform: (params: any) => void;
  onChartClickPayload: (params: any) => void;
  [FE_BASED_FILTERS]: any;
}
