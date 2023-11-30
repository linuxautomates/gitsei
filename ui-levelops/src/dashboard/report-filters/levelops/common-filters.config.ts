import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { DynamicGraphFilter } from "dashboard/constants/applications/levelops.application";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get } from "lodash";
import { LevelOpsFilter, baseFilterConfig, LevelOpsFilterTypes, ApiDropDownData } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";
import { withDeleteAPIProps } from "../common/common-api-filter-props";

const assessmentsSupportedFilters: Array<DynamicGraphFilter> = [
  {
    label: "Assessments",
    uri: "questionnaires",
    searchField: "name",
    filterType: "apiMultiSelect",
    position: "left",
    filterField: "questionnaire_template_id"
  },
  {
    label: "Progress",
    filterType: "select",
    filterField: "completed",
    position: "left",
    options: [
      { label: "COMPLETED", value: "true" },
      { label: "NOT COMPLETED", value: "false" }
    ]
  },
  { label: "Tags", filterType: "apiMultiSelect", position: "left", filterField: "tags", uri: "tags" },
  { label: "Updated Between", filterType: "dateRange", position: "right", filterField: "updated_at" },
  { label: "Created Between", filterType: "dateRange", position: "right", filterField: "created_at" }
];

export const levelopsSupportedFilters = ["tags", "questionnaire_template_id", "completed"];

export const levelopsFilterLabelMapping: basicMappingType<string> = {
  questionnaire_template_id: "Assessment",
  completed: "Progress"
};

const levelopsURIMapping: basicMappingType<string> = {
  questionnaire_template_id: "questionnaires"
};

const commonLevelopsFilters: Array<{ key: string; label: string }> = levelopsSupportedFilters.map((filter: string) => ({
  key: filter,
  label: toTitleCase(filter.replace(/_/g, " "))
}));

export const LevelopsCommonFiltersConfig: LevelOpsFilter[] = [
  ...commonLevelopsFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig(item.key, {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: levelopsFilterLabelMapping[item.key] ?? item?.label,
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
        uri: levelopsURIMapping[item.key] ?? item.key,
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
          const currData = filterApiData;
          if (filterApiData) {
            return (filterApiData as Array<any>)
              ?.map((item: any) => ({
                label: item.name,
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
