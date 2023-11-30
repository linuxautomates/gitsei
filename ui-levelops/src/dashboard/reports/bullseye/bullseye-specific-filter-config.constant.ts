import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { bullseyeSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalCheckboxFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalCheckboxFilter";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { genericGetFilterAPIData } from "dashboard/report-filters/helper";
import { get } from "lodash";
import { baseFilterConfig, DropDownData, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import { bullseyeBranchCoverageReportFilterNameMapping } from "./branch-coverage-report/constants";
import { BULLSEYE_FILTER_KEY_MAPPING } from "./constant";
import { apiFilterProps } from "./helper";

const commonBullseyeFilters = bullseyeSupportedFilters?.values.map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

export const bullseyeCommonFiltersConfig: LevelOpsFilter[] = [
  baseFilterConfig(BULLSEYE_FILTER_KEY_MAPPING["name"], {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: "Name",
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    partialKey: "name",
    supportPaginatedSelect: true,
    apiFilterProps,
    filterMetaData: {
      selectMode: "multiple",
      uri: "bullseye_filter_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["name"],
          filter: { integration_ids: get(args, "integrationIds", []) }
        };
      },
      options: (args: any) => genericGetFilterAPIData(args, "name"),
      specialKey: "name",
      sortOptions: true,
      createOption: true
    } as DropDownData
  }),
  ...commonBullseyeFilters
    .filter(item => item.key != "name")
    .map((item: { key: string; label: string }) =>
      baseFilterConfig((BULLSEYE_FILTER_KEY_MAPPING as any)[item.key], {
        renderComponent: UniversalSelectFilterWrapper,
        apiContainer: APIFilterContainer,
        label: (bullseyeBranchCoverageReportFilterNameMapping as any)[item.key] ?? item.label,
        tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
        type: LevelOpsFilterTypes.DROPDOWN,
        labelCase: "title_case",
        deleteSupport: true,
        apiFilterProps: (args: any) => ({
          withDelete: withDeleteAPIProps(args)
        }),
        supportPaginatedSelect: true,
        filterMetaData: {
          selectMode: "multiple",
          uri: "bullseye_filter_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [item.key],
              filter: { integration_ids: get(args, "integrationIds", []) }
            };
          },
          options: (args: any) => genericGetFilterAPIData(args, item.key),
          specialKey: item.key,
          sortOptions: true,
          createOption: true
        } as DropDownData
      })
    )
];

export const StackedFilterConfig: LevelOpsFilter = {
  id: "stacked_metrics",
  renderComponent: UniversalCheckboxFilter,
  label: "",
  beKey: "stacked_metrics",
  labelCase: "title_case",
  filterMetaData: {
    checkboxLabel: "Stacked"
  },
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};
