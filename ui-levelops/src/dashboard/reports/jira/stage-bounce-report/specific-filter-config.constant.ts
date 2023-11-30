import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import genericApiFilterProps from "dashboard/report-filters/common/common-api-filter-props";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { get, uniqBy } from "lodash";
import {
  ApiDropDownData,
  baseFilterConfig,
  DropDownData,
  LevelOpsFilter,
  LevelOpsFilterTypes
} from "model/filters/levelopsFilters";
import { JIRA_FILTER_KEY_MAPPING } from "../constant";
import { ACROSS_OPTIONS, STACK_OPTIONS } from "./constant";
import { isStackFilterDisabled } from "./helper";

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

export const StackFilterConfig: LevelOpsFilter = {
  id: "stacks",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stacks",
  beKey: "stacks",
  labelCase: "title_case",
  disabled: isStackFilterDisabled,
  filterInfo: (args: any) => {
    if (isStackFilterDisabled(args)) return "Stacks option is not applicable when x-Axis value is Time Based or Stage";
    return "";
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

export const StageFilterConfig: LevelOpsFilter = baseFilterConfig((JIRA_FILTER_KEY_MAPPING as any)["stage"], {
  renderComponent: UniversalSelectFilterWrapper,
  apiContainer: APIFilterContainer,
  label: "Stage",
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  type: LevelOpsFilterTypes.API_DROPDOWN,
  labelCase: "title_case",
  required: true,
  partialSupport: true,
  excludeSupport: true,
  partialKey: "stage",
  supportPaginatedSelect: true,
  apiFilterProps: genericApiFilterProps,
  filterMetaData: {
    selectMode: "multiple",
    uri: "jira_filter_values",
    method: "list",
    payload: (args: Record<string, any>) => {
      return {
        integration_ids: get(args, "integrationIds", []),
        fields: ["stage"],
        filter: { integration_ids: get(args, "integrationIds", []) }
      };
    },
    options: (args: any) => {
      const data = extractFilterAPIData(args, "stage");
      return data
        ?.map((item: any) => ({
          label: item.additional_key ?? item.key,
          value: item.key
        }))
        .filter((item: { label: string; value: string }) => !!item.value);
    },
    specialKey: "stage",
    sortOptions: true,
    createOption: true
  } as ApiDropDownData
});
