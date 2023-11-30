import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import {
  widgetDataSortingOptions,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { ISSUE_MANAGEMENT_REPORTS, JIRA_MANAGEMENT_TICKET_REPORT } from "dashboard/constants/applications/names";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { allTimeFilterKeys } from "dashboard/graph-filters/components/helper";
import { get, isArray } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

export const SortXAxisFilterConfig: LevelOpsFilter = {
  id: "sort_xaxis",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Sort X-Axis",
  beKey: "sort_xaxis",
  labelCase: "title_case",
  filterMetaData: {
    selectMode: "default",
    options: (args: any) => {
      const across = args?.allFilters?.across;
      const metric = get(args, ["allFilters", "metric"], []);
      if (across.toLowerCase() === "sprint") {
        return widgetDataSortingOptions[widgetDataSortingOptionsNodeType.SPRINT_TIME_BASED];
      }
      const timeFilterKeys = allTimeFilterKeys.concat(args?.customTimeFilterKeys || []);
      if (timeFilterKeys.includes(across)) {
        return widgetDataSortingOptions[widgetDataSortingOptionsNodeType.TIME_BASED];
      }
      if (
        [
          ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
          JIRA_MANAGEMENT_TICKET_REPORT.RESOLUTION_TIME_REPORT
        ].includes(args?.filterMetaData?.reportType) &&
        metric &&
        isArray(metric) &&
        metric.length > 1
      ) {
        return widgetDataSortingOptions[widgetDataSortingOptionsNodeType.NON_TIME_METRIC_BASED];
      }
      return widgetDataSortingOptions[widgetDataSortingOptionsNodeType.NON_TIME_BASED];
    }
  },
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
