import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import JenkinsParametersFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/jenkinsParametersFilterWrapper";
import { ApiDropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { withDeleteAPIProps } from "../common/common-api-filter-props";
import { get } from "lodash";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import { CICD_EXECUTION_HARNESS_FITER_NOTE } from "configurations/pages/lead-time-profiles/containers/workflowDetails/components/constant";

export const ParameterFilterConfig: LevelOpsFilter = {
  id: "parameter",
  renderComponent: JenkinsParametersFilterWrapper,
  label: "Build Parameter",
  beKey: "parameters",
  labelCase: "title_case",
  apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
  deleteSupport: true,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  apiContainer: APIFilterContainer,
  filterMetaData: {
    selectMode: "multiple",
    uri: "cicd_job_params",
    method: "list",
    payload: (args: Record<string, any>) => {
      return {
        integration_ids: get(args, "integrationIds", []),
        fields: ['parameters'],
        filter: { integration_ids: get(args, "integrationIds", []) }
      };
    },
    specialKey: 'parameters',
    options: (args: any) => {
      const filterMetaData = get(args, ["filterMetaData"], {});
      const filterApiData = get(filterMetaData, ["apiConfig", "data", "records"], []);
      if (filterApiData) {
        return filterApiData;
      }
      return [];
    },
    sortOptions: true,
    createOption: true
  } as ApiDropDownData,
  filterInfo: CICD_EXECUTION_HARNESS_FITER_NOTE,
};

export const generateParameterFilterConfig = (label?: string): LevelOpsFilter => ({
  ...ParameterFilterConfig,
  label: label ? label : "JOB RUN PARAMETERS"
});
