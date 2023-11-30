import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { widgetDataSortingOptionKeys } from "dashboard/constants/WidgetDataSortingFilter.constant";
import { ISSUE_MANAGEMENT_REPORTS, JIRA_MANAGEMENT_TICKET_REPORT } from "dashboard/constants/applications/names";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get } from "lodash";
import { DropDownData, LevelOpsFilter, SelectModeType } from "model/filters/levelopsFilters";

// This is a base config don't use this , use generator function
const BaseMetricFilterConfig: LevelOpsFilter = {
  id: "metrics",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Metric",
  beKey: "metrics",
  labelCase: "title_case",
  filterMetaData: {
    selectMode: "default",
    options: [{}],
    sortOptions: false
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};

export const generateMetricFilterConfig = (
  options: Array<{ label: string; value: string | number }> | ((args: any) => Array<{ label: string; value: string }>),
  selectMode?: SelectModeType,
  defaultValue?: any,
  beKey?: string,
  label?: string,
  updateInWidgetMetadata?: boolean
): LevelOpsFilter => ({
  ...BaseMetricFilterConfig,
  defaultValue,
  beKey: beKey ?? "metrics",
  label: label ?? "Metric",
  updateInWidgetMetadata: !!updateInWidgetMetadata,
  filterMetaData: {
    ...BaseMetricFilterConfig.filterMetaData,
    options,
    selectMode,
    selectModeFunction: (args: any) => {
      const sortXaxis = get(args, ["filters", "sort_xaxis"]);
      if (
        [
          ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
          JIRA_MANAGEMENT_TICKET_REPORT.RESOLUTION_TIME_REPORT
        ].includes(args?.report) &&
        [widgetDataSortingOptionKeys.VALUE_LOW_HIGH, widgetDataSortingOptionKeys.VALUE_HIGH_LOW].includes(sortXaxis)
      ) {
        return "default";
      }
      return selectMode;
    },
    mapFilterValueForBE: (value: any) => (selectMode === "multiple" && typeof value === "string" ? [value] : value)
  } as DropDownData
});
