import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { generateAcrossFilterConfig } from "dashboard/report-filters/common/across-filter.config";
import { withDeleteAPIProps } from "dashboard/report-filters/common/common-api-filter-props";
import { PagerdutyServicesCommonFiltersConfig } from "dashboard/report-filters/pagerduty/pagerduty-services-common-filters.config";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import { PAGERDUTY_SERVICE_FILTER_KEY_MAPPING } from "../constant";
import { ACROSS_OPTIONS } from "./constants";

export const PagerdutyReleaseincidentsReportFiltersConfig: LevelOpsFilter[] = [
  baseFilterConfig((PAGERDUTY_SERVICE_FILTER_KEY_MAPPING as any)["cicd_job_id"], {
    renderComponent: UniversalSelectFilterWrapper,
    apiContainer: APIFilterContainer,
    label: "cicd job id",
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
    type: LevelOpsFilterTypes.API_DROPDOWN,
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: false,
    excludeSupport: false,
    supportPaginatedSelect: true,
    required: true,
    apiFilterProps: (args: any) => ({ withDelete: withDeleteAPIProps(args) }),
    filterMetaData: {
      selectMode: "multiple",
      uri: "jenkins_pipelines_jobs_filter_values",
      method: "list",
      payload: (args: Record<string, any>) => {
        return {
          integration_ids: get(args, "integrationIds", []),
          fields: ["cicd_job_id"],
          filter: { integration_ids: get(args, "integrationIds", []) }
        };
      },
      options: (args: any) => {
        const filterMetaData = get(args, ["filterMetaData"], {});
        const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
        const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === "cicd_job_id");
        if (currData) {
          return (Object.values(currData)[0] as Array<any>)
            ?.map((_item: any) => ({
              label: typeof _item === "string" ? _item : _item?.name,
              value: typeof _item === "string" ? _item : _item?.id
            }))
            .filter((item: { label: string; value: string }) => !!item.value);
        }
        return [];
      },
      specialKey: "cicd_job_id",
      sortOptions: true,
      createOption: true
    } as ApiDropDownData
  }),
  ...PagerdutyServicesCommonFiltersConfig,
  generateAcrossFilterConfig(ACROSS_OPTIONS)
];
