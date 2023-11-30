import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { zendeskSupportedFilters, ZENDESK_COMMON_FILTER_KEY_MAPPING } from "dashboard/reports/zendesk/constant";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import OrganisationFilterSelect from "../organisationFilterSelect";

const zendeskCommonFilters = zendeskSupportedFilters.values.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

export const OUZendeskCommonFiltersConfig: LevelOpsFilter[] = [
  ...zendeskCommonFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((ZENDESK_COMMON_FILTER_KEY_MAPPING as any)[item.key] ?? item.key, {
      renderComponent: OrganisationFilterSelect,
      apiContainer: APIFilterContainer,
      label: item.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: true,
      excludeSupport: true,
      partialKey: item.key,
      filterMetaData: {
        selectMode: "multiple",
        uri: "zendesk_filter_values",
        method: "list",
        payload: (args: Record<string, any>) => {
          return {
            integration_ids: get(args, "integrationIds", []),
            fields: [item.key],
            filter: { integration_ids: get(args, "integrationIds", []) }
          };
        },
        specialKey: item.key,
        options: (args: any) => {
          const data = extractFilterAPIData(args, item.key);
          return data
            ?.map((item: any) => ({
              label: item.additional_key ?? item.key,
              value: item.key
            }))
            .filter((item: { label: string; value: string }) => !!item.value);
        },
        sortOptions: true,
        createOption: true
      } as ApiDropDownData
    })
  )
];
