import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { sonarqubemetricsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { SONARQUBE_COMMON_FILTER_KEY_MAPPING } from "dashboard/reports/sonarqube/constant";
import { get } from "lodash";
import { LevelOpsFilter, baseFilterConfig, LevelOpsFilterTypes, ApiDropDownData, DropDownData } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import { withDeleteAPIProps } from "../common/common-api-filter-props";
import { generateMetricFilterConfig } from "../common/metrics-filter.config";

const commonSonarqubeMetricsFilters: Array<{
  key: string;
  label: string;
}> = sonarqubemetricsSupportedFilters.values.map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

export const SonarqubeMetricsCommonFiltersConfig: LevelOpsFilter[] = [
  ...commonSonarqubeMetricsFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((SONARQUBE_COMMON_FILTER_KEY_MAPPING as any)[item.key], {
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
        uri: "sonarqube_metrics_values",
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
              ?.map((item: any) => ({
                label: item.key,
                value: item.key
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

export const sonarqubeCodeComplexityMetricsFilterConfig = (CODE_COMPLEXITY_METRIC_OPTIONS: { label: string, value: string}[]): LevelOpsFilter => {
  const metricFilterConfig = generateMetricFilterConfig(CODE_COMPLEXITY_METRIC_OPTIONS);
  return {
    ...metricFilterConfig,
    filterMetaData: {
      ...metricFilterConfig.filterMetaData,
      mapFilterValueForBE: (value: any) => typeof value === 'string' ? [value] : value,      
    } as DropDownData
  }
}