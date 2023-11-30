import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { jenkinsJobApiFilterProps } from "dashboard/report-filters/jenkins/jenkins-common-filter-props.config";
import { get } from "lodash";
import {
  LevelOpsFilter,
  DropDownData,
  baseFilterConfig,
  ApiDropDownData,
  LevelOpsFilterTypes
} from "model/filters/levelopsFilters";
import { STACK_OPTIONS } from "./constants";

export const StackFilterConfig: LevelOpsFilter = {
  id: "stacks",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stacks",
  beKey: "stacks",
  labelCase: "title_case",
  disabled: ({ filters }) => {
    return filters && filters.visualization && filters.visualization === IssueVisualizationTypes.DONUT_CHART;
  },
  filterMetaData: {
    clearSupport: true,
    options: STACK_OPTIONS,
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};

export const TriageRuleFilterConfig: LevelOpsFilter = baseFilterConfig("triage_rule", {
  renderComponent: UniversalSelectFilterWrapper,
  apiContainer: APIFilterContainer,
  label: "Triage Rule",
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
        fields: ["triage_rule"],
        filter: {
          integration_ids: get(args, "integrationIds", []),
          ...additionalFilter,
        }
      };
    },
    specialKey: "triage_rule",
    options: (args: any) => {
      const filterMetaData = get(args, ["filterMetaData"], {});
      const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
      const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === "triage_rule");
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
    sortOptions: true,
    createOption: true
  } as ApiDropDownData
});
