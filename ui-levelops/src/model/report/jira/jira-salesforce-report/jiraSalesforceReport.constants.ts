import { REPORT_FILTERS_CONFIG } from "./../../../../dashboard/constants/applications/names";
import { HIDE_REPORT, SHOW_SETTINGS_TAB } from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { Dict } from "types/dict";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "dashboard/helpers/helper";
import { supportedFilterType } from "dashboard/constants/supported-filters.constant";
import { SupportDrillDownType } from "dashboard/constants/drilldown.constants";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export interface JiraSalesforceReportTypes {
  xaxis: boolean;
  [FILTER_NAME_MAPPING]: Dict<string, string>;
  [SHOW_SETTINGS_TAB]: boolean;
  [HIDE_REPORT]?: boolean;
  name: string;
  application: string;
  chart_type: ChartType;
  chart_container: ChartContainerType;
  method: string;
  supported_filters: supportedFilterType;
  drilldown: SupportDrillDownType;
  uri: string;
  chart_props?: basicMappingType<any>;
  [REPORT_FILTERS_CONFIG]: LevelOpsFilter[];
}
