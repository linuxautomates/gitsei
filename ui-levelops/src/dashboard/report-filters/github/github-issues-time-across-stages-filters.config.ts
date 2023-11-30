import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { scmIssuesTimeAcrossStagesSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { SCM_COMMON_FILTER_KEY_MAPPING, SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING } from "dashboard/reports/scm/constant";
import { get } from "lodash";
import { baseFilterConfig, LevelOpsFilterTypes, ApiDropDownData } from "model/filters/levelopsFilters";
import {
  WithSwitchProps,
  withDeleteProps
} from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

const githubIssuesTimeAcrossStagesFilters = scmIssuesTimeAcrossStagesSupportedFilters.values.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

export const githubSCMIssuesTimeAcrossStagesCommonFilters = [
  ...githubIssuesTimeAcrossStagesFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig(SCM_COMMON_FILTER_KEY_MAPPING[item.key], {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: SCM_ISSUES_COMMON_FILTER_LABEL_MAPPING[item.key] ?? item.label,
      tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: false,
      excludeSupport: true,
      partialKey: item.key,
      supportPaginatedSelect: true,
      apiFilterProps: (args: any) => {
        const switchValue = !!get(args?.allFilters || {}, ["exclude", args?.excludeKey ?? args?.beKey], undefined);
        const withSwitch: WithSwitchProps = {
          showSwitch: item?.key === "column",
          showSwitchText: true,
          switchText: "Exclude",
          switchValue,
          onSwitchValueChange: (value: any) => args.handleSwitchValueChange(args?.excludeKey ?? args?.beKey, value)
        };
        const withDelete: withDeleteProps = {
          showDelete: args?.deleteSupport,
          key: args?.beKey,
          onDelete: args.handleRemoveFilter
        };
        return { withSwitch: args?.excludeSupport === false ? undefined : withSwitch, withDelete };
      },
      filterMetaData: {
        selectMode: "multiple",
        uri: "scm_issues_filter_values",
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
