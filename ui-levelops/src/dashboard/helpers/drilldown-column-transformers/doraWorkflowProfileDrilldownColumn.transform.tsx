import { cloneDeep, get } from "lodash";
import {
  ReportDrilldownColTransFuncType,
  ReportDrilldownFilterTransFuncType
} from "dashboard/dashboard-types/common-types";
import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import {
  DeploymentFrequencyTableConfigCICD,
  DeploymentFrequencyTableConfigHarnessCICDDefaultColumn,
  DeploymentFrequencyTableConfigHarnessCICDAllColumn,
  DeploymentFrequencyTableConfigIM,
  DeploymentFrequencyTableConfigIMAdo,
  DeploymentFrequencyTableConfigSCM,
  DoraSCMCommitsTableConfigSCM,
  LeadTimeForChangeTableConfigIMAdo,
  LeadTimeForChangeTableConfigSCM,
  DeploymentFrequencyTableConfigReleaseIM
} from "dashboard/pages/dashboard-tickets/configs/doraMatrixTableConfig";
import { IM_ADO } from "dashboard/pages/dashboard-drill-down-preview/drilldownColumnsHelper";
import {
  jiraSupportedFilters,
  azureLeadTimeSupportedFilters,
  doraScmPRsSupportedFilters,
  doraJenkinsGithubJobSupportedFilters
} from "dashboard/constants/supported-filters.constant";
import { JiraLeadTimeTableConfig } from "dashboard/pages/dashboard-tickets/configs";
import { CALCULATION_RELEASED_IN_KEY } from "configurations/pages/lead-time-profiles/containers/workflowDetails/components/constant";
import {
  AzureLeadTimeTableConfig,
  SCMLeadTimeTableConfig
} from "dashboard/pages/dashboard-tickets/configs/leadTimeTableConfig";
import { IntegrationTypes } from "constants/IntegrationTypes";

/**
 * This function transforms columns for drilldown specific to reports
 * requirements.For ex. for E.I. it integration type is im then im realted column same for different
 */
export const doraDrilldownColumnTransformer: ReportDrilldownColTransFuncType = utilities => {
  const { columns: _columns, filters, doraProfileDeploymentRoute } = utilities;

  const columns = cloneDeep(_columns);

  const { integrationType, integrationApplication, getAllColumn } = filters;

  switch (integrationType) {
    case WorkflowIntegrationType.IM:
      return integrationApplication === IntegrationTypes.AZURE_NON_SPLITTED
        ? DeploymentFrequencyTableConfigIMAdo
        : integrationApplication === IntegrationTypes.JIRA && doraProfileDeploymentRoute === CALCULATION_RELEASED_IN_KEY
          ? DeploymentFrequencyTableConfigReleaseIM
          : DeploymentFrequencyTableConfigIM

      break;
    case WorkflowIntegrationType.SCM:
      if (doraProfileDeploymentRoute === "commit") {
        return DoraSCMCommitsTableConfigSCM;
      }
      return DeploymentFrequencyTableConfigSCM;
      break;
    case WorkflowIntegrationType.CICD:
      return integrationApplication === IntegrationTypes.HARNESSNG
        ? getAllColumn
          ? DeploymentFrequencyTableConfigHarnessCICDAllColumn
          : DeploymentFrequencyTableConfigHarnessCICDDefaultColumn
        : DeploymentFrequencyTableConfigCICD;
      break;
    case IM_ADO:
      return DeploymentFrequencyTableConfigIMAdo;
      break;
    default:
      return columns;
      break;
  }
};

const appendActiveClass = (activeColumn: string, columns: any[]) => {
  return (columns || []).map(item => {
    if (item.key === activeColumn) {
      return {
        ...item,
        className: "active-stage"
      };
    }
    return item;
  });
};

export const doraLeadTimeForChangeDrilldownColumnTransformer: ReportDrilldownColTransFuncType = utilities => {
  const { columns: _columns, filters } = utilities;

  const application = get(filters, "application", "");
  const activeColumnKey = get(filters, [application, "activeColumn"], "");
  const columns = cloneDeep(_columns);

  const integrationType = get(filters, ["integrationType"]);
  switch (integrationType) {
    case WorkflowIntegrationType.IM:
    case IM_ADO:
      return appendActiveClass(activeColumnKey, LeadTimeForChangeTableConfigIMAdo);
    case WorkflowIntegrationType.SCM:
      return appendActiveClass(activeColumnKey, LeadTimeForChangeTableConfigSCM);
    default:
      return appendActiveClass(activeColumnKey, columns);
  }
};

export const leadTimeForChangeDrilldownColumnTransformer: ReportDrilldownColTransFuncType = utilities => {
  const { columns: _columns, filters, doraProfileEvent } = utilities;

  const application = get(filters, "application", "");
  const activeColumnKey = get(filters, [application, "activeColumn"], "");
  const columns = cloneDeep(_columns);

  const integrationType = get(filters, ["integrationType"]);
  switch (doraProfileEvent) {
    case "ticket_created":
    case "api_event":
      if (integrationType === IM_ADO) {
        return AzureLeadTimeTableConfig;
      }
      return JiraLeadTimeTableConfig;
    case "commit_created":
      return appendActiveClass(activeColumnKey, SCMLeadTimeTableConfig);
    default:
      return appendActiveClass(activeColumnKey, columns);
  }
};
export const doraDrilldownSpecificSupportedFilters: ReportDrilldownFilterTransFuncType = (utilities: any) => {
  const { integrationType } = utilities;
  switch (integrationType) {
    case WorkflowIntegrationType.IM:
      return jiraSupportedFilters;
      break;
    case WorkflowIntegrationType.SCM:
      return doraScmPRsSupportedFilters;
      break;
    case WorkflowIntegrationType.CICD:
      return doraJenkinsGithubJobSupportedFilters;
      break;
    case IM_ADO:
      return azureLeadTimeSupportedFilters;
      break;
    default:
      return jiraSupportedFilters;
      break;
  }
};
