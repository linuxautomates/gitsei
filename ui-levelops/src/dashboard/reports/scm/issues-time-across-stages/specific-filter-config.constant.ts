import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import ExcludeStatusAPIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/ExcludeStatusApiContainer";
import UniversalCheckboxFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalCheckboxFilter";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get } from "lodash";
import {
  LevelOpsFilter,
  DropDownData,
  ApiDropDownData,
  LevelOpsFilterTypes,
  CheckboxData
} from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const MetricFilterConfig: LevelOpsFilter = {
  id: "metric",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Metric",
  beKey: "metric",
  labelCase: "title_case",
  defaultValue: ["median_time"],
  filterMetaData: {
    selectMode: "default",
    options: [
      { value: "median_time", label: "Median Time In Status" },
      { value: "average_time", label: "Average Time In Status" }
    ],
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};

export const ExcludeStatusFiltersConfig: LevelOpsFilter = {
  id: "stages",
  renderComponent: UniversalSelectFilterWrapper,
  apiContainer: ExcludeStatusAPIFilterContainer,
  type: LevelOpsFilterTypes.API_DROPDOWN,
  label: "Exclude Status",
  beKey: "stages",
  labelCase: "title_case",
  filterInfo:
    "Exclude selected statuses from Cycle Time computation. It is recommended to exclude terminal states like Done and Won't Do.",
  filterMetaData: {
    alwaysExclude: true,
    uri: "jira_filter_values",
    method: "list",
    payload: (args: Record<string, any>) => {
      return {
        integration_ids: get(args, "integrationIds", []),
        fields: ["status"],
        filter: { integration_ids: get(args, "integrationIds", []), status_categories: ["Done", "DONE"] }
      };
    },
    selectMode: "multiple",
    specialKey: "exclude_status",
    options: (args: any) => {
      const data = args?.filterMetaData?.apiConfig?.status_data ?? [];
      const excludeStatusState = args?.filterMetaData?.apiConfig?.data ?? [];
      const excludeStatus = get(excludeStatusState, [0, "status"], []).map((item: any) => item.key);
      const statusObject = data
        ? data.filter(
            (item: { [filterType: string]: { [key: string]: string }[] }) => Object.keys(item)[0] === "status"
          )[0]
        : [];
      if (statusObject && Object.keys(statusObject).length > 0) {
        let list = statusObject.status;
        if (list) {
          list = list.filter((item: { [key: string]: string }) => !excludeStatus.includes(item.key));
        } else {
          list = [];
        }
        return (list || []).map((item: any) => ({ label: item.key, value: item.key }));
      }
      return [];
    },
    sortOptions: true
  } as ApiDropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const StackByHistoricalStatusFilterConfig: LevelOpsFilter = {
  id: "stack-by-historical-status",
  renderComponent: UniversalCheckboxFilter,
  label: "Stacks",
  beKey: "stacks",
  hideFilter: args => {
    const { filters } = args;
    return get(filters, ["across"]) === "column";
  },
  modifyFilterValue: args => {
    const { value, onFilterValueChange, beKey } = args;
    onFilterValueChange(value ? ["column"] : [], beKey);
  },
  getMappedValue: args => {
    const { allFilters } = args;
    return get(allFilters, ["stacks", "0"], undefined) === "column";
  },
  labelCase: "none",
  filterMetaData: {
    checkboxLabel: "Stack by time in historical status"
  } as CheckboxData,
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};
