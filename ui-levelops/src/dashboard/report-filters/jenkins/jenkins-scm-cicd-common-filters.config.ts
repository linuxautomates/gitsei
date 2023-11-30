import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { scmCicdSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import {
  JENKINS_CICD_FILTER_KEY_MAPPING,
  JENKINS_CICD_FILTER_LABEL_MAPPING
} from "dashboard/reports/jenkins/constants";
import { get } from "lodash";
import { LevelOpsFilter, baseFilterConfig, LevelOpsFilterTypes, ApiDropDownData } from "model/filters/levelopsFilters";
import genericApiFilterProps from "../common/common-api-filter-props";
import { jenkinsJobApiFilterProps } from "./jenkins-common-filter-props.config";

const jenkinsScmCicdFilters = scmCicdSupportedFilters.values.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

export const jenkinsCicdCommonFiltersConfig: LevelOpsFilter[] = [
  ...jenkinsScmCicdFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig(JENKINS_CICD_FILTER_KEY_MAPPING[item.key], {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: JENKINS_CICD_FILTER_LABEL_MAPPING[item.key] ?? item.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: true,
      excludeSupport: true,
      partialKey: item.key,
      supportPaginatedSelect: true,
      apiFilterProps: item.key === "job_normalized_full_name" ? genericApiFilterProps : jenkinsJobApiFilterProps,
      filterMetaData: {
        selectMode: "multiple",
        uri: "cicd_filter_values",
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
