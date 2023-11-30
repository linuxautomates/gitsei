import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { STARTS_WITH, CONTAINS } from "dashboard/constants/constants";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get } from "lodash";
import { LevelOpsFilter, baseFilterConfig, LevelOpsFilterTypes, ApiDropDownData } from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

export const salesforceFiltersKeyMapping: Record<string, string> = {
  status: "statuses",
  priority: "priorities",
  type: "types",
  contact: "contacts",
  account_name: "accounts"
};

export const salesforceSupportedFilters = ["status", "priority", "type", "contact", "account_name"];

const apiFilterProps = (args: any) => {
  const withDelete: withDeleteProps = {
    showDelete: args?.deleteSupport,
    key: args?.beKey,
    onDelete: args.handleRemoveFilter
  };
  return { withDelete };
};

const salesforceCommonFilters = salesforceSupportedFilters.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

export const salesforceCommonFiltersConfig: LevelOpsFilter[] = [
  ...salesforceCommonFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((salesforceFiltersKeyMapping as any)[item.key], {
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
      apiFilterProps: apiFilterProps,
      filterMetaData: {
        selectMode: "multiple",
        uri: "salesforce_filter_values",
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
