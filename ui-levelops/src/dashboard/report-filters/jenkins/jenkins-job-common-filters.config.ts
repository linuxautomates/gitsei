import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { jenkinsCicdJobCountSupportedFilters, jenkinsGithubJobSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import {
  JENKINS_CICD_FILTER_LABEL_MAPPING,
  JENKINS_JOB_FILTERS_NAME_MAPPING
} from "dashboard/reports/jenkins/constants";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import genericApiFilterProps from "../common/common-api-filter-props";
import { getChildFilterLableName, getChildKeys, jenkinsJobApiFilterProps } from "./jenkins-common-filter-props.config";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import UniversalAddChildFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalAddChildFilterWrapper";

const jenkinsGithubJobFilters = jenkinsGithubJobSupportedFilters.values.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

const jenkinsCicdJobCountFilters = jenkinsCicdJobCountSupportedFilters.values.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

export const jenkinsCommonChildConfig = (keyName: string[]) => {
  return keyName.map((childKey) =>
    baseFilterConfig(childKey, {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: JENKINS_CICD_FILTER_LABEL_MAPPING[childKey],
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
          return {
            integration_ids: get(args, "integrationIds", []),
            fields: [childKey],
            filter: {
              integration_ids: get(args, "integrationIds", []),
              ...args?.childComponentFilter
            }
          };
        },
        specialKey: childKey,
        options: (args: any) => {
          const filterMetaData = get(args, ["filterMetaData"], {});
          const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
          const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === childKey);
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
      } as ApiDropDownData,
      renderAddChildComponent: UniversalAddChildFilterWrapper,
      childButtonLableName: getChildFilterLableName
    })
  );
}

const jenkinsCommonFiltersConfig = (filters: Array<{ key: string; label: string }>, keyMapping: basicMappingType<string>, lableMapping: basicMappingType<string>): LevelOpsFilter[] =>
  filters.map((item: { key: string; label: string }) =>
    baseFilterConfig((keyMapping as any)[item.key], {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: lableMapping[item.key] ?? item.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: item.key === "job_normalized_full_name" ? true : false,
      partialKey: item.key,
      excludeSupport: true,
      supportPaginatedSelect: true,
      apiFilterProps: item.key === "job_normalized_full_name" ? genericApiFilterProps : jenkinsJobApiFilterProps,
      childFilterKeys: getChildKeys,
      renderChildComponent: jenkinsCommonChildConfig,
      filterMetaData: {
        selectMode: "multiple",
        uri: "jenkins_jobs_filter_values",
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
  );


export const jenkinsJobsCommonFiltersConfig: LevelOpsFilter[] = [
  baseFilterConfig("projects", {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: JENKINS_CICD_FILTER_LABEL_MAPPING["project_name"],
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
  ...jenkinsCommonFiltersConfig(jenkinsGithubJobFilters, JENKINS_JOB_FILTERS_NAME_MAPPING, JENKINS_CICD_FILTER_LABEL_MAPPING)
];

export const jenkinsCicdJobCountFiltersConfig: LevelOpsFilter[] = [
  ...jenkinsCommonFiltersConfig(jenkinsCicdJobCountFilters, JENKINS_JOB_FILTERS_NAME_MAPPING, JENKINS_CICD_FILTER_LABEL_MAPPING)
];
