import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { jenkinsJobConfigSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { JENKINS_JOB_FILTERS_NAME_MAPPING } from "dashboard/reports/jenkins/constants";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { withDeleteAPIProps } from "../common/common-api-filter-props";

const jenkinsJobConfigFilters = jenkinsJobConfigSupportedFilters.values.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

export const jenkinsJobConfigCommonFiltersConfig: LevelOpsFilter[] = [
  ...jenkinsJobConfigFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((JENKINS_JOB_FILTERS_NAME_MAPPING as any)[item.key], {
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
      apiFilterProps: (args: any) => ({
        withDelete: withDeleteAPIProps(args)
      }),
      filterMetaData: {
        selectMode: "multiple",
        uri: "jenkins_job_config_filter_values",
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
