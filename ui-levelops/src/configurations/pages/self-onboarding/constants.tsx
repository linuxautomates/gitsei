import React from "react";
import { ColumnProps } from "antd/lib/table";
import { WebRoutes } from "routes/WebRoutes";
import IntegrationConnectComponent from "./components/IntegrationConnectComponent";
import SelectRepositoriesComponent from "./components/select-repositories/SelectRepositoriesComponent";
import { AuthorizationConfigType, IntegrationStepsConfigType } from "./types/integration-step-components-types";
import { STARTS_WITH, CONTAINS } from "dashboard/constants/constants";
import {
  BITBUCKET_URL,
  GITHUB_DEFAULT_URL,
  GITHUB_NEW_TOKEN_URL,
  GITHUB_URL,
  GITLAB_URL
} from "constants/integrations";
import moment from "moment";
import { DateFormats } from "utils/dateUtils";
import SatelliteOptionsComponent from "./components/satellite-options-component/SatelliteOptionsComponent";
import SatelliteOptionFinalComponent from "./components/satellite-options-component/SatelliteOptionFinalComponent";
import { AntText } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import { capitalize } from "lodash";
import { DOCS_ROOT, DOCS_PATHS } from "constants/docsPath";
import { getBaseUrl } from "constants/routePaths";
import LocalStoreService from "services/localStoreService";
import { IntegrationTypes } from "constants/IntegrationTypes";
import envConfig from "env-config";

export enum SelfOnboardingFormFields {
  DORA_METRICS = "dora_metrics",
  DEFINITION_CONFIGURATIONS = "definition_configurations",
  INTEGRATION_NAME = "name",
  REPOS = "repos",
  INTEGRATION_DESCRIPTION = "description",
  INGEST_ALL_REPOS = "ingest_all_repos",
  SATELLITE_INTEGRATION = "satellite",
  INTEGRATION_URL = "url",
  PERSONAL_ACCESS_TOKEN = "pac",
  INTEGRATION_ID = "integration_id",
  VALID_NAME = "valid_name"
}

export const INTEGRATION_STEP_CONFIGS: Array<IntegrationStepsConfigType> = [
  {
    title: (integration: string) => `Connect To ${integration} Cloud`,
    nav_buttons: [],
    component: IntegrationConnectComponent
  },
  {
    title: "Name Integration & Select Repositories",
    nav_buttons: [
      {
        title: "Back",
        type: "default",
        onClick: (args: any) => () => {
          const { history, integration } = args;
          history?.push(WebRoutes.self_onboarding.root(integration, 0));
        }
      },
      {
        title: "Finish",
        type: "primary",
        hidden: (args: any) => {
          const { integrationId } = args;
          return !integrationId;
        },
        isDisabled: (args: any) => {
          const { name, validName } = args;
          return !name || !validName;
        },
        onClick: (args: any) => () => {
          const { onClick } = args;
          if (onClick) onClick();
        }
      }
    ],
    component: SelectRepositoriesComponent,
    required: [SelfOnboardingFormFields.INTEGRATION_NAME]
  },
  {
    title: "Satellite Options",
    titleClassName: "satellite-header",
    description: (
      <div className="description">
        <AntText>
          Please finish the installation of your satellite by following{" "}
          <a className="integration-doc-link" href={DOCS_ROOT + DOCS_PATHS.PROPELO_INGESTION_SATELLITE} target="_blank">
            these instructions
          </a>
          .
        </AntText>
        <br />
        <AntText>It may take upto 30 minutes to complete the connection.</AntText>
      </div>
    ),

    nav_buttons: [
      {
        title: "Back",
        type: "default",
        onClick: (args: any) => () => {
          const { history, integration } = args;
          history?.push(WebRoutes.self_onboarding.root(integration, 0));
        }
      },
      {
        title: "Cancel",
        type: "default",
        onClick: (args: any) => () => {
          const { history } = args;
          history.push(`${WebRoutes.integration.list()}?tab=your_integrations`);
        }
      },
      {
        title: "Next",
        type: "primary",
        isDisabled: (args: any) => {
          const { name, url, validName } = args;
          return !name || !validName || !url;
        },
        onClick: (args: any) => () => {
          const { onClick } = args;
          if (onClick) onClick();
        }
      }
    ],
    required: [SelfOnboardingFormFields.INTEGRATION_NAME, SelfOnboardingFormFields.INTEGRATION_URL],
    component: SatelliteOptionsComponent
  },
  {
    title: "Satellite Options",
    titleClassName: "satellite-header",
    description: (
      <div className="description">
        <AntText>
          Please finish the installation of your satellite by following{" "}
          <a className="integration-doc-link" href={DOCS_ROOT + DOCS_PATHS.PROPELO_INGESTION_SATELLITE} target="_blank">
            these instructions
          </a>
          .
        </AntText>
        <br />
        <AntText>It may take upto 30 minutes to complete the connection.</AntText>
      </div>
    ),

    nav_buttons: [
      {
        title: "Finish",
        type: "primary",
        onClick: (args: any) => () => {
          const { history, integration_id, dashboard_id } = args;
          const storage = new LocalStoreService();
          storage.setNewIntegrationAdded(integration_id);
          history.push(getBaseUrl());
        }
      }
    ],
    required: [SelfOnboardingFormFields.INTEGRATION_NAME, SelfOnboardingFormFields.INTEGRATION_URL],
    component: SatelliteOptionFinalComponent
  }
];

export const SELECT_REPOSITORIES_COLUMNS_CONFIG: Array<ColumnProps<any>> = [
  { title: "REPO", dataIndex: "name", key: "name", width: "25%", ellipsis: true },
  {
    title: "PATH",
    dataIndex: "url",
    key: "path",
    width: "55%",
    ellipsis: true,
    render: item => {
      if (item === NO_REPO_FOUND_TEXT) return <span style={{ color: "red" }}>{item}</span>;
      return item;
    }
  },
  {
    title: "LAST UPDATED AT",
    dataIndex: "updated_at",
    key: "last_updated_at",
    width: "20%",
    render: (item: number) => (item != 0 ? moment.unix(item).utc().format(DateFormats.DAY) : "")
  }
];

export const DORA_METRIC_DEFINITIONS: Array<{ label: string; value: string }> = [
  { label: "releases", value: "release_fields" },
  { label: "deployments", value: "deployment_fields" },
  { label: "hotfixes", value: "hotfix_fields" },
  { label: "defects", value: "defect_fields" }
];

export const SELF_ONBOARDING_INTEGRATION_FORM = "SELF_ONBOARDING_INTEGRATION_FORM";

export const INTEGRATION_BASED_ADDITIONAL_OPTIONS_MAP: Record<string, { [x: string]: string }> = {
  github: {
    is_push_based: "Connect via Github Webhook",
    fetch_prs: "Fetch PRs",
    fetch_issues: "Fetch Issues",
    fetch_projects: "Fetch Projects",
    fetch_commits: "Fetch Commits",
    fetch_commit_files: "Fetch Commit Files"
  },
  gitlab: {
    fetch_prs: "Fetch PRs",
    fetch_issues: "Fetch Issues",
    fetch_projects: "Fetch Projects",
    fetch_commits: "Fetch Commits",
    fetch_commit_files: "Fetch Commit Files"
  },
  bitbucket: {
    fetch_prs: "Fetch PRs",
    fetch_pr_reviews: "Fetch PR Reviews",
    fetch_commits: "Fetch Commits",
    fetch_commit_files: "Fetch Commit Files"
  }
};

export const DORA_METRIC_CONFIGURABLE_DEFINITIONS: Record<string, string> = {
  pull_request_to_branches: "Pull Requests to branches that",
  direct_merge_to_branches: "Direct merges to branches that",
  tags: "Tags that",
  labels_on_pull_request: "Labels on pull request that"
};

export const DEFINITION_PARTIAL_OPTIONS = [
  {
    label: "start with",
    value: STARTS_WITH
  },
  {
    label: "contain",
    value: CONTAINS
  }
];

export const INTEGRATION_SCOPE: Record<string, Array<string>> = {
  github: ["repo", "admin:org -> read:org"],
  [IntegrationTypes.GITHUB_ACTIONS]: ["repo", "admin:org -> read:org"],
  gitlab: ["api"]
};

export const getAuthorizationConfigs = (): Record<string, AuthorizationConfigType> => {
  const BITBUCKET_CLIENT_ID = envConfig.get("BITBUCKET_CLIENT_ID");
  const GITLAB_CLIENT_ID = envConfig.get("GITLAB_CLIENT_ID");
  const GITHUB_CLIENT_ID = envConfig.get("GITHUB_CLIENT_ID");
  return {
    [IntegrationTypes.GITHUB]: {
      oauth: GITHUB_URL,
      pac: GITHUB_NEW_TOKEN_URL,
      client_id: GITHUB_CLIENT_ID,
      scope: ["repo", "read:org"],
      default_url: GITHUB_DEFAULT_URL
    },
    [IntegrationTypes.GITHUB_ACTIONS]: {
      oauth: GITHUB_URL,
      pac: GITHUB_NEW_TOKEN_URL,
      client_id: GITHUB_CLIENT_ID,
      scope: ["repo", "read:org"],
      default_url: GITHUB_DEFAULT_URL
    },
    gitlab: {
      oauth: GITLAB_URL,
      pac: "https://gitlab.com/-/profile/personal_access_tokens",
      client_id: GITLAB_CLIENT_ID,
      scope: ["api"],
      default_url: "https://gitlab.com"
    },
    bitbucket: {
      oauth: BITBUCKET_URL,
      client_id: BITBUCKET_CLIENT_ID,
      scope: []
    }
  };
};

export const CLOUD_BASED_INTEGRATIONS: Array<string> = [
  IntegrationTypes.GITHUB_CLOUD,
  IntegrationTypes.BITBUCKET_CLOUD,
  IntegrationTypes.GITLAB_CLOUD,
  IntegrationTypes.GITHUB_ACTIONS
];
export const ENTERPRISE_BASED_INTEGRATIONS: Array<string> = [
  IntegrationTypes.BITBUCKET_ENTERPRISE,
  IntegrationTypes.GITLAB_ENTERPRISE
];
export const SELF_ONBOARDING_INTEGRATIONS: Array<string> = [
  IntegrationTypes.GITHUB,
  IntegrationTypes.BITBUCKET,
  IntegrationTypes.GITLAB,
  IntegrationTypes.GITHUB_ACTIONS
];
export const NEW_ONBOARDING_INTEGRATIONS: Array<string> = [IntegrationTypes.JIRA];
export const NO_REPO_FOUND_TEXT = "No repositories match this search.";

export const SCM_INTEGRATIONS_COLUMNS_CONFIGS: Array<ColumnProps<any>> = [
  {
    ...baseColumnConfig("Status", "success"),
    render: item => <span>{capitalize(item + "")}</span>,
    width: "2%"
  },
  baseColumnConfig("Issue", "exception")
];
