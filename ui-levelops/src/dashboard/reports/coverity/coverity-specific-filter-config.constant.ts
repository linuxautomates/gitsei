import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { coverityIssueSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import { extractFilterAPIData } from "./../../report-filters/helper";
import { COVERITY_FILTER_KEY_MAPPING } from "./commonCoverityReports.constants";
import { apiFilterProps } from "./commonCoverityReports.helper";

const commonCoverityFilters = coverityIssueSupportedFilters.values.map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

export const coverityCommonFiltersConfig: LevelOpsFilter[] = [
  ...commonCoverityFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((COVERITY_FILTER_KEY_MAPPING as any)[item.key], {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: item.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: true,
      excludeSupport: true,
      partialKey: item.key,
      supportPaginatedSelect: true,
      apiFilterProps,
      filterMetaData: {
        selectMode: "multiple",
        uri: "coverity_defects_values",
        method: "list",
        payload: (args: Record<string, any>) => {
          return {
            integration_ids: get(args, "integrationIds", []),
            fields: [item.key],
            filter: { integration_ids: get(args, "integrationIds", []) }
          };
        },
        options: (args: any) => {
          const data = extractFilterAPIData(args, item.key);
          const isDetectedStream = ["first_detected_stream", "last_detected_stream"].includes(item.key);
          return data
            ?.map((item: any) => ({
              label: item.additional_key ?? item.key,
              value: isDetectedStream ? item.additional_key : item.key
            }))
            .filter((item: { label: string; value: string }) => !!item.value);
        },
        specialKey: item.key,
        sortOptions: true,
        createOption: true
      } as ApiDropDownData
    })
  )
];

export const FirstDetectedAtFilterConfig: LevelOpsFilter = {
  id: "first_detected_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "First Detected At",
  beKey: "cov_defect_first_detected_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const LastDetectedAtFilterConfig: LevelOpsFilter = {
  id: "last_detected_at",
  renderComponent: UniversalTimeBasedFilter,
  label: "Last Detected At",
  beKey: "cov_defect_last_detected_at",
  labelCase: "title_case",
  deleteSupport: true,
  filterMetaData: {
    slicing_value_support: true,
    selectMode: "default",
    options: []
  },
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
