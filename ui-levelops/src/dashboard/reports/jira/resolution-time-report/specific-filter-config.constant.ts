import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { CONTAINS, STARTS_WITH } from "dashboard/constants/constants";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get, uniqBy } from "lodash";
import {
  ApiDropDownData,
  baseFilterConfig,
  DropDownData,
  LevelOpsFilter,
  LevelOpsFilterTypes
} from "model/filters/levelopsFilters";
import {
  switchWithDropdownProps,
  withDeleteProps,
  WithSwitchProps
} from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";
import { ACROSS_OPTIONS } from "./constants";
import { widgetDataSortingOptionKeys } from "dashboard/constants/WidgetDataSortingFilter.constant";

export const MetricFilterConfig: LevelOpsFilter = {
  id: "metric",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Metric",
  beKey: "metric",
  labelCase: "title_case",
  defaultValue: ["median_resolution_time", "number_of_tickets_closed"],
  filterMetaData: {
    selectMode: "multiple",
    selectModeFunction: (args: any) => {
      const sortXaxis = get(args, ["filters", "sort_xaxis"]);
      if([widgetDataSortingOptionKeys.VALUE_LOW_HIGH,widgetDataSortingOptionKeys.VALUE_HIGH_LOW].includes(sortXaxis)){
        return "default"
      }
      return "multiple";
    },
    options: [
      { value: "median_resolution_time", label: "Median Resolution Time" },
      { value: "number_of_tickets_closed", label: "Number Of Tickets" },
      { value: "90th_percentile_resolution_time", label: "90th Percentile Resolution Time" },
      { value: "average_resolution_time", label: "Average Resolution Time" }
    ],
    mapFilterValueForBE: (value: any) => typeof value === 'string' ? [value] : value,
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
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
      const customFieldsOptions = customFieldsData.map((item: any) => ({ label: item.name, value: item.key }));
      return uniqBy([...commonOptions, ...customFieldsOptions], "value");
    },
    sortOptions: true
  } as DropDownData,
  getMappedValue: (data: any) => {
    const { allFilters } = data;
    if (allFilters?.across === "issue_resolved" && allFilters?.interval) {
      return `${allFilters?.across}_${allFilters?.interval}`;
    }
    return undefined;
  },
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};

export const StatusFilterConfig: LevelOpsFilter = baseFilterConfig("statuses", {
  renderComponent: UniversalSelectFilterWrapper,
  label: "Current Status",
  apiContainer: APIFilterContainer,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  type: LevelOpsFilterTypes.DROPDOWN,
  labelCase: "none",
  deleteSupport: true,
  partialSupport: false,
  excludeSupport: false,
  partialKey: "status",
  supportPaginatedSelect: true,
  apiFilterProps: (args: any) => {
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withDelete };
  },
  filterMetaData: {
    selectMode: "multiple",
    uri: "jira_filter_values",
    method: "list",
    payload: (args: Record<string, any>) => {
      return {
        integration_ids: get(args, "integrationIds", []),
        fields: ["status"],
        filter: { integration_ids: get(args, "integrationIds", []) }
      };
    },
    options: (args: any) => {
      const filterMetaData = get(args, ["filterMetaData"], {});
      const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
      const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === "status");
      if (currData) {
        return (Object.values(currData)[0] as Array<any>)?.map((item: any) => ({
          label: item.value ?? item.key,
          value: item.key
        }));
      }
      return [];
    },
    sortOptions: true,
    createOption: true,
    specialKey: "status"
  } as ApiDropDownData
});
