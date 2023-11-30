import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { testrailsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { TESTRAILS_COMMON_FILTER_KEY_MAPPING } from "dashboard/reports/testRails/commonTestRailsReports.constants";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import OrganisationFilterSelect from "../organisationFilterSelect";

const commonTestrailsFilters: Array<{ key: string; label: string }> = testrailsSupportedFilters.values.map(
  (filter: string) => ({
    key: filter,
    label: toTitleCase(filter.replace(/_/g, " "))
  })
);

export const OUTestrailsCommonFiltersConfig: LevelOpsFilter[] = [
  ...commonTestrailsFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig(TESTRAILS_COMMON_FILTER_KEY_MAPPING[item.key] ?? item.key, {
      renderComponent: OrganisationFilterSelect,
      apiContainer: APIFilterContainer,
      label: item?.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: false,
      excludeSupport: false,
      supportPaginatedSelect: true,
      filterMetaData: {
        selectMode: "multiple",
        uri: "testrails_tests_values",
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
          return data
            ?.map((item: any) => ({
              label: item.additional_key ?? item.key,
              value: item.key
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
