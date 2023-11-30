import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalInputFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalInputFilter";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { get } from "lodash";
import {
  ApiDropDownData,
  baseFilterConfig,
  DropDownData,
  LevelOpsFilter,
  LevelOpsFilterTypes
} from "model/filters/levelopsFilters";
import { STACK_OPTIONS } from "./constants";

export const praetorianCommonFiltersConfig: LevelOpsFilter[] = [
  baseFilterConfig("priority", {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: "Priority",
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    supportPaginatedSelect: true,
    apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
    isSelected: (args: any) => !!get(args, ["filters", "priority"]) || !!get(args, ["filters", "priorities"]),
    getMappedValue: (args: any) => get(args, ["allFilters", "priority"]) ?? get(args, ["allFilters", "priorities"]),
    filterMetaData: {
      selectMode: "multiple",
      uri: "praetorian_issues_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["priority"],
          filter: { integration_ids: get(args, "integrationIds", []) }
        };
      },
      options: (args: any) => {
        const data = extractFilterAPIData(args, "priority");
        return data
          ?.map((item: any) => ({
            label: item.additional_key ?? item.key,
            value: item.key
          }))
          .filter((item: { label: string; value: string }) => !!item.value);
      },
      specialKey: "priority",
      sortOptions: true,
      createOption: true
    } as ApiDropDownData
  }),

  baseFilterConfig("category", {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: "Category",
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    supportPaginatedSelect: true,
    apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
    filterMetaData: {
      selectMode: "multiple",
      uri: "praetorian_issues_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["category"],
          filter: { integration_ids: get(args, "integrationIds", []) }
        };
      },
      options: (args: any) => {
        const data = extractFilterAPIData(args, "category");
        return data
          ?.map((item: any) => ({
            label: item.additional_key ?? item.key,
            value: item.key
          }))
          .filter((item: { label: string; value: string }) => !!item.value);
      },
      specialKey: "category",
      sortOptions: true,
      createOption: true
    } as ApiDropDownData
  })
];

export const StackFilterConfig: LevelOpsFilter = {
  id: "stacks",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stacks",
  beKey: "stacks",
  labelCase: "title_case",
  filterMetaData: {
    clearSupport: true,
    options: STACK_OPTIONS,
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};

export const IngestedInFilterConfig: LevelOpsFilter = {
  id: "praetorian_ingested_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Ingested In",
  beKey: "ingested_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const LastReportFilterConfig: LevelOpsFilter = {
  id: "n_last_reports",
  renderComponent: UniversalInputFilterWrapper,
  label: "Last Reports",
  beKey: "n_last_reports",
  labelCase: "title_case",
  defaultValue: 1,
  filterMetaData: {
    type: "number"
  },
  deleteSupport: true,
  apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
