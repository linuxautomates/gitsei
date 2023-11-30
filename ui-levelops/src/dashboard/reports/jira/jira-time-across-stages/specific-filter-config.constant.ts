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

export const StatusFilterConfig: LevelOpsFilter = baseFilterConfig("statuses", {
  renderComponent: UniversalSelectFilterWrapper,
  apiContainer: APIFilterContainer,
  label: "Current Status",
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  type: LevelOpsFilterTypes.DROPDOWN,
  labelCase: "none",
  deleteSupport: true,
  partialSupport: true,
  excludeSupport: true,
  partialKey: "status",
  supportPaginatedSelect: true,
  apiFilterProps: (args: any) => {
    const partialValue = get(
      args.allFilters,
      [args?.partialFilterKey ?? "partial_match", args?.partialKey ?? args?.beKey],
      {}
    );
    const switchValue = !!get(args?.allFilters || {}, ["exclude", args?.excludeKey ?? args?.beKey], undefined);
    const selectPropOptions = [
      {
        label: "Start With",
        value: STARTS_WITH
      },
      {
        label: "Contain",
        value: CONTAINS
      }
    ];
    const withSwitch: WithSwitchProps = {
      showSwitch: true,
      showSwitchText: true,
      switchText: "Exclude",
      switchValue,
      onSwitchValueChange: (value: any) => args.handleSwitchValueChange(args?.excludeKey ?? args?.beKey, value)
    };
    const switchWithDropdown: switchWithDropdownProps = {
      showSwitchWithDropdown: true,
      checkboxProps: {
        text: "Include all values that",
        disabled: switchValue,
        value: Object.keys(partialValue).length > 0
      },
      selectProps: {
        options: selectPropOptions,
        value: Object.keys(partialValue).length > 0 ? Object.keys(partialValue)[0] : selectPropOptions[0].value,
        disabled: switchValue || !(Object.keys(partialValue).length > 0),
        onSelectChange: (key: any) =>
          args?.handlePartialValueChange?.(
            args?.partialKey ?? args?.beKey,
            key ? { [key]: Object.values(partialValue)[0] || "" } : undefined
          )
      }
    };
    const withDelete: withDeleteProps = {
      showDelete: args?.deleteSupport,
      key: args?.beKey,
      onDelete: args.handleRemoveFilter
    };
    return { withSwitch, switchWithDropdown, withDelete };
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
    specialKey: "status",
    sortOptions: true,
    createOption: true
  } as ApiDropDownData
});

export const MetricFilterConfig: LevelOpsFilter = {
  id: "metric",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Metric",
  beKey: "metric",
  labelCase: "title_case",
  filterMetaData: {
    options: [
      { value: "median_time", label: "Median Time In Status" },
      { value: "average_time", label: "Average Time In Status" }
    ],
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
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};

export const HideStatusFilterConfig: LevelOpsFilter = {
  id: "stages",
  renderComponent: UniversalSelectFilterWrapper,
  apiContainer: APIFilterContainer,
  label: "Hide Status",
  beKey: "stages",
  labelCase: "title_case",
  filterInfo:
    "Hide selected status values from the graph. It is recommended to hide terminal statuses like Done, Won't Do.",
  filterMetaData: {
    alwaysExclude: true,
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
      const filterMetaData = args?.filterMetaData;
      const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
      const jiraStatuses = filterApiData
        ? filterApiData.filter((item: any) => Object.keys(item)[0] === "status")[0]
        : [];

      return (jiraStatuses?.["status"] || []).map((item: any) => ({ label: item.key, value: item.key }));
    },
    specialKey: "status",
    sortOptions: true
  } as ApiDropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
