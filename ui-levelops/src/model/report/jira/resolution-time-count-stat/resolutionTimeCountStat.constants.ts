import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { BaseJiraReportTypes } from "../baseJiraReports.constants";
import { DEFAULT_METADATA, STAT_TIME_BASED_FILTER, PARTIAL_FILTER_KEY } from "dashboard/constants/filter-key.mapping";
import { FILTER_WITH_INFO_MAPPING, filterWithInfoType } from "dashboard/constants/filterWithInfo.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";

export interface ResolutionTimeSingleStatType extends BaseJiraReportTypes {
  xaxis: boolean;
  [DEFAULT_METADATA]: { [x: string]: any };
  [FILTER_WITH_INFO_MAPPING]: filterWithInfoType[];
  [FILTER_NAME_MAPPING]: { [x: string]: string };
  prev_report_transformer: (data: basicMappingType<any>) => void;
  hasStatUnit?: (compareField: string) => boolean;
}
