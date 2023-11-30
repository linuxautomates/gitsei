import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { get } from "lodash";
import { AzureApplicationSubType, AzureIntegrationSubType, Integration } from "model/entities/Integration";

export const IssueManagementApplications = [
  IntegrationTypes.JIRA,
  IntegrationTypes.AZURE,
  IntegrationTypes.AZURE_NON_SPLITTED
];
export const AZURE_SPLIT_AND_JIRA_APPLICATIONS = [
  IntegrationTypes.JIRA,
  IntegrationTypes.AZURE_BOARDS,
  IntegrationTypes.AZURE_REPOS,
  IntegrationTypes.AZURE_PIPELINES
];
export const SCMApplications = [
  IntegrationTypes.BITBUCKET,
  IntegrationTypes.BITBUCKET_SERVER,
  IntegrationTypes.GITHUB,
  IntegrationTypes.GITLAB,
  IntegrationTypes.AZURE_NON_SPLITTED
];
export const CICDApplications = [
  IntegrationTypes.DRONECI,
  IntegrationTypes.CIRCLECI,
  IntegrationTypes.JENKINS,
  IntegrationTypes.HARNESSNG,
  IntegrationTypes.GITLAB,
  IntegrationTypes.AZURE_NON_SPLITTED,
  IntegrationTypes.GITHUB_ACTIONS
];
export const WORKFLOW_SUPPORTED_APPLICATION = [
  IntegrationTypes.JIRA,
  IntegrationTypes.AZURE,
  IntegrationTypes.AZURE_BOARDS,
  IntegrationTypes.AZURE_REPOS,
  IntegrationTypes.AZURE_PIPELINES,
  IntegrationTypes.BITBUCKET,
  IntegrationTypes.BITBUCKET_SERVER,
  IntegrationTypes.GITHUB,
  IntegrationTypes.GITLAB,
  IntegrationTypes.DRONECI,
  IntegrationTypes.CIRCLECI,
  IntegrationTypes.JENKINS,
  IntegrationTypes.HARNESSNG
];

// Finding type of integration, weather CICD/SCM/IssueManagement
export const findIntegrationType = (integration: Integration | null) => {
  if (!integration) {
    return undefined;
  }
  if (CICDApplications.includes(integration.application as IntegrationTypes)) {
    return WorkflowIntegrationType.CICD;
  } else if (SCMApplications.includes(integration.application as IntegrationTypes)) {
    return WorkflowIntegrationType.SCM;
  } else if (IssueManagementApplications.includes(integration.application as IntegrationTypes)) {
    const subType = get(integration, ["metadata", "subtype"], AzureIntegrationSubType.WI);
    switch (subType) {
      case AzureIntegrationSubType.SCM:
        return WorkflowIntegrationType.SCM;
      case AzureIntegrationSubType.CICD:
        return WorkflowIntegrationType.CICD;
      case AzureIntegrationSubType.WI:
        return WorkflowIntegrationType.IM;
      default:
        return WorkflowIntegrationType.IM;
    }
  } else {
    return undefined;
  }
};

export const getIntegrationType = (application: string | null) => {
  if (!application) {
    return undefined;
  }
  if (application === IntegrationTypes.AZURE_NON_SPLITTED) {
    return WorkflowIntegrationType.IM;
  }
  if (SCMApplications.includes(application as IntegrationTypes)) {
    return WorkflowIntegrationType.SCM;
  } else if (CICDApplications.includes(application as IntegrationTypes)) {
    return WorkflowIntegrationType.CICD;
  } else if (AZURE_SPLIT_AND_JIRA_APPLICATIONS.includes(application as IntegrationTypes)) {
    switch (application) {
      case AzureApplicationSubType.REPOS:
        return WorkflowIntegrationType.SCM;
      case AzureApplicationSubType.PIPELINES:
        return WorkflowIntegrationType.CICD;
      case AzureApplicationSubType.BOARDS:
        return WorkflowIntegrationType.IM;
      default:
        return WorkflowIntegrationType.IM;
    }
  } else {
    return undefined;
  }
};

export const getAzureApplication = (integrationType: string) => {
  switch (integrationType) {
    case WorkflowIntegrationType.IM:
      return "azure_devops_boards";
    case WorkflowIntegrationType.SCM:
      return "azure_devops_repos";
    case WorkflowIntegrationType.CICD:
      return "azure_devops_pipelines";
    default:
      return "azure_devops_boards";
  }
};
// Finding an integration of type provided.
export const integrationFinder = (integration_type: WorkflowIntegrationType) => (int: Integration) => {
  switch (integration_type) {
    case WorkflowIntegrationType.IM:
      return IssueManagementApplications.includes(int.application as IntegrationTypes);
    case WorkflowIntegrationType.SCM:
      return SCMApplications.includes(int.application as IntegrationTypes);
    case WorkflowIntegrationType.CICD:
      return CICDApplications.includes(int.application as IntegrationTypes);
    default:
      return false;
  }
};

export const isJira = (application = "") => application === IntegrationTypes.JIRA;
export const isZendesk = (application = "") => application === IntegrationTypes.ZENDESK;
export const isAzure = (application = "") => application === IntegrationTypes.AZURE;
export const isHelix = (application = "") => application === IntegrationTypes.HELIX;
export const isJenkins = (application = "") => application === IntegrationTypes.JENKINS;
export const isTestrails = (application = "") => application === IntegrationTypes.TESTRAILS;
