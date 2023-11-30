import { IntegrationTypes } from "constants/IntegrationTypes";
import { uniq } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { getCICDFilterConfig } from "../dora/filters.config";
import { getCICDSuportedFilters } from "../dora/helper";
import { supportedFilterType } from "dashboard/constants/supported-filters.constant";
import { harnessJobSupportedFilters } from "configurations/pages/Organization/Filters/harnessng/harnessng-job-filter.config";
import { HarnessTableConfigOpenReport } from "dashboard/pages/dashboard-tickets/configs/jenkinsTableConfig";

interface filterConfigParamsProps {
  integrationState: any;
  reportConfig: LevelOpsFilter[];
  columnConfig: LevelOpsFilter[];
  slackConfig?: LevelOpsFilter;
}

interface filterConfigParamsDrilldownProps {
  integrationData: any;
  filterConfig?: any;
  columnConfig?: any;
}

export const getFilterCommonConfig = (filterConfigParams: filterConfigParamsProps) => {
  let { integrationState, reportConfig, columnConfig } = filterConfigParams;

  const getUniqApplicationName = uniq(
    integrationState.map((integration: { application: string }) => integration.application)
  );

  if (getUniqApplicationName.includes(IntegrationTypes.JENKINS)) {
    return [...columnConfig, ...reportConfig];
  } else if (getUniqApplicationName.includes(IntegrationTypes.HARNESSNG)) {
    const harnessConfig = getCICDFilterConfig(
      getCICDSuportedFilters(IntegrationTypes.HARNESSNG || ""),
      IntegrationTypes.HARNESSNG
    );
    return [...harnessConfig, ...reportConfig].filter(item => !["triage_rule"].includes(item.beKey));
  } else {
    return [...columnConfig, ...reportConfig].filter(
      item => !["triage_rule", "instance_names", "parameters"].includes(item.beKey)
    );
  }
};

export const getFilterCommonConfigDrillDown = (
  type: string,
  filterConfigParamsDrilldown: filterConfigParamsDrilldownProps
) => {
  let { integrationData, filterConfig, columnConfig } = filterConfigParamsDrilldown;

  const getUniqApplicationName = uniq(
    integrationData?.map((integration: { application: string }) => integration.application)
  );

  switch (type) {
    case "filterConfig":
      if (getUniqApplicationName.includes(IntegrationTypes.JENKINS)) {
        return filterConfig;
      } else if (getUniqApplicationName.includes(IntegrationTypes.HARNESSNG)) {
        return harnessJobSupportedFilters;
      } else {
        return filterConfig;
      }
      break;
    case "drillDownColumnConfig":
      if (getUniqApplicationName.includes(IntegrationTypes.JENKINS)) {
        return columnConfig;
      } else if (getUniqApplicationName.includes(IntegrationTypes.HARNESSNG)) {
        return [...columnConfig, ...HarnessTableConfigOpenReport];
      } else {
        return columnConfig;
      }

    default:
      break;
  }
};
