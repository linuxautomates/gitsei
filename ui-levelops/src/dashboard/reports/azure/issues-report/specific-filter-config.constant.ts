import { AZURE_FILTER_KEY_MAPPING, WORKITEM_PARENT_KEY, WORKITEM_PARENT_TYPE_KEY } from "./../constant";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import UniversalCheckboxFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalCheckboxFilter";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get, uniqBy } from "lodash";
import { CheckboxData, DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS, STACK_OPTIONS } from "./constant";
import { getAcrossValue } from "./helper";
import { removeCustomPrefix } from "../common-helper";

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
      const customFieldsOptions = customFieldsData.map((item: any) => ({
        label: item.name,
        value: removeCustomPrefix(item)
      }));
      return uniqBy([...commonOptions, ...customFieldsOptions], "value");
    },
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};

export const AcrossFilterConfig: LevelOpsFilter = {
  id: "across",
  renderComponent: UniversalSelectFilterWrapper,
  label: "X-Axis",
  beKey: "across",
  labelCase: "title_case",
  required: true,
  filterMetaData: {
    selectMode: "default",
    options: (args: any) => {
      const commonOptions = ACROSS_OPTIONS;
      const filterMetaData = get(args, ["filterMetaData"], {});
      const customFieldsData = get(filterMetaData, ["customFieldsRecords"], []);
      const customFieldsOptions = customFieldsData.map((item: any) => ({
        label: item.name,
        value: removeCustomPrefix(item)
      }));
      return uniqBy([...commonOptions, ...customFieldsOptions], "value");
    },
    sortOptions: true
  } as DropDownData,
  getMappedValue: (data: any) => {
    const { allFilters } = data;
    if (allFilters?.across && allFilters?.interval) {
      return `${allFilters?.across}_${allFilters?.interval}`;
    }
    return undefined;
  },
  modifyFilterValue: async (args: any) => {
    const { value, beKey, onAggregationAcrossSelection } = args;
    let appendDeleteQuery = {};
    if (beKey === "across" && value === WORKITEM_PARENT_KEY) {
      appendDeleteQuery = {
        appendQuery: {
          [WORKITEM_PARENT_TYPE_KEY]: ["Feature"]
        }
      };
    }
    if (beKey === "across" && value !== WORKITEM_PARENT_KEY) {
      appendDeleteQuery = {
        deleteQuery: [WORKITEM_PARENT_TYPE_KEY]
      };
    }
    onAggregationAcrossSelection(value, appendDeleteQuery);
  },
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};

export const XAxisLabelFilterConfig: LevelOpsFilter = {
  id: "filter_across_values",
  renderComponent: UniversalCheckboxFilter,
  label: "X-Axis Labels",
  beKey: "filter_across_values",
  labelCase: "title_case",
  defaultValue: true,
  deleteSupport: true,
  disabled: (args: any) => {
    const { filters } = args;
    const across = getAcrossValue({ ...args, allFilters: args?.filters });
    return !Object.keys(filters || {}).includes(get(AZURE_FILTER_KEY_MAPPING, [across], across));
  },
  filterMetaData: {
    checkboxLabel: "Display only filtered values"
  } as CheckboxData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
