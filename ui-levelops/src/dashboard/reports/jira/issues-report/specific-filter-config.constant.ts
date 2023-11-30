import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import UniversalCheckboxFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalCheckboxFilter";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { getVisualizationOptions } from "dashboard/reports/helper";
import { JIRA_FILTER_KEY_MAPPING } from "dashboard/reports/jira/constant";
import { get, uniqBy } from "lodash";
import { CheckboxData, DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS, STACK_OPTIONS } from "./constants";
import { getAcrossValue } from "./helper";

export const VisualizationFilterConfig: LevelOpsFilter = {
  id: "visualization",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Visualization",
  beKey: "visualization",
  labelCase: "title_case",
  filterMetaData: {
    selectMode: "default",
    options: getVisualizationOptions,
    sortOptions: false
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const AcrossFilterConfig: LevelOpsFilter = {
  id: "across",
  renderComponent: UniversalSelectFilterWrapper,
  label: "X-Axis",
  beKey: "across",
  labelCase: "title_case",
  required: true,
  getMappedValue: getAcrossValue,
  filterMetaData: {
    selectMode: "default",
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
      const commonOptions = STACK_OPTIONS;
      const filterMetaData = get(args, ["filterMetaData"], {});
      const customFieldsData = get(filterMetaData, ["customFieldsRecords"], []);
      const customFieldsOptions = customFieldsData.map((item: any) => ({ label: item.name, value: item.key }));
      return uniqBy([...commonOptions, ...customFieldsOptions], "value");
    },
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};

export const MetricFilterConfig: LevelOpsFilter = {
  id: "metric",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Metric",
  beKey: "metric",
  labelCase: "upper_case",
  filterMetaData: {
    selectMode: "default",
    options: [
      { label: "Number of tickets", value: "ticket" },
      { label: "Sum of story points", value: "story_point" }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};

export const XAxisLabelFilterConfig: LevelOpsFilter = {
  id: "filter_across_values",
  renderComponent: UniversalCheckboxFilter,
  label: "X-Axis Labels",
  beKey: "filter_across_values",
  labelCase: "title_case",
  deleteSupport: true,
  defaultValue: true,
  disabled: (args: any) => {
    const { filters } = args;
    const across = getAcrossValue({ ...args, allFilters: args?.filters });
    return !Object.keys(filters || {}).includes(get(JIRA_FILTER_KEY_MAPPING, [across], across));
  },
  filterMetaData: {
    checkboxLabel: "Display only filtered values"
  } as CheckboxData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const JiraDueDateFilterConfig: LevelOpsFilter = {
  id: "issue_due_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Jira Due Date",
  beKey: "issue_due_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
