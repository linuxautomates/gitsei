import { baseColumnConfig } from "utils/base-table-config";
import { tableCell } from "utils/tableUtils";
import React from "react";
import { TitleWithCount } from "shared-resources/components";
import { Dict } from "../../../types/dict";
import { OUJiraFiltersConfig } from "./Filters/Jira/jira-filter-config";
import {
  OUAzureCICDCommonFiltersConfig,
  OUAzureCommonFiltersConfig,
  OUAzureIMCommonFiltersConfig,
  OUAzureSCMCommonFiltersConfig
} from "./Filters/azure/azure-filter-config";
import { OUPagerdutyCommonFiltersConfig } from "./Filters/pagerduty/pagerduty-filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { OUGerritCommonFiltersConfig } from "./Filters/gerrit/gerrit-filter.config";
import { OUSonarqubeCommonFiltersConfig } from "./Filters/sonarqube/sonarqube.filters-config";
import { OUSalesforceCommonFiltersConfig } from "./Filters/salesforce/salesforce.filter-config";
import { OUTestrailsCommonFiltersConfig } from "./Filters/testrails/testrails.filter-config";
import { OUCoverityCommonFiltersConfig } from "./Filters/coverity/coverity.filters-config";
import {
  OUDroneCircleJobsCommonFiltersConfig,
  OUGithubActionJobsCommonFiltersConfig,
  OUJenkinsJobsCommonFiltersConfig
} from "./Filters/jenkins/jenkins-job-filter.config";
import { OUSynkCommonFiltersConfig } from "./Filters/synk/synk-filter.config";
import { OUZendeskCommonFiltersConfig } from "./Filters/zendesk/zendesk.filter-config";
import { OUGitlabCommonFiltersCicdConfig, OUGitlabCommonFiltersConfig } from "./Filters/gitlab/gitlab-filter.config";
import { OUHarnessngJobsCommonFiltersConfig } from "./Filters/harnessng/harnessng-job-filter.config";

export enum OrganizationTabKey {
  ORGANIZATIONAL_Unit = "organizational_unit",
  GROUPS = "groups",
  USERS = "users",
  CUSTOM_PIVOT = "custom_pivot",
  BASIC_INFO = "basic_info",
  OU_DASHBOARDS = "ou_dashboards",
  OU_DEFINATIONS = "ou_definitions",
  CONTRIBUTORS_ROLE_PROFILE = "contributors_role_profile"
}

export enum CsvUploadType {
  REPLACE = "replace",
  UPDATE = "update"
}

export const ImportCsvsubHeading =
  "Contributors are autodiscovered through application integrations. They can also be added by importing a CSV file. The CSV file must contain the information listed below.";

export const configureAttributeSubHeading =
  "User attributes can be used to customize Collection. Add or edit them here.";

export const configureAttributeHint =
  "You can also export the existing user list (or a sample CSV file), add information and re-import the CSV.";

export const configureAttributeDefaultDataSource = [
  {
    key: "full_name",
    title: "Name",
    type: "text",
    description: "Name",
    default: true,
    required: true
  },
  {
    key: "email",
    title: "Email",
    type: "text",
    description: "Unique email address per member",
    default: true,
    required: true
  },
  {
    key: "integration",
    title: "Integration",
    type: "text",
    description: "Add a column for every integration",
    default: true,
    required: true
  }
];
export const configureAttributeDefaultDataSourceNew = [
  {
    key: "full_name",
    title: "Name",
    type: "text",
    description: "Name",
    default: true,
    required: true
  },
  {
    key: "email",
    title: "Email",
    type: "text",
    description: "Unique email address per member",
    default: true,
    required: true
  },
  {
    key: "integration",
    title: "Integration",
    type: "text",
    description: "Add a column for every integration",
    default: true,
    required: true
  },
  {
    key: "contributors_role",
    title: "Contributors Role",
    type: "text",
    description: "Add a Contributors Role for every integration",
    default: true,
    required: false
  }
];
export const OU_PROD_SCORE_ID = "OU_PROD_SCORE_ID";
export const ORG_UNIT_LIST_ID = "ORG_UNIT_LIST_ID";
export const ORG_UNIT_DELETE_ID = "ORG_UNIT_DELETE_ID";
export const ORG_UNIT_CLONE_ID = "ORG_UNIT_CLONE_ID";
export const ORG_UNIT_VERSION_LIST_ID = "ORG_UNIT_VERSION_LIST_ID";
export const ORGANIZATION_UNIT_NODE = "organization_unit_management";
export const NEW_ORG_UNIT_ID = "new_org_unit";
export const ORG_UNIT_DASHBOARDS_ASSOCIATION_ID = "ORG_UNIT_DASHBOARDS_ASSOCIATION_ID";
export const ORG_UNIT_FILTER_VALUES = "org_unit_filter_values_mapping";
export const ORG_UNIT_UTILITIES = "org_unit_utilities";
export const ORG_UNIT_LANDING_LIST_ID = "ORG_UNIT_LANDING_PAGE_LIST";

export const ouVersionTableConfig = [
  { ...baseColumnConfig("Versions", "version"), width: "5%" },
  { ...baseColumnConfig("Timestamp", "created_at"), render: (item: string) => tableCell("time_utc_f3", item) },
  {
    title: "",
    key: "id",
    dataIndex: "id",
    width: "8%"
  }
];

export const ouSelectedUserPreview = (count: number) => [
  {
    ...baseColumnConfig("Name", "name"),
    title: <TitleWithCount count={count} title="Name" titleClass="name-preview-column" />
  },
  baseColumnConfig("Email", "email")
];

export const orgUnitCSVFormatDataSource = [
  {
    key: 2,
    column: "Email",
    required: "Required",
    description: "Unique email address per member"
  },
  {
    key: 1,
    column: "Name",
    required: "Required",
    description: "Name"
  },
  {
    key: 3,
    column: "Integrations",
    required: "Optional",
    description: "Add a column for every integration"
  },
  {
    key: 4,
    column: "Start Date",
    required: "Optional",
    description: "Contributors start date"
  },
  {
    key: 4,
    column: "Custom",
    required: "Optional",
    description: "Add a column for every custom field. \n" + "For example, manager, tags, department."
  }
];

export const orgUnitCSVFormatColumns = [
  {
    title: "Column",
    dataIndex: "column",
    key: 1
  },
  {
    title: "Required",
    dataIndex: "required",
    key: 2
  },
  {
    title: "Description",
    dataIndex: "description",
    key: 3
  }
];

export const OUSupportedFiltersByApplication: { [x: string]: any } = {
  jira: {
    uri: "jira_filter_values",
    values: [
      "status",
      "priority",
      "issue_type",
      "assignee",
      "project",
      "component",
      "label",
      "reporter",
      // removing fix_version and version as a temp fix for now
      // "fix_version",
      // "version",
      "resolution",
      "status_category"
    ]
  },
  azure_devops: [
    {
      uri: "issue_management_workitem_values",
      values: [
        "workitem_project",
        "workitem_status",
        "workitem_priority",
        "workitem_type",
        "workitem_status_category",
        "workitem_parent_workitem_id",
        "workitem_epic",
        "workitem_assignee",
        "workitem_version",
        "workitem_fix_version",
        "workitem_reporter",
        "workitem_label"
      ]
    },
    {
      uri: "github_prs_filter_values",
      values: ["approver", "assignee", "creator", "reviewer", "repo_id", "state", "branch", "label"]
    },
    {
      uri: "github_commits_filter_values",
      values: ["committer", "author"]
    },
    {
      uri: "jenkins_jobs_filter_values",
      values: ["cicd_user_id", "job_status", "job_name", "instance_name", "job_normalized_full_name"]
    },
    {
      uri: "cicd_filter_values",
      values: ["project_name"]
    }
  ],
  github: [
    {
      uri: "github_prs_filter_values",
      values: ["approver", "assignee", "creator", "reviewer", "repo_id", "state", "branch", "label", "project"]
    },
    {
      uri: "github_commits_filter_values",
      values: ["committer", "author"]
    }
  ],
  jenkins: {
    uri: "jenkins_jobs_filter_values",
    values: ["cicd_user_id", "job_status", "job_name", "project_name", "instance_name", "job_normalized_full_name"]
  },
  pagerduty: {
    uri: "pagerduty_filter_values",
    values: ["pd_service", "incident_priority", "incident_urgency", "alert_severity", "user_id"]
  }
};

export const orgFilterNameMapping: Dict<string, string> = {
  workitem_version: "Affects Version",
  workitem_parent_workitem_id: "Parent Workitem id",
  workitem_sprint_full_names: "Azure Iteration",
  repo_id: "Repo",
  instance_name: "Instance Name",
  job_normalized_full_name: "Jenkins Job Path",
  project_name: "Project"
};

export const FILTERS_CONFIG = {
  [IntegrationTypes.JIRA]: OUJiraFiltersConfig,
  [IntegrationTypes.AZURE]: OUAzureCommonFiltersConfig,
  [IntegrationTypes.PAGERDUTY]: OUPagerdutyCommonFiltersConfig,
  [IntegrationTypes.GITHUB]: OUGerritCommonFiltersConfig,
  [IntegrationTypes.GERRIT]: OUGerritCommonFiltersConfig,
  [IntegrationTypes.BITBUCKET_SERVER]: OUGerritCommonFiltersConfig,
  [IntegrationTypes.GITLAB]: OUGitlabCommonFiltersConfig,
  [IntegrationTypes.SONARQUBE]: OUSonarqubeCommonFiltersConfig,
  [IntegrationTypes.SALESFORCE]: OUSalesforceCommonFiltersConfig,
  [IntegrationTypes.TESTRAILS]: OUTestrailsCommonFiltersConfig,
  [IntegrationTypes.HELIX]: OUGerritCommonFiltersConfig,
  [IntegrationTypes.COVERITY]: OUCoverityCommonFiltersConfig,
  [IntegrationTypes.JENKINS]: OUJenkinsJobsCommonFiltersConfig,
  [IntegrationTypes.SNYK]: OUSynkCommonFiltersConfig,
  [IntegrationTypes.ZENDESK]: OUZendeskCommonFiltersConfig,
  [IntegrationTypes.BITBUCKET]: OUGerritCommonFiltersConfig,
  [IntegrationTypes.CIRCLECI]: OUDroneCircleJobsCommonFiltersConfig,
  [IntegrationTypes.DRONECI]: OUDroneCircleJobsCommonFiltersConfig,
  [IntegrationTypes.HARNESSNG]: OUHarnessngJobsCommonFiltersConfig,
  azure_devops_im: OUAzureIMCommonFiltersConfig,
  azure_devops_cicd: OUAzureCICDCommonFiltersConfig,
  azure_devops_scm: OUAzureSCMCommonFiltersConfig,
  [IntegrationTypes.GITHUB_ACTIONS]: OUGithubActionJobsCommonFiltersConfig,
  [IntegrationTypes.GITLAB_CICD]: OUGitlabCommonFiltersCicdConfig
};

export const INTEGRATION_WITH_NEW_FILTER_FLOW = [
  IntegrationTypes.JIRA,
  IntegrationTypes.AZURE,
  IntegrationTypes.GERRIT,
  IntegrationTypes.BITBUCKET_SERVER,
  IntegrationTypes.GITHUB,
  IntegrationTypes.GITLAB,
  IntegrationTypes.PAGERDUTY,
  IntegrationTypes.SONARQUBE,
  IntegrationTypes.SALESFORCE,
  IntegrationTypes.TESTRAILS,
  IntegrationTypes.HELIX,
  IntegrationTypes.COVERITY,
  IntegrationTypes.JENKINS,
  IntegrationTypes.SNYK,
  IntegrationTypes.ZENDESK,
  IntegrationTypes.BITBUCKET,
  IntegrationTypes.CIRCLECI,
  IntegrationTypes.DRONECI,
  IntegrationTypes.CXSAST,
  IntegrationTypes.POSTGRESS,
  IntegrationTypes.SLACK,
  IntegrationTypes.SPLUNK,
  IntegrationTypes.HARNESSNG,
  IntegrationTypes.GITHUB_ACTIONS
];

export const ORG_UNIT_CONFIGURATION_TABS_CONFIG = [
  {
    label: "Basic Info",
    tab_key: OrganizationTabKey.BASIC_INFO
  },
  { label: "Insights", tab_key: OrganizationTabKey.OU_DASHBOARDS },
  { label: "Definition", tab_key: OrganizationTabKey.OU_DEFINATIONS }
];

export const ORG_UNIT_CONFIGURATION_TABS_CONFIG_NEW = [
  {
    label: "Basic Info",
    tab_key: OrganizationTabKey.BASIC_INFO
  },
  { label: "Insights", tab_key: OrganizationTabKey.OU_DASHBOARDS },
  { label: "Definition", tab_key: OrganizationTabKey.OU_DEFINATIONS },
  { label: "Trellis Contributor Scoring", tab_key: OrganizationTabKey.CONTRIBUTORS_ROLE_PROFILE }
];

export const PIVOT_LIST_ID = "PIVOT_LIST_ID";
export const LANDING_PAGE_PIVOT_LIST_ID = "LANDING_PAGE_PIVOT_LIST_ID";
export const OU_DASHBOARDS_LIST_ID = "OU_DASHBOARDS_LIST_ID";
export const PREDEFINED_PIVOTES = ["Teams", "Sprints", "Projects"];
export const PREDEFINED_DASHBOARDS = ["Alignment", "Planning", "Execution"];
export const MIN_DASHBOARD_COUNT = 3;
export const DROPDOWN_DASH = "DROPDOWN_DASH";
export const DASHBOARD_ELIPSIS_LENGTH = 20;
export const DASHBOARD_DROPDOWN_LIST_ELLIPSIS_LENGTH = 24;
export const OU_DASHBOARD_LIST_ID = "OU_DASHBOARD_LIST_ID";
export const OU_DASHBOARD_SEARCH_LIST_ID = "OU_DASHBOARD_SEARCH_LIST_ID";
export const ORG_UNIT_CATEGORY_PARENT_NODE_WARNING = `The changes you are trying to make to the category and/or the parent node for this collection will also apply to all of its child collection nodes.\n\nDo you still wish to proceed?`;
export const ORG_UNIT_ASSOCIATED_DASHBOARD_CHANGE_WARNING =
  "Changes to the insight association of this collection will also be applied to the child collections.\n\nAre you sure you want to proceed?";
export const ORG_UNIT_LIST_DASHBOARD_COUNT_COLUMN_INFO =
  "Indicates the number of insights specifically associated with a collection, not including inherited insights.";
export const ALLOWED_ORG_UNIT_NAME_LENGTH = 100;
export const OU_NAME_EXISTS_WARNING = "This collection name already exist!";
export const OU_NAME_TOO_LARGE_TEXT = "This collection name is too long!";
export const OU_NAME_INVALID_CHARS_EXISTS_WARNING = "/ or \\ not allowed in collection name!";
export const TRELLIS_TOOLTIP = "Enabling Trellis Scoring allows you to score contributors within this collection";
export const TRELLIS_LABEL = "Trellis Contributor Scoring";
