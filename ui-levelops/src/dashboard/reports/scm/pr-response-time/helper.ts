import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { widgetDataSortingOptionsDefaultValue, widgetDataSortingOptionsNodeType } from "dashboard/constants/WidgetDataSortingFilter.constant";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get, uniqBy } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ACROSS_OPTIONS } from "./constant";

export const scmPrsResponseTimeReportChartProps = (params: any) => {
  const { data, across, chart_type, visualization } = params;
  const chart = chart_type ?? visualization;
  if ([ChartType.BAR, ChartType.LINE, ChartType.AREA].includes(chart)) {
    const _data = data?.activePayload?.[0]?.payload || {};
    if (["author", "committer", "reviewer"].includes(across)) {
      return {
        name: data.activeLabel || "",
        id: _data.key || data.activeLabel
      };
    }
    return data.activeLabel || "";
  } else {
    if (["author", "committer", "reviewer"].includes(across)) {
      return {
        name: data.name || data.key || "",
        id: data.key || data.name || ""
      };
    }
    return data.name || "";
  }
};

export const PR_RESPONSE_TIME_REPORT_QUERY = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
};

export const StackFilterConfig: LevelOpsFilter = {
  id: "stacks",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stacks",
  beKey: "stacks",
  labelCase: "title_case",
  disabled: ({ filters }) => {
    return filters && filters.visualization && filters.visualization === IssueVisualizationTypes.DONUT_CHART;
  },
  filterMetaData: {
    clearSupport: true,
    options: (args: any) => {
      const commonOptions = ACROSS_OPTIONS;
      const filterMetaData = get(args, ["filterMetaData"], {});
      const customFieldsData = get(filterMetaData, ["customFieldsRecords"], []);
      const customFieldsOptions = customFieldsData.map((item: any) => ({ label: item.name, value: item.key }));
      return uniqBy([...commonOptions, ...customFieldsOptions], "value");
    },
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};