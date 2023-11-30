import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { issueManagementSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import {
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig,
  AzureIterationFilterConfig
} from "dashboard/reports/azure/azure-specific-filter-config.constant";
import { AZURE_FILTER_KEY_MAPPING } from "dashboard/reports/azure/constant";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { staticPriorties } from "shared-resources/charts/jira-prioirty-chart/helper";
import { Dict } from "types/dict";
import { toTitleCase } from "utils/stringUtils";

const commonAzureFilters = issueManagementSupportedFilters?.values.map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

const issueManagementCommonFilterOptionsMapping: Dict<string, string> = {
  workitem_version: "Affects Version",
  workitem_parent_workitem_id: "Parent Workitem id",
  workitem_sprint_full_names: "Azure Iteration"
};

export const EIAzureCommonFiltersConfig: LevelOpsFilter[] = [
  baseFilterConfig((AZURE_FILTER_KEY_MAPPING as any)["workitem_priority"], {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: "Workitem Priority",
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "workitem_priority",
    apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
    filterMetaData: {
      selectMode: "multiple",
      uri: "issue_management_workitem_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["priority"],
          filter: { integration_ids: get(args, "integrationIds", []) }
        };
      },
      specialKey: "workitem_priority",
      options: (args: any) => {
        const data = extractFilterAPIData(args, "priority");
        const newData = get(data, ["records"], []);
        return (newData as Array<any>)?.map((item: any) => ({
          label: get(staticPriorties, [item.key], item.key),
          value: item.key
        }));
      },
      sortOptions: true,
      createOption: true
    } as ApiDropDownData
  }),
  ...commonAzureFilters
    .filter((item: { key: string; label: string }) => item.key !== "workitem_priority")
    .map((item: { key: string; label: string }) =>
      baseFilterConfig((AZURE_FILTER_KEY_MAPPING as any)[item.key], {
        renderComponent: UniversalSelectFilterWrapper,
        apiContainer: APIFilterContainer,
        label: (issueManagementCommonFilterOptionsMapping as any)[item.key] ?? item.label,
        tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
        type: LevelOpsFilterTypes.DROPDOWN,
        labelCase: "title_case",
        deleteSupport: true,
        partialSupport: true,
        excludeSupport: true,
        partialKey: item.key,
        apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
        filterMetaData: {
          selectMode: "multiple",
          uri: "issue_management_workitem_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            const newKey =
              item.key !== "workitem_type" && item.key.slice(0, 9) === "workitem_" ? item.key.slice(9) : item.key;
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [newKey],
              filter: { integration_ids: get(args, "integrationIds", []) }
            };
          },
          options: (args: any) => {
            const newKey =
              item.key !== "workitem_type" && item.key.slice(0, 9) === "workitem_" ? item.key.slice(9) : item.key;
            const data = extractFilterAPIData(args, newKey);
            const newData = get(data, ["records"], []);
            if (newData) {
              return (newData as Array<any>)?.map((item: any) => ({
                label: item.additional_key ?? item.key,
                value: item.key
              }));
            }
            return [];
          },
          specialKey: item.key,
          sortOptions: true,
          createOption: true
        } as ApiDropDownData
      })
    ),
  AzureTeamsFilterConfig,
  AzureCodeAreaFilterConfig
];
