import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalDateRangeFilter from "dashboard/graph-filters/components/GenericFilterComponents/UniversalDateRangeFilter";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { TRIAGE_FILTERS_MAPPING } from "./constant";

export const triageGridViewFilterConfigs: LevelOpsFilter[] = [
  ...Object.keys(TRIAGE_FILTERS_MAPPING).map(key =>
    baseFilterConfig(key, {
      renderComponent: UniversalSelectFilterWrapper,
      apiContainer: APIFilterContainer,
      label: TRIAGE_FILTERS_MAPPING[key].label,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: false,
      placeholder: TRIAGE_FILTERS_MAPPING[key].label,
      supportPaginatedSelect: false,
      apiFilterProps: args => ({
        withDelete: withDeleteAPIProps(args)
      }),
      filterMetaData: {
        selectMode: "multiple",
        uri: "jenkins_pipelines_jobs_filter_values",
        method: "list",
        payload: (args: Record<string, any>) => {
          return {
            integration_ids: get(args, "integrationIds", []),
            fields: [TRIAGE_FILTERS_MAPPING[key].value],
            filter: {}
          };
        },
        options: (args: any) => {
          const data = extractFilterAPIData(args, TRIAGE_FILTERS_MAPPING[key].value);
          return data
            ?.map((item: any) => ({
              label: item.key,
              value: TRIAGE_FILTERS_MAPPING[key].value === "cicd_job_id" ? item.cicd_job_id : item.key
            }))
            .filter((item: { label: string; value: string }) => !!item.value);
        },
        specialKey: key,
        sortOptions: true,
        createOption: true
      } as ApiDropDownData
    })
  ),
  baseFilterConfig("triage_rule_ids", {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: "Triage Rules",
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    placeholder: "Triage Rules",
    partialSupport: false,
    supportPaginatedSelect: false,
    apiFilterProps: args => ({
      withDelete: withDeleteAPIProps(args)
    }),
    filterMetaData: {
      selectMode: "multiple",
      uri: "triage_rules",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          filter: {}
        };
      },
      options: (args: any) => {
        const filterMetaData = get(args, ["filterMetaData"], {});
        const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
        return filterApiData
          ?.map((item: any) => ({
            label: item.name,
            value: item.id
          }))
          .filter((item: { label: string; value: string }) => !!item.value);
      },
      specialKey: "triage_rule_ids",
      sortOptions: true,
      createOption: true
    } as ApiDropDownData
  }),
  {
    id: "date_range",
    renderComponent: UniversalDateRangeFilter,
    label: "Date Between",
    beKey: "start_time",
    labelCase: "title_case",
    deleteSupport: true,
    apiFilterProps: args => ({
      withDelete: withDeleteAPIProps(args)
    }),
    filterMetaData: {}
  }
];
