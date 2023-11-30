import { SCM_PRS_TIME_FILTERS_KEYS } from "constants/filters";
import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ACROSS_OPTIONS } from "./constant";
import { get, set, uniqBy, unset } from "lodash";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";

export const scmPrsReportChartClickPayload = (params: any) => {
  const { data, across } = params;
  if (params?.chart_type === ChartType.CIRCLE) {
    if (across && SCM_PRS_TIME_FILTERS_KEYS.includes(across)) {
      const key = data?.payload?.name;
      return key;
    } else {
      const name = data?.name ?? data?.additional_key ?? data.key;
      return { name: name, id: data.key };
    }
  }

  const newData = params?.data?.activePayload?.[0]?.payload;
  if (across && SCM_PRS_TIME_FILTERS_KEYS.includes(across)) {
    return newData?.name;
  }
  const name = newData?.name ?? newData?.key;
  const id = newData?.key ?? newData?.name;
  return { name, id };
};

export const PR_REPORT_QUERY = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED],
  visualization: IssueVisualizationTypes.BAR_CHART
};

export const StackFilterConfig: LevelOpsFilter = {
  id: "stacks",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stacks",
  beKey: "stacks",
  labelCase: "title_case",
  disabled: ({ filters }) => {
    return (
      filters &&
      filters.visualization &&
      [IssueVisualizationTypes.DONUT_CHART, IssueVisualizationTypes.PIE_CHART].includes(filters.visualization)
    );
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

export const transformSCMPRsReportPrevQuery = (widget: any) => {
  const { query, metadata } = widget;
  const { stacks } = query;
  const { visualization } = metadata;

  let visualizationVal = visualization;

  if (!visualizationVal) {
    // If value is not set, assign it to default value to make it backward compatible.
    visualizationVal = IssueVisualizationTypes.BAR_CHART;
    set(widget, ["metadata", "visualization"], visualizationVal);
  }

  if (visualizationVal === IssueVisualizationTypes.PIE_CHART && stacks?.length > 0) {
    unset(widget, ["query", "stacks"]);
  }
  return widget;
};
