import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { githubFilesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { SCM_COMMON_FILTER_KEY_MAPPING, SCM_FILTER_OPTIONS_MAPPING } from "dashboard/reports/scm/constant";
import { get } from "lodash";
import { LevelOpsFilter, baseFilterConfig, LevelOpsFilterTypes, ApiDropDownData } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import genericApiFilterProps from "../common/common-api-filter-props";

const commonFilesGithubFilters = githubFilesSupportedFilters.values.map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

export const githubFilesCommonFiltersConfig: LevelOpsFilter[] = [
  ...commonFilesGithubFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((SCM_COMMON_FILTER_KEY_MAPPING as any)[item.key] ?? item.key, {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: (SCM_FILTER_OPTIONS_MAPPING as any)[item.key] ?? item.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: true,
      excludeSupport: true,
      partialKey: item.key,
      supportPaginatedSelect: true,
      apiFilterProps: genericApiFilterProps,
      filterMetaData: {
        selectMode: "multiple",
        uri: "scm_files_filter_values",
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
        sortOptions: true,
        createOption: true
      } as ApiDropDownData
    })
  )
];
