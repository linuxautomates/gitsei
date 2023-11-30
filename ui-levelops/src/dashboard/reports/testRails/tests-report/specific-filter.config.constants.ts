import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { getAcrossValue } from "dashboard/reports/jira/issues-report/helper";
import { get, uniqBy } from "lodash";
import { LevelOpsFilter, DropDownData } from "model/filters/levelopsFilters";
import { TESTRAILS_ACROSS_OPTIONS, TESTRAILS_STACK_FILTERS_OPTIONS } from "../commonTestRailsReports.constants";
import { allowedFilterInTestCaseCountMetric } from "dashboard/report-filters/testrails/common-filters.config";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";

export const StackFilterConfig: LevelOpsFilter = {
  id: "stacks",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stacks",
  beKey: "stacks",
  labelCase: "title_case",
  disabled: ({ filters }) => {
    return filters && filters.visualization && [IssueVisualizationTypes.PIE_CHART].includes(filters.visualization);
  },
  filterMetaData: {
    clearSupport: true,
    options: (args: any) => {
      let commonOptions = TESTRAILS_STACK_FILTERS_OPTIONS;
      const metric = get(args, ["allFilters", "metric"], "");
      if (metric === "test_case_count") {
        commonOptions = commonOptions.filter(data => allowedFilterInTestCaseCountMetric.includes(data.value));
      }
      const filterMetaData = get(args, ["filterMetaData"], {});
      const customFieldsData = get(filterMetaData, ["customFieldsRecords"], []);
      const customFieldsOptions = customFieldsData
        .filter((item: any) => item?.type !== "MULTI_SELECT")
        .map((item: any) => ({ label: item.name, value: item.key }));
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
  getMappedValue: getAcrossValue,
  filterMetaData: {
    selectMode: "default",
    options: (args: any) => {
      let commonOptions = TESTRAILS_ACROSS_OPTIONS;
      const metric = get(args, ["allFilters", "metric"], "");
      if (metric === "test_case_count") {
        commonOptions = commonOptions.filter(data => allowedFilterInTestCaseCountMetric.includes(data.value));
      }
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
  labelCase: "title_case",
  filterMetaData: {
    selectMode: "default",
    options: [
      { label: "Number of test cases", value: "test_case_count" },
      { label: "Number of test executions", value: "test_count" }
    ],
    sortOptions: true
  } as DropDownData,
  defaultValue: "test_case_count",
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};
