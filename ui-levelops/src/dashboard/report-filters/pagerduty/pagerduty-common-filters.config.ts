import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { pagerdutyFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { timeToResolveValuesToFilters } from "dashboard/reports/pagerduty/time-to-resolve/constants";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import { withDeleteAPIProps } from "../common/common-api-filter-props";

export const commonPagerdutyFilters: Array<{ key: string; label: string }> = pagerdutyFilters.values.map(
  (filter: string) => ({
    key: filter,
    label: toTitleCase(filter.replace(/_/g, " "))
  })
);

export const PagerdutyCommonFiltersConfig: LevelOpsFilter[] = [
  ...commonPagerdutyFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((timeToResolveValuesToFilters as any)[item.key], {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: item?.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: false,
      excludeSupport: false,
      supportPaginatedSelect: true,
      apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
      filterMetaData: {
        selectMode: "multiple",
        uri: "pagerduty_filter_values",
        method: "list",
        payload: (args: Record<string, any>) => {
          return {
            integration_ids: get(args, "integrationIds", []),
            fields: [item.key],
            filter: { integration_ids: get(args, "integrationIds", []) }
          };
        },
        options: (args: any) => {
          const filterMetaData = get(args, ["filterMetaData"], {});
          const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
          const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === item.key);
          if (currData) {
            return (Object.values(currData)[0] as Array<any>)
              ?.map((_item: any) => ({
                label: typeof _item === "string" ? _item : _item?.name,
                value: typeof _item === "string" ? _item : _item?.id
              }))
              .filter((item: { label: string; value: string }) => !!item.value);
          }
          return [];
        },
        specialKey: item.key,
        sortOptions: true,
        createOption: true
      } as ApiDropDownData
    })
  )
];
