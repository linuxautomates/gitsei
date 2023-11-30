import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { testrailsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { TESTRAILS_COMMON_FILTER_KEY_MAPPING, TESTRAILS_FILTER_LABEL_MAPPING } from "dashboard/reports/testRails/commonTestRailsReports.constants";
import { get, uniqBy } from "lodash";
import { LevelOpsFilter, baseFilterConfig, LevelOpsFilterTypes, ApiDropDownData } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import { withDeleteAPIProps } from "../common/common-api-filter-props";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import UniversalCustomSprintSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalCustomSprintFilter";
import UniversalTimeBasedFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTimeBasedFilter";
import { INTEGRATION_FIELD_NUMERIC_TYPES } from "configurations/containers/integration-steps/constant";
import { extractFilterAPIData } from "../helper";

export const allowedFilterInTestCaseCountMetric = ["type", "project", "priority", "milestone"];

const commonTestrailsFilters: Array<{ key: string; label: string }> = testrailsSupportedFilters.values.map(
  (filter: string) => ({
    key: filter,
    label: toTitleCase(filter.replace(/_/g, " "))
  })
);

export const TestrailsCommonFiltersConfig: LevelOpsFilter[] = [
  ...commonTestrailsFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((TESTRAILS_COMMON_FILTER_KEY_MAPPING as any)[item.key], {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: TESTRAILS_FILTER_LABEL_MAPPING[item.key] ?? item?.label,
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
        uri: "testrails_tests_values",
        method: "list",
        payload: (args: Record<string, any>) => {
          const marticFilter = args?.filters && args?.filters?.metric ? { 'metric' : args?.filters?.metric} : {}
          return {
            integration_ids: get(args, "integrationIds", []),
            fields: [item.key],
            filter: { integration_ids: get(args, "integrationIds", []), ...marticFilter }
          };
        },
        options: (args: any) => {
          const filterMetaData = get(args, ["filterMetaData"], {});
          const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
          const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === item.key);
          if (currData) {
            return (Object.values(currData)[0] as Array<any>)
              ?.map((item: any) => ({
                label: item.additional_key ?? item.key,
                value: item.key
              }))
              .filter((item: { label: string; value: string }) => !!item.value);
          }
          return [];
        },
        specialKey: item.key,
        sortOptions: true,
        createOption: true
      } as ApiDropDownData,
      hideFilter: (args: any) => {
        const metric = get(args, ["filters", "metric"], "");
        return metric === "test_case_count" && !allowedFilterInTestCaseCountMetric.includes(item.key) ? true : false;
      }
    })
  )
];

export const generateTestrailsCustomFieldConfig = (
  customData: any[],
  fieldsList?: { key: string; type: string; name: string }[]
): LevelOpsFilter[] => {

  return uniqBy(
    customData.map((custom: any) => {

      let isTimeBased = false;
      let isSingleDropdown = false;
      const itemFromFieldsList = (fieldsList || []).find(
        (item: { key: string; type: string; name: string }) => item.key === custom.key
      );
      if (itemFromFieldsList) {
        isTimeBased = CustomTimeBasedTypes.includes(itemFromFieldsList.type?.toLowerCase());
        isSingleDropdown = itemFromFieldsList.type?.toLowerCase() === 'checkbox' ? true : false;
      }
      const transformedPrefix = get(custom, ["metadata", "transformed"]);
      const fieldKey = !!transformedPrefix ? custom?.key?.replace(transformedPrefix, "") : custom?.key;
      return baseFilterConfig(custom.key, {
        renderComponent:
          ((custom.name || "") as string).toLowerCase() === "sprint"
            ? UniversalCustomSprintSelectFilterWrapper
            : isTimeBased
              ? UniversalTimeBasedFilter
              : UniversalSelectFilterWrapper,
        apiContainer: isTimeBased ? undefined : APIFilterContainer,
        label: custom.name,
        deleteSupport: true,
        supportPaginatedSelect: true,
        tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
        type: LevelOpsFilterTypes.API_DROPDOWN,
        labelCase: "title_case",
        partialSupport: INTEGRATION_FIELD_NUMERIC_TYPES.includes(itemFromFieldsList?.type ?? "") ? false : true,
        excludeSupport: false,
        partialKey: custom.key,
        apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
        filterMetaData: {
          selectMode: isSingleDropdown ? "default" : "multiple",
          uri: "testrails_custom_filter_values",
          method: "list",
          payload: (args: Record<string, any>) => {
            const marticFilter = args?.filters && args?.filters?.metric ? { 'metric' : args?.filters?.metric} : {}
            return {
              integration_ids: get(args, "integrationIds", []),
              fields: [fieldKey],
              filter: { integration_ids: get(args, "integrationIds", []), ...marticFilter }
            };
          },
          specialKey: custom.key,
          options: (args: any) => {
            const data = extractFilterAPIData(args, fieldKey);
            return (data as Array<any>)?.map((item: any) => ({
              label: item.key,
              value: item.key
            }));
          },
          sortOptions: true,
          createOption: true
        } as ApiDropDownData
      });
    }),
    "beKey"
  );
};
