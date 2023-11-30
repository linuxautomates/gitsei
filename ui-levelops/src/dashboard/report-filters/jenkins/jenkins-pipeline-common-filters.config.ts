import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import {
  jenkinsPipelineJobSupportedFilters,
  supportedFilterType
} from "dashboard/constants/supported-filters.constant";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import {
  JENKINS_PIPELINE_FILTERS_NAME_MAPPING,
  JENKINS_PIPElINE_FILTER_LABEL_MAPPING
} from "dashboard/reports/jenkins/constants";
import { get } from "lodash";
import { LevelOpsFilter, baseFilterConfig, LevelOpsFilterTypes, ApiDropDownData } from "model/filters/levelopsFilters";
import { withDeleteAPIProps } from "../common/common-api-filter-props";
import { jenkinsJobApiFilterProps } from "./jenkins-common-filter-props.config";

const jenkinsPipelineJobFilters = jenkinsPipelineJobSupportedFilters.values.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

export const jenkinsPipelineCommonFiltersConfig: LevelOpsFilter[] = [
  baseFilterConfig("projects", {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: JENKINS_PIPElINE_FILTER_LABEL_MAPPING["project_name"],
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: false,
    excludeSupport: true,
    supportPaginatedSelect: true,
    apiFilterProps: jenkinsJobApiFilterProps,
    filterMetaData: {
      selectMode: "multiple",
      uri: "jenkins_jobs_filter_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        const additionalFilter = get(args, "additionalFilter", {});
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["project_name"],
          filter: {
            integration_ids: get(args, "integrationIds", []),
            ...additionalFilter,
          }
        };
      },
      specialKey: "projects",
      options: (args: any) => {
        const filterMetaData = get(args, ["filterMetaData"], {});
        const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
        const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === "project_name");
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
  }),
  ...jenkinsPipelineJobFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((JENKINS_PIPELINE_FILTERS_NAME_MAPPING as any)[item.key], {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: (JENKINS_PIPElINE_FILTER_LABEL_MAPPING as any)[item.key] ?? item.label,
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
        uri: "jenkins_pipelines_jobs_filter_values",
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
          let res: any = [];
          if (currData) {
            res = (Object.values(currData)[0] as Array<any>)
              ?.map((_item: any) => {
                const labelKey = item.key === "cicd_job_id" ? _item?.key : _item.additional_key ?? _item.key;
                const valueKey = item.key === "cicd_job_id" ? _item?.cicd_job_id : _item.key;
                return {
                  label: labelKey,
                  value: valueKey
                };
              })
              .filter((item: { label: string; value: string }) => !!item.value);
          }
          return res;
        },
        sortOptions: true,
        createOption: true
      } as ApiDropDownData
    })
  )
];
