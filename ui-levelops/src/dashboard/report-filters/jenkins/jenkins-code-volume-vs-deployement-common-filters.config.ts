import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { GET_PARENT_AND_TYPE_KEY } from "dashboard/constants/filter-name.mapping";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { CodeVolVsDeployemntSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import {
  CODE_VOL_VS_DEPLOYMENT_FILTER_INFO_MAPPING,
  REQUIRED_CODE_VOL_VS_DEPLOYMENT_FILTERS,
  SCM_CODE_VOLUME_FILTER_KEY_MAPPING
} from "dashboard/reports/jenkins/code-volume-vs-deployment-report/constants";
import { get } from "lodash";
import { LevelOpsFilter, baseFilterConfig, LevelOpsFilterTypes, ApiDropDownData } from "model/filters/levelopsFilters";
import { withDeleteAPIProps } from "../common/common-api-filter-props";

const jenkinsJCodeVolVsDeployementFilters = CodeVolVsDeployemntSupportedFilters.values.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

export const jenkinsCodeVolVsDeployementCommonFiltersConfig: LevelOpsFilter[] = [
  ...jenkinsJCodeVolVsDeployementFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig(item.key, {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: item.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: REQUIRED_CODE_VOL_VS_DEPLOYMENT_FILTERS.includes(item.key) ? false : true,
      partialSupport: true,
      excludeSupport: false,
      partialKey: item.key,
      supportPaginatedSelect: true,
      filterInfo: CODE_VOL_VS_DEPLOYMENT_FILTER_INFO_MAPPING[item.key] ?? "",
      required: REQUIRED_CODE_VOL_VS_DEPLOYMENT_FILTERS.includes(item.key),
      modifiedFilterValueChange: (args: any) => {
        const { value, onModifiedFilterValueChange } = args;
        const getParentAndTypeKey = getWidgetConstant(
          "code_volume_vs_deployment_report",
          GET_PARENT_AND_TYPE_KEY,
          undefined
        );
        const { parentKey, _type } = getParentAndTypeKey(item.key);
        const payload = {
          parentKey: parentKey,
          value: value,
          type: _type
        };
        onModifiedFilterValueChange?.(payload);
      },
      modifiedFilterRemove: (args: any) => {
        const { onChildFilterRemove, key } = args;
        const filterKey = item.key.slice(item.key.indexOf("_") + 1);
        let filterPayload: any = {};
        if (key?.includes("build")) {
          filterPayload = {
            value: filterKey,
            parentKey: "build_job"
          };
        } else {
          filterPayload = {
            value: filterKey,
            parentKey: "deploy_job"
          };
        }
        onChildFilterRemove(filterPayload);
      },
      apiFilterProps: (args: any) => ({
        withDelete: withDeleteAPIProps(args)
      }),
      isSelected: (args: any) => {
        const { filters } = args;
        const filterKey = item.key.slice(item.key.indexOf("_") + 1);
        if (item.key.includes("build")) {
          return !!get(filters, ["build_job", SCM_CODE_VOLUME_FILTER_KEY_MAPPING[filterKey]]);
        } else {
          return !!get(filters, ["deploy_job", SCM_CODE_VOLUME_FILTER_KEY_MAPPING[filterKey]]);
        }
      },
      getMappedValue: (args: any) => {
        const { allFilters } = args;
        const filterKey = item.key.slice(item.key.indexOf("_") + 1);
        if (item.key.includes("build")) {
          return get(allFilters, ["build_job", SCM_CODE_VOLUME_FILTER_KEY_MAPPING[filterKey]]);
        } else {
          return get(allFilters, ["deploy_job", SCM_CODE_VOLUME_FILTER_KEY_MAPPING[filterKey]]);
        }
      },
      filterMetaData: {
        selectMode: "multiple",
        uri: item.key.includes("build") ? "cicd_filter_values" : "jenkins_jobs_filter_values",
        method: "list",
        payload: (args: Record<string, any>) => {
          return {
            integration_ids: get(args, "integrationIds", []),
            fields: [item.key.slice(item.key.indexOf("_") + 1)],
            filter: {
              integration_ids: get(args, "integrationIds", []),
              cicd_integration_ids: get(args, "integrationIds", [])
            }
          };
        },
        specialKey: item.key,
        options: (args: any) => {
          const filterMetaData = get(args, ["filterMetaData"], {});
          const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
          const currData = filterApiData.find(
            (fData: any) => Object.keys(fData)[0] === item.key.slice(item.key.indexOf("_") + 1)
          );
          if (currData) {
            return (Object.values(currData)[0] as Array<any>)
              ?.map((_item: any) => ({
                label: _item.additional_key ?? _item.key,
                value: item.key.includes("job_normalized_full_name") ? _item.cicd_job_id : _item.key
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
