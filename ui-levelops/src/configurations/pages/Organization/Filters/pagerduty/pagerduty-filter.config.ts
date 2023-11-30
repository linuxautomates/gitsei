import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { pagerdutySupportedFilters, PAGERDUTY_COMMON_FILTER_KEY_MAPPING } from "dashboard/reports/pagerduty/constant";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import OrganisationFilterSelect from "../organisationFilterSelect";

const commonPagerdutyFilters: Array<{ key: string; label: string }> = pagerdutySupportedFilters.values.map(
  (filter: string) => ({
    key: filter,
    label: toTitleCase(filter.replace(/_/g, " "))
  })
);

export const OUPagerdutyCommonFiltersConfig: LevelOpsFilter[] = [
  ...commonPagerdutyFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig(PAGERDUTY_COMMON_FILTER_KEY_MAPPING[item.key] ?? item.key, {
      renderComponent: OrganisationFilterSelect,
      apiContainer: APIFilterContainer,
      label: item?.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: false,
      excludeSupport: false,
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
          const data = extractFilterAPIData(args, item.key);
          if (["pd_service", "user_id"].includes(item.key)) {
            return data
              ?.map((item: any) => ({
                label: item.name ?? item.key,
                value: item.id
              }))
              .filter((item: { label: string; value: string }) => !!item.value);
          }
          return data
            ?.map((item: any) => ({
              label: item,
              value: item
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
