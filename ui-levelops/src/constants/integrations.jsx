import React from "react";
import githubLogo from "assets/img/integrations/github-logo-icon.png";
import tenableLogo from "assets/img/integrations/github-logo-icon.png"; // just for development
// import tenableLogo from "assets/img/integrations/tenable-logo-icon.png"; // not working
import jiraLogo from "assets/img/integrations/jira-logo-icon.jpeg";
import slackLogo from "../assets/img/integrations/slack-logo-icon.png";
import salesforceLogo from "../assets/img/integrations/logo-salesforce.svg";
import gerritLogo from "../assets/img/integrations/gerrit_icon.svg";
import bitbucketLogo from "../assets/img/integrations/bitbucket-logo.svg";
import sonarqubeLogo from "../assets/img/integrations/sonarqube.png";
import testrailLogo from "../assets/img/integrations/testrail.png";
import checkmarxLogo from "../assets/img/integrations/checkmarx.png";
import azureDevopsLogo from "../assets/img/integrations/azure-devops.png";
import gitlabLogo from "../assets/img/integrations/gitlab-icon.svg";
import coverityLogo from "../assets/img/integrations/coverity.svg";
import droneciLogo from "../assets/img/integrations/droneci-logo-icon.svg";
import msteamsLogo from "../assets/img/integrations/msteams-logo-icon.svg";
import harnessLogo from "../assets/img/integrations/harness-logo-icon.svg";
import ZendeskLogo from "../shared-resources/components/integration-icons/icon-path/zendesk.path.svg";
import CircleCI from "../shared-resources/components/integration-icons/icon-path/circleci.path.svg";
import githubActionsLogo from "../assets/img/integrations/github-actions-logo-icon.svg";
import { unset, forEach, get } from "lodash";
import { SENSITIVE_FIELDS_KEY } from "configurations/containers/integration-steps/integrations-details-new/integration.constants";
import { sanitizeObject } from "utils/commonUtils";
import {
  AZURE_DESC,
  BITBUCKET_DESC,
  CHECKMARX_DESC,
  CIRCLECI_DESC,
  COVERITY_DESC,
  GERRIT_DESC,
  GITHUB_DESC,
  GITHUB_REPO_DESC,
  GITLAB_DESC,
  JIRA_DESC,
  JIRA_JQL_DESC,
  JIRA_OPTIONS_DESC,
  PAGERDUTY_DESC,
  PERFORCE_HELIX_SATELLITE_DESC,
  POSTGRES_DESC,
  PREFORCE_HELIX_DESC,
  SALESFORCE_DESC,
  SLACK_DESC,
  SNYK_DESC,
  SONARQUBE_ORGANIZATION_DESC,
  SONARQUBE_PROJECT_KEYS_DESC,
  SPLUNK_DESC,
  TENABLE_DESC,
  TESTRAILS_DESC,
  ZENDESK_DESC,
  DRONECI_DESC,
  DRONECI_REPO_DESC,
  CIRCLECI_REPO_DESC,
  MS_TEAMS_DESC,
  HARNESS_DESC,
  HARNESS_ORGANIZATION_DESC,
  HARNESS_PROJECT_DESC
} from "./integration.description";
import { timezones } from "../utils/timezones.utils";
import { BASE_UI_URL } from "helper/envPath.helper";
import { DOCS_PATHS, DOCS_ROOT } from "./docsPath";
import envConfig from "env-config";
import { IntegrationTypes } from "./IntegrationTypes";
import { getIsStandaloneApp } from "helper/helper";

export const OAUTH = "oauth";
export const FORM = "form";
export const HYBRID = "hybrid";

export const INTEGRATIONS = [
  { value: IntegrationTypes.TENABLE, label: "Tenable" },
  { value: IntegrationTypes.GITHUB, label: "GitHub" },
  { value: IntegrationTypes.JIRA, label: "Jira" },
  { value: IntegrationTypes.CONFLUENCE, label: "Confluence" },
  { value: IntegrationTypes.SERVICE_NOW, label: "Service Now" },
  { value: IntegrationTypes.AWS, label: "AWS" }
];

// IntegrationSteps Oauth related constants
export const GITHUB_URL = "https://github.com/login/oauth/authorize";
export const GITHUB_NEW_TOKEN_URL = "https://github.com/settings/tokens/new";
export const GITHUB_DEFAULT_URL = "https://api.github.com";

export const JIRA_URL = "https://auth.atlassian.com/authorize";

export const SLACK_URL = "https://slack.com/oauth/v2/authorize";

export const BITBUCKET_URL = "https://bitbucket.org/site/oauth2/authorize";

export const SALESFORCE_URL = "https://login.salesforce.com/services/oauth2/authorize";

export const GITLAB_URL = "https://gitlab.com/oauth/authorize";

export const AZURE_DEVOPS_URL = "https://app.vssps.visualstudio.com/oauth2/authorize";

export const DRONECI_URL = "https://drone.io";

export const MS_TEAMS_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";

export const HARNESS_URL = "https://app.harness.io";

export const AUTOMATIC_DOCS_URL = DOCS_ROOT + DOCS_PATHS.AUTOMATIC_INTEGRATIONS;
export const SEMI_AUTOMATIC_DOCS_URL = DOCS_ROOT + DOCS_PATHS.SEMI_AUTOMATIC_INTEGRATIONS;

export const stringFields = ["url", "username", "apikey", "ip", "server"];

export const ALL_SCM_INTEGRATIONS = [
  IntegrationTypes.GITHUB_CLOUD,
  IntegrationTypes.GITHUB_ENTERPRISE,
  IntegrationTypes.GERRIT,
  IntegrationTypes.BITBUCKET_CLOUD,
  IntegrationTypes.BITBUCKET_ENTERPRISE,
  IntegrationTypes.AZURE,
  IntegrationTypes.GITLAB_CLOUD,
  IntegrationTypes.GITLAB_ENTERPRISE,
  IntegrationTypes.DRONECI,
  IntegrationTypes.GITHUB_ACTIONS
];
const redirect_uri = integraion =>
  getIsStandaloneApp()
    ? BASE_UI_URL.concat("/integration-callback")
    : window.location.origin.concat("/api/sei-integration/redirect/").concat(integraion);

export const getIntegrationUrlMap = () => {
  const SLACK_CLIENT_ID = envConfig.get("SLACK_CLIENT_ID");
  const BITBUCKET_CLIENT_ID = envConfig.get("BITBUCKET_CLIENT_ID");
  const SALESFORCE_CLIENT_ID = envConfig.get("SALESFORCE_CLIENT_ID");
  const GITLAB_CLIENT_ID = envConfig.get("GITLAB_CLIENT_ID");
  const AZURE_DEVOPS_CLIENT_ID = envConfig.get("AZURE_DEVEOPS_CLIENT_ID");
  const MS_TEAMS_CLIENT_ID = envConfig.get("MS_TEAMS_CLIENT_ID");
  return {
    [IntegrationTypes.TENABLE]: {
      type: FORM,
      form_fields: [
        { key: "url", label: "Url" },
        { key: "username", label: "Username" },
        { key: "apikey", label: "Apikey", type: "password" }
      ],
      url: "",
      client_id: "",
      logo: tenableLogo,
      description: TENABLE_DESC,
      docs_url_slug: "automated-integrations/tenable/",
      icon: "tenable",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/tenable/`,
      application: IntegrationTypes.TENABLE,
      notFullySupportedIntegration: true
    },
    [IntegrationTypes.GITHUB_CLOUD]: {
      title: "GitHub Cloud",
      type: FORM,
      form_fields: [
        { key: "url", label: "Url", defaultValue: "https://api.github.com" },
        {
          key: "keys",
          label: "Personal Access Token",
          arrayType: true,
          fields: [
            { key: "username", label: "User Name" },
            { key: "apikey", label: "Integration API Key", type: "password" }
          ],
          defaultValue: [],
          required: true
        }
      ],
      metadata_fields: [
        {
          key: "repos",
          label: "Repositories",
          type: "comma-multi-select",
          description: GITHUB_REPO_DESC,
          required: false
        },
        {
          key: "options",
          label: "Options",
          type: "checkbox-group",
          required: false,
          // If new options are added, please update metaOptions in mapIntegrationForm as well...
          options: [
            { value: "is_push_based", label: "Connect via Github Webhook" },
            { value: "fetch_commits", label: "Fetch Commits" },
            { value: "fetch_prs", label: "Fetch PRs" },
            { value: "fetch_issues", label: "Fetch Issues" },
            { value: "fetch_projects", label: "Fetch Projects" }
          ],
          hiddenOptions: [
            {
              key: "hiddenOptions",
              label: "",
              type: "checkbox-group",
              required: false,
              isParentChecked: "fetch_commits",
              options: [{ value: "fetch_commit_files", label: "Fetch Commit Files" }]
            }
          ]
        }
      ],
      url: "https://api.github.com",
      client_id: "",
      logo: githubLogo,
      description: GITHUB_DESC,
      docs_url_slug: "automated-integrations/github/",
      icon: "github",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/github/`,
      application: IntegrationTypes.GITHUB,
      mapIntegrationForm: form => {
        // Options fields
        const metaOptions = [
          "is_push_based",
          "fetch_commits",
          "fetch_prs",
          "fetch_issues",
          "fetch_projects",
          "fetch_commit_files"
        ];
        form.metadata = {
          ...(form.metadata || {}),
          ...metaOptions.reduce((acc, option) => ({ ...acc, [option]: !!(form.metadata || {})[option] }), {})
        };
        return form;
      },
      support_multiple_api_keys: true,
      mapDataForYAML: form => {
        // transforming the keys fields
        form.keys = form.keys.map(field => ({ api_key: field.apikey, user_name: field.username }));
        return form;
      },
      displayForTrialUser: false
    },
    [IntegrationTypes.GITHUB_ENTERPRISE]: {
      title: "GitHub Enterprise",
      type: FORM,
      form_fields: [
        { key: "url", label: "Url", defaultValue: "https://api.github.com" },
        {
          key: "keys",
          label: "Personal Access Token",
          arrayType: true,
          fields: [
            { key: "username", label: "User Name" },
            { key: "apikey", label: "Integration API Key", type: "password" }
          ],
          defaultValue: [],
          required: true
        }
      ],
      metadata_fields: [
        {
          key: "repos",
          label: "Repositories",
          type: "comma-multi-select",
          description: GITHUB_REPO_DESC,
          required: false
        },
        {
          key: "options",
          label: "Options",
          type: "checkbox-group",
          required: false,
          // If new options are added, please update metaOptions in mapIntegrationForm as well...
          options: [
            { value: "is_push_based", label: "Connect via Github Webhook" },
            { value: "fetch_commits", label: "Fetch Commits", defaultValue: true },
            { value: "fetch_prs", label: "Fetch PRs", defaultValue: true },
            { value: "fetch_issues", label: "Fetch Issues", defaultValue: true },
            { value: "fetch_projects", label: "Fetch Projects" }
          ],
          hiddenOptions: [
            {
              key: "hiddenOptions",
              label: "",
              type: "checkbox-group",
              required: false,
              isParentChecked: "fetch_commits",
              options: [{ value: "fetch_commit_files", label: "Fetch Commit Files" }]
            },
            {
              key: "autoRegister",
              label: "",
              type: "checkbox-group",
              required: false,
              isParentChecked: "is_push_based",
              options: [{ value: "auto_register_webhook", label: "Auto Register Webhook", defaultValue: true }]
            }
          ]
        }
      ],
      url: "https://api.github.com",
      client_id: "",
      logo: githubLogo,
      description: GITHUB_DESC,
      docs_url_slug: "automated-integrations/github/",
      icon: "github",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/github/`,
      application: IntegrationTypes.GITHUB,
      mapIntegrationForm: form => {
        // Options fields
        const metaOptions = [
          "is_push_based",
          "fetch_commits",
          "fetch_prs",
          "fetch_issues",
          "fetch_projects",
          "fetch_commit_files",
          "auto_register_webhook"
        ];
        form.metadata = {
          ...(form.metadata || {}),
          ...metaOptions.reduce((acc, option) => ({ ...acc, [option]: !!(form.metadata || {})[option] }), {})
        };
        return form;
      },
      support_multiple_api_keys: true,
      mapDataForYAML: form => {
        // transforming the keys fields
        form.keys = form.keys.map(field => ({ api_key: field.apikey, user_name: field.username }));
        return form;
      }
    },
    // [IntegrationTypes.SERVICE_NOW]: {
    //     type: HYBRID,
    //     form_fields: ['client_id','client_secret','url','username','password'],
    //     url: "",
    //     client_id: "",
    //     uri: "oauth_auth.do",
    //     query_params: {
    //         response_type: "code",
    //         client_id: null // whatever is null in query params for hybrid types, it will pick up from form_fields
    //     },
    //     logo: servicenow
    //
    // },
    // aws: {
    //     type: FORM,
    //     form_fields: ['account_number','external_arn'],
    //     url: "",
    //     client_id: "",
    //     logo: aws
    // },
    [IntegrationTypes.CUSTOM]: {
      type: FORM,
      form_fields: [],
      url: "",
      satellite: true,
      logo: "levelops",
      description: "Custom Integration",
      application: IntegrationTypes.CUSTOM
    },
    [IntegrationTypes.JIRA]: {
      type: FORM,
      form_fields: [
        { key: "url", label: "Url" },
        { key: "username", label: "Username" },
        { key: "apikey", label: "Apikey", type: "password" }
      ],
      metadata_fields: [
        {
          key: "jql",
          label: "Custom JQL",
          type: "text",
          description: JIRA_JQL_DESC,
          required: false
        },
        {
          key: "timezone",
          label: "Timezone",
          type: "single-select",
          required: false,
          options: timezones.map(timezone => timezone.label),
          showSearch: true
        },
        {
          key: "options",
          label: "Exclude Sensitive Fields",
          type: "checkbox-group",
          required: false,
          description: JIRA_OPTIONS_DESC,
          options: [
            { value: "summary", label: "Summary" },
            { value: "description", label: "Description" },
            { value: "comment", label: "Comments" }
          ]
        }
      ],
      mapIntegrationForm: form => {
        let mappedForm = { ...form };
        const sensitiveFields = [];
        forEach(Object.keys(mappedForm?.metadata || {}), key => {
          if (key === "summary" || key === "description" || key === "comment") {
            sensitiveFields.push(key);
            unset(mappedForm, ["metadata", key]);
          }
        });
        mappedForm = {
          ...mappedForm,
          metadata: { ...(mappedForm?.metadata || {}), [SENSITIVE_FIELDS_KEY]: sensitiveFields.join(",") }
        };
        return mappedForm;
      },
      mapDataForYAML: form => {
        let mappedForm = { ...form };
        const sensitiveFields = get(mappedForm, ["metadata", SENSITIVE_FIELDS_KEY], "");
        mappedForm = {
          ...mappedForm,
          metadata: sanitizeObject({
            ...(mappedForm?.metadata || {}),
            [SENSITIVE_FIELDS_KEY]: sensitiveFields?.length > 0 ? sensitiveFields?.split(",") : []
          })
        };
        if (Object.keys(mappedForm?.metadata || {}).length === 0) {
          unset(mappedForm, ["metadata"]);
        }
        return mappedForm;
      },
      url: "",
      client_id: "",
      logo: jiraLogo,
      description: JIRA_DESC,
      docs_url_slug: "automated-integrations/jira/",
      icon: "jira",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/jira/`,
      application: IntegrationTypes.JIRA
    },
    [IntegrationTypes.TESTRAILS]: {
      title: "TestRail",
      type: FORM,
      form_fields: [
        { key: "url", label: "Url" },
        { key: "username", label: "Username" },
        { key: "apikey", label: "Apikey", type: "password" }
      ],
      url: "",
      client_id: "",
      logo: testrailLogo,
      description: TESTRAILS_DESC,
      // instruction @TODO: change when ready
      instructions: (
        <small>
          Create an apikey token{" "}
          <a href="#" target="_blank">
            here
          </a>
          <br />
          Verify that the user has read access to all the projects needed
        </small>
      ),
      icon: "testrail",
      application: IntegrationTypes.TESTRAILS,
      docs_url_slug: "automated-integrations/testrail/",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/testrail/`,
      bypassTitleTransform: true
    },
    [IntegrationTypes.SONARQUBE]: {
      type: FORM,
      form_fields: [
        { key: "url", label: "Url" },
        { key: "username", label: "Username" },
        { key: "apikey", label: "Apikey", type: "password" }
      ],
      metadata_fields: [
        {
          key: "organization",
          label: "Collection",
          type: "text",
          description: SONARQUBE_ORGANIZATION_DESC,
          required: false
        },
        {
          key: "project_keys",
          label: "Project Keys",
          type: "comma-multi-select",
          description: SONARQUBE_PROJECT_KEYS_DESC,
          required: false
        }
      ],
      url: "",
      logo: sonarqubeLogo,
      client_id: "",
      description: `SonarQube empowers all developers to write cleaner and safer code.`,
      // instruction @TODO: change when ready
      instructions: (
        <small>
          Create an apikey token{" "}
          <a href="#" target="_blank">
            here
          </a>
          <br />
          Verify that the user has read access to all the projects needed
        </small>
      ),
      icon: "sonarqube",
      application: IntegrationTypes.SONARQUBE,
      docs_url_slug: "automated-integrations/sonarqube/",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/sonarqube/`
    },
    [IntegrationTypes.PERFORCE_HELIX_SERVER]: {
      type: FORM,
      firstStepTitle: "Perforce Helix Core",
      newDetailsPage: true,
      supportCancel: true,
      finalStepButtontext: "Finish",
      backButtonText: "Back",
      stepButtonBothEnd: false,
      newDetailsData: {
        heading: "Satellite Integration",
        description: PERFORCE_HELIX_SATELLITE_DESC,
        buttonText: "Submit and download the configuration file"
      },
      form_fields: [
        {
          key: "ip",
          label: "Helix Core Server",
          helpText: "Server name or IP address.",
          errorText: "This Field is Required"
        },
        {
          key: "port",
          label: "Helix Core Port",
          defaultValue: 1666,
          helpText: "Port of Helix server",
          errorText: "Invalid port number"
        },
        { key: "username", label: "Username" },
        { key: "apikey", label: "Password", type: "password", required: false, defaultValue: "" }
      ],
      metadata_fields: [
        {
          key: "helix_swarm_url",
          label: "Helix Swarm Url",
          type: "text",
          placeholder: "https://yourintegrationurl.com",
          required: false
        },
        {
          key: "options",
          label: "Options",
          type: "checkbox-group",
          required: false,
          options: [{ value: "ssl_enabled", label: "Server uses SSL" }]
        }
      ],
      mapIntegrationForm: form => {
        let mappedForm = { ...form };
        const sslCheck = mappedForm?.metadata && mappedForm.metadata["ssl_enabled"];
        const prefixurl = sslCheck ? "p4javassl://" : "p4java://";
        mappedForm["url"] = prefixurl + mappedForm.ip + ":" + mappedForm.port;
        if (mappedForm?.metadata && sslCheck && mappedForm.metadata["ssl_fingerprint"]?.length) {
          mappedForm.metadata["ssl_auto_accept"] = false;
        } else {
          mappedForm = {
            ...mappedForm,
            metadata: {
              ...(mappedForm.metadata || {}),
              ssl_auto_accept: true
            }
          };
        }
        delete mappedForm["ip"];
        delete mappedForm["port"];
        return mappedForm;
      },
      application: IntegrationTypes.HELIX,
      isFormattedDescription: true,
      description: PREFORCE_HELIX_DESC,
      docs_url_slug: "automated-integrations/helix/",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/helix/`,
      notFullySupportedIntegration: true
    },
    // [CONFLUENCE]: {
    //     type: FORM,
    //     form_fields:["url","username","apikey"],
    //     url: "",
    //     client_id:"",
    //     logo: confluenceLogo
    // },
    [IntegrationTypes.SLACK]: {
      type: OAUTH,
      url: SLACK_URL,
      client_id: SLACK_CLIENT_ID,
      form_fields: [],
      query_params: {
        client_id: SLACK_CLIENT_ID,
        scope:
          "app_mentions:read,channels:history,channels:join,chat:write,commands,files:write,groups:history,im:history,im:write,mpim:history,users:read,users:read.email,chat:write.customize",
        redirect_uri: redirect_uri(IntegrationTypes.SLACK)
      },
      logo: slackLogo,
      description: SLACK_DESC,
      icon: "slack",
      docs_url_slug: "automated-integrations/slack/",
      application: IntegrationTypes.SLACK
    },
    [IntegrationTypes.SALESFORCE]: {
      type: OAUTH,
      url: SALESFORCE_URL,
      client_id: SALESFORCE_CLIENT_ID,
      form_fields: [],
      query_params: {
        client_id: SALESFORCE_CLIENT_ID,
        // scope: "bot,chat:write:bot,users:read.email,users:read,im:write",
        response_type: "code",
        redirect_uri: redirect_uri(IntegrationTypes.SALESFORCE)
      },
      logo: salesforceLogo,
      description: SALESFORCE_DESC,
      icon: "salesforce",
      docs_url_slug: "automated-integrations/salesforce/",
      application: IntegrationTypes.SALESFORCE,
      notFullySupportedIntegration: true
    },
    [IntegrationTypes.SNYK]: {
      type: FORM,
      form_fields: [
        { key: "url", label: "Url" },
        { key: "username", label: "Username" },
        { key: "apikey", label: "Apikey", type: "password" }
      ],
      url: "",
      client_id: "",
      logo: jiraLogo,
      description: SNYK_DESC,
      docs_url_slug: "automated-integrations/snyk/",
      icon: "snyk",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/snyk/`,
      application: IntegrationTypes.SNYK,
      notFullySupportedIntegration: true
    },
    [IntegrationTypes.PAGERDUTY]: {
      type: FORM,
      form_fields: [
        { key: "url", label: "Url" },
        { key: "username", label: "Username" },
        { key: "apikey", label: "Apikey", type: "password" }
      ],
      url: "",
      client_id: "",
      logo: jiraLogo,
      description: PAGERDUTY_DESC,
      docs_url_slug: "automated-integrations/pagerduty/",
      icon: "pagerduty",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/pagerduty/`,
      application: IntegrationTypes.PAGERDUTY,
      notFullySupportedIntegration: true
    },
    [IntegrationTypes.SPLUNK]: {
      type: FORM,
      form_fields: [
        { key: "url", label: "Url" },
        { key: "username", label: "Username" },
        { key: "apikey", label: "Apikey", type: "password" }
      ],
      metadata_fields: [
        {
          key: "options",
          label: "Options",
          type: "checkbox-group",
          options: [
            { value: "ignore_server_cert", label: "Ignore Server Cert" },
            { value: "is_splunk_cloud", label: "Is Splunk Cloud" }
          ]
        }
      ],
      url: "",
      client_id: "",
      logo: "",
      description: SPLUNK_DESC,
      docs_url_slug: "automated-integrations/splunk/",
      icon: "splunk",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/splunk/`,
      application: IntegrationTypes.SPLUNK,
      notFullySupportedIntegration: true
    },
    [IntegrationTypes.POSTGRES]: {
      type: FORM,
      form_fields: [
        { key: "company", label: "Company" },
        { key: "server", label: "Server" },
        { key: "username", label: "Username" },
        { key: "password", label: "Password", type: "password" },
        { key: "database", label: "Database" }
      ],
      url: "",
      client_id: "",
      logo: "",
      description: POSTGRES_DESC,
      docs_url_slug: "automated-integrations/postgresql/",
      icon: "postgres",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/postgres/`,
      application: IntegrationTypes.POSTGRES,
      notFullySupportedIntegration: true
    },
    [IntegrationTypes.ZENDESK]: {
      type: FORM,
      form_fields: [
        { key: "url", label: "Url" },
        { key: "username", label: "Username" },
        { key: "apikey", label: "Apikey", type: "password" }
      ],
      metadata_fields: [
        {
          key: "options",
          label: "Options",
          type: "checkbox-group",
          options: [{ value: "jiralinks_enabled", label: "Jira Links" }]
        }
      ],
      url: "",
      client_id: "",
      logo: ZendeskLogo,
      description: ZENDESK_DESC,
      icon: "zendesk",
      docs_url_slug: "automated-integrations/zendesk/",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/zendesk/`,
      application: IntegrationTypes.ZENDESK,
      notFullySupportedIntegration: true
    },
    [IntegrationTypes.GERRIT]: {
      type: FORM,
      form_fields: [
        { key: "url", label: "Url" },
        { key: "username", label: "Username" },
        { key: "apikey", label: "Apikey", type: "password" }
      ],
      url: "",
      client_id: "",
      logo: gerritLogo,
      description: GERRIT_DESC,
      docs_url_slug: "automated-integrations/gerrit/",
      icon: "gerrit",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/gerrit/`,
      application: IntegrationTypes.GERRIT,
      notFullySupportedIntegration: true
    },
    [IntegrationTypes.BITBUCKET_CLOUD]: {
      type: OAUTH,
      url: BITBUCKET_URL,
      client_id: BITBUCKET_CLIENT_ID,
      form_fields: [],
      query_params: {
        client_id: BITBUCKET_CLIENT_ID,
        response_type: "code",
        redirect_uri: redirect_uri(IntegrationTypes.BITBUCKET)
      },
      logo: bitbucketLogo,
      description: BITBUCKET_DESC,
      icon: "bitbucket",
      docs_url_slug: "automated-integrations/bitbucket/",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/bitbucket/`,
      application: IntegrationTypes.BITBUCKET,
      mapIntegrationForm: form => {
        // Options fields
        const metaOptions = ["fetch_commits", "fetch_prs", "fetch_pr_reviews", "fetch_commit_files"];
        form.metadata = {
          ...(form.metadata || {}),
          ...metaOptions.reduce((acc, option) => ({ ...acc, [option]: !!(form.metadata || {})[option] }), {})
        };
        return form;
      },
      displayForTrialUser: false
    },
    [IntegrationTypes.BITBUCKET_ENTERPRISE]: {
      type: OAUTH,
      url: BITBUCKET_URL,
      client_id: BITBUCKET_CLIENT_ID,
      form_fields: [],
      query_params: {
        client_id: BITBUCKET_CLIENT_ID,
        response_type: "code",
        redirect_uri: redirect_uri(IntegrationTypes.BITBUCKET)
      },
      logo: bitbucketLogo,
      description: BITBUCKET_DESC,
      icon: "bitbucket",
      docs_url_slug: "automated-integrations/bitbucket/",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/bitbucket/`,
      application: IntegrationTypes.BITBUCKET,
      mapIntegrationForm: form => {
        // Options fields
        const metaOptions = ["fetch_commits", "fetch_prs", "fetch_pr_reviews", "fetch_commit_files"];
        form.metadata = {
          ...(form.metadata || {}),
          ...metaOptions.reduce((acc, option) => ({ ...acc, [option]: !!(form.metadata || {})[option] }), {})
        };
        return form;
      }
    },
    [IntegrationTypes.CXSAST]: {
      type: FORM,
      title: "Checkmarx SAST",
      form_fields: [
        { key: "url", label: "Url" },
        { key: "username", label: "Username" },
        { key: "password", label: "Password", type: "password" }
      ],
      url: "",
      logo: checkmarxLogo,
      client_id: "",
      description: CHECKMARX_DESC,
      icon: "checkmarx",
      application: IntegrationTypes.CXSAST,
      bypassTitleTransform: true,
      notFullySupportedIntegration: true
    },
    [IntegrationTypes.AZURE]: {
      title: "Azure DevOps Services",
      type: OAUTH,
      url: AZURE_DEVOPS_URL,
      client_id: AZURE_DEVOPS_CLIENT_ID,
      form_fields: [],
      query_params: {
        client_id: AZURE_DEVOPS_CLIENT_ID,
        response_type: "Assertion",
        redirect_uri: redirect_uri(IntegrationTypes.AZURE),
        scope: "vso.build vso.code vso.project vso.work vso.agentpools vso.variablegroups_read vso.release"
      },
      logo: azureDevopsLogo,
      docs_url_slug: "automated-integrations/azure-devops/",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/azure-devops/`,
      lowercaseTitleString: false,
      bypassTitleTransform: true,
      description: AZURE_DESC,
      icon: "azure-devops",
      application: IntegrationTypes.AZURE,
      metadata_fields: [
        {
          key: "options",
          label: "Options",
          type: "checkbox-group",
          required: false,
          options: [{ value: "comments", label: "Fetch Workitem Comments", defaultValue: true }]
        }
      ],
      detailsPage_metadata_fields: [
        {
          key: "wi",
          be_key: "wi",
          label: "Azure DevOps Boards",
          type: "toggle",
          defaultValue: {
            enabled: true
          }
        },
        {
          key: "scm",
          be_key: "scm",
          label: "Azure DevOps Git Repos",
          type: "toggle",
          defaultValue: {
            enabled: true
          }
        },
        {
          key: "cicd",
          be_key: "cicd",
          label: "Azure DevOps Pipelines",
          type: "toggle",
          defaultValue: {
            enabled: true
          }
        }
      ],
      mapIntegrationForm: form => {
        let metadata = { ...form?.metadata };
        if (!metadata.hasOwnProperty("wi") && !metadata.hasOwnProperty("scm") && !metadata.hasOwnProperty("cicd")) {
          metadata = {
            ...metadata,
            wi: { enabled: true },
            scm: { enabled: true },
            cicd: { enabled: true }
          };
        }
        form.metadata = {
          ...(metadata || {})
        };
        return form;
      }
    },
    [IntegrationTypes.GITLAB_CLOUD]: {
      type: OAUTH,
      form_fields: [],
      url: GITLAB_URL,
      client_id: GITLAB_CLIENT_ID,
      query_params: {
        client_id: GITLAB_CLIENT_ID,
        response_type: "code",
        redirect_uri: redirect_uri(IntegrationTypes.GITHUB),
        scope: "api"
      },
      logo: gitlabLogo,
      description: GITLAB_DESC,
      docs_url_slug: "automated-integrations/gitlab/",
      icon: "gitlab",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/gitlab/`,
      application: IntegrationTypes.GITLAB,
      displayForTrialUser: false
    },
    [IntegrationTypes.GITLAB_ENTERPRISE]: {
      type: OAUTH,
      form_fields: [],
      url: GITLAB_URL,
      client_id: GITLAB_CLIENT_ID,
      query_params: {
        client_id: GITLAB_CLIENT_ID,
        response_type: "code",
        redirect_uri: redirect_uri(IntegrationTypes.GITHUB),
        scope: "api"
      },
      logo: gitlabLogo,
      description: GITLAB_DESC,
      docs_url_slug: "automated-integrations/gitlab/",
      icon: "gitlab",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/gitlab/`,
      application: IntegrationTypes.GITLAB,
      metadata_fields: [
        {
          key: "options",
          label: "Options",
          type: "checkbox-group",
          required: false,
          // If new options are added, please update metaOptions in mapIntegrationForm as well...
          options: [
            { value: "fetch_commits", label: "Fetch Commits", defaultValue: true },
            { value: "fetch_prs", label: "Fetch PRs", defaultValue: true },
            { value: "fetch_issues", label: "Fetch Issues", defaultValue: true },
            { value: "fetch_projects", label: "Fetch Projects", defaultValue: true }
          ],
          hiddenOptions: [
            {
              key: "hiddenOptions",
              label: "",
              type: "checkbox-group",
              required: false,
              isParentChecked: "fetch_commits",
              options: [{ value: "fetch_commit_files", label: "Fetch Commit Files", defaultValue: true }]
            }
          ]
        }
      ],
      mapIntegrationForm: form => {
        // Options fields
        const metaOptions = ["fetch_commits", "fetch_prs", "fetch_issues", "fetch_projects", "fetch_commit_files"];
        form.metadata = {
          ...(form.metadata || {}),
          ...metaOptions.reduce((acc, option) => ({ ...acc, [option]: !!(form.metadata || {})[option] }), {})
        };
        return form;
      }
    },
    [IntegrationTypes.JENKINS]: {
      type: FORM,
      firstStepTitle: "Details",
      newDetailsPage: true,
      supportCancel: true,
      finalStepButtontext: "Finish",
      backButtonText: "Back",
      stepButtonBothEnd: false,
      description: `CICD platform`,
      application: IntegrationTypes.JENKINS,
      url: "",
      client_id: "",
      form_fields: []
    },
    [IntegrationTypes.COVERITY]: {
      type: FORM,
      form_fields: [
        { key: "url", label: "Url" },
        { key: "username", label: "Username" },
        { key: "apikey", label: "Apikey", type: "password" }
      ],
      url: "",
      client_id: "",
      logo: coverityLogo,
      description: COVERITY_DESC,
      icon: "coverity",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/coverity/`,
      application: IntegrationTypes.COVERITY,
      notFullySupportedIntegration: true
    },
    [IntegrationTypes.DRONECI]: {
      title: "Drone CI",
      type: FORM,
      bypassTitleTransform: true,
      form_fields: [
        { key: "url", label: "Url", defaultValue: "https://drone.io", required: true },
        {
          key: "apikey",
          label: "Authorization Token",
          type: "password",
          defaultValue: "",
          required: true
        }
      ],
      metadata_fields: [
        {
          key: "repos",
          label: "Repositories",
          type: "comma-multi-select",
          description: DRONECI_REPO_DESC,
          required: false
        },
        {
          key: "options",
          label: "Options",
          type: "checkbox-group",
          required: false,
          // If new options are added, please update metaOptions in mapIntegrationForm as well...
          options: [{ value: "fetch_steplogs", label: "Fetch Step logs" }]
        }
      ],
      url: "https://drone.io",
      client_id: "",
      logo: droneciLogo,
      description: DRONECI_DESC,
      docs_url_slug: "automated-integrations/droneci/",
      icon: "droneci",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/droneci/`,
      application: IntegrationTypes.DRONECI,
      mapIntegrationForm: form => {
        // Options fields
        const metaOptions = ["is_push_based", "fetch_steplogs"];
        form.metadata = {
          ...(form.metadata || {}),
          ...metaOptions.reduce((acc, option) => ({ ...acc, [option]: !!(form.metadata || {})[option] }), {})
        };
        return form;
      },
      support_multiple_api_keys: false,
      mapDataForYAML: form => {
        // transforming the keys fields
        // form.keys = form.keys.map(field => ({ api_key: field.apikey, user_name: field.username }));
        return form;
      }
    },
    [IntegrationTypes.CIRCLECI]: {
      title: "Circle CI",
      type: FORM,
      bypassTitleTransform: true,
      form_fields: [
        { key: "url", label: "Url", defaultValue: "https://circleci.com", required: true },
        {
          key: "apikey",
          label: "Authorization Token",
          type: "password",
          defaultValue: "",
          required: true
        }
      ],
      metadata_fields: [
        {
          key: "repos",
          label: "Repositories",
          type: "comma-multi-select",
          description: CIRCLECI_REPO_DESC,
          required: false
        },
        {
          key: "options",
          label: "Options",
          type: "checkbox-group",
          required: false,
          // If new options are added, please update metaOptions in mapIntegrationForm as well...
          options: [{ value: "fetch_action_logs", label: "Fetch Action logs" }]
        }
      ],
      url: "https://circleci.com",
      client_id: "",
      logo: CircleCI,
      description: CIRCLECI_DESC,
      docs_url_slug: "automated-integrations/circleci/",
      icon: "circleci",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/circleci/`,
      application: IntegrationTypes.CIRCLECI,
      mapIntegrationForm: form => {
        // Options fields
        const metaOptions = ["is_push_based", "fetch_action_logs"];
        form.metadata = {
          ...(form.metadata || {}),
          ...metaOptions.reduce((acc, option) => ({ ...acc, [option]: !!(form.metadata || {})[option] }), {})
        };
        return form;
      },
      support_multiple_api_keys: false,
      mapDataForYAML: form => {
        // transforming the keys fields
        // form.keys = form.keys.map(field => ({ api_key: field.apikey, user_name: field.username }));
        return form;
      }
    },
    [IntegrationTypes.MS_TEAMS]: {
      title: "Microsoft Teams",
      type: OAUTH,
      url: MS_TEAMS_URL,
      client_id: MS_TEAMS_CLIENT_ID,
      form_fields: [],
      query_params: {
        client_id: MS_TEAMS_CLIENT_ID,
        response_type: "code",
        redirect_uri: redirect_uri(IntegrationTypes.MS_TEAMS),
        response_mode: "query",
        scope:
          "Channel.ReadBasic.All ChannelMessage.Send Chat.Create Chat.ReadWrite offline_access Team.ReadBasic.All User.Read User.ReadBasic.All",
        state: "PROPELO_STATE"
      },
      logo: msteamsLogo,
      description: MS_TEAMS_DESC,
      icon: "ms_teams",
      application: IntegrationTypes.MS_TEAMS,
      docs_url_slug: "automated-integrations/microsoft-teams/",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/microsoft-teams/`,
      notFullySupportedIntegration: true
    },
    [IntegrationTypes.HARNESSNG]: {
      title: "Harness NG",
      type: FORM,
      bypassTitleTransform: true,
      form_fields: [
        {
          key: "url",
          label: "Url",
          defaultValue: HARNESS_URL,
          required: true
        },
        {
          key: "apikey",
          label: "Api Key",
          type: "password",
          defaultValue: "",
          required: true
        }
      ],
      metadata_fields: [
        {
          key: "accountId",
          label: "Account Id",
          type: "text",
          required: true
        },
        {
          key: "organization",
          label: "Collection(s)",
          type: "comma-multi-select",
          required: false,
          description: HARNESS_ORGANIZATION_DESC
        },
        {
          key: "project",
          label: "PROJECT(s)",
          type: "comma-multi-select",
          required: false,
          description: HARNESS_PROJECT_DESC
        }
      ],
      url: HARNESS_URL,
      client_id: "",
      logo: harnessLogo,
      description: HARNESS_DESC,
      docs_url_slug: "automated-integrations/harnessng/",
      icon: "harness",
      config_docs_url: `${AUTOMATIC_DOCS_URL}/harnessng/`,
      application: IntegrationTypes.HARNESSNG,
      mapIntegrationForm: form => {
        // Options fields
        const metaOptions = [];
        form.metadata = {
          ...(form.metadata || {}),
          ...metaOptions.reduce((acc, option) => ({ ...acc, [option]: !!(form.metadata || {})[option] }), {})
        };
        return form;
      },
      support_multiple_api_keys: false,
      mapDataForYAML: form => {
        return form;
      }
    },
    github_actions: {
      title: "GitHub Action",
      type: FORM,
      form_fields: [
        { key: "url", label: "Url", defaultValue: "https://api.github.com" },
        {
          key: "keys",
          label: "Personal Access Token",
          arrayType: true,
          fields: [
            { key: "username", label: "User Name" },
            { key: "apikey", label: "Integration API Key", type: "password" }
          ],
          defaultValue: [],
          required: true
        }
      ],
      metadata_fields: [
        {
          key: "repos",
          label: "Repositories",
          type: "comma-multi-select",
          description: GITHUB_REPO_DESC,
          required: false
        },
        {
          key: "options",
          label: "Options",
          type: "checkbox-group",
          required: false,
          // If new options are added, please update metaOptions in mapIntegrationForm as well...
          options: [
            { value: "is_push_based", label: "Connect via Github Webhook" },
            { value: "fetch_commits", label: "Fetch Commits" },
            { value: "fetch_prs", label: "Fetch PRs" },
            { value: "fetch_issues", label: "Fetch Issues" },
            { value: "fetch_projects", label: "Fetch Projects" }
          ],
          hiddenOptions: [
            {
              key: "hiddenOptions",
              label: "",
              type: "checkbox-group",
              required: false,
              isParentChecked: "fetch_commits",
              options: [{ value: "fetch_commit_files", label: "Fetch Commit Files" }]
            }
          ]
        }
      ],
      url: "https://api.github.com",
      client_id: "",
      logo: githubActionsLogo,
      description: GITHUB_DESC,
      docs_url_slug: "automated-integrations/github/",
      icon: "github_actions",
      check_for_exact_icon_path: true,
      config_docs_url: `${AUTOMATIC_DOCS_URL}/github/`,
      application: "github_actions",
      doNotSplitIntegration: true,
      mapIntegrationForm: form => {
        // Options fields
        const metaOptions = [
          "is_push_based",
          "fetch_commits",
          "fetch_prs",
          "fetch_issues",
          "fetch_projects",
          "fetch_commit_files"
        ];
        form.metadata = {
          ...(form.metadata || {}),
          ...metaOptions.reduce((acc, option) => ({ ...acc, [option]: !!(form.metadata || {})[option] }), {})
        };
        return form;
      },
      support_multiple_api_keys: true,
      mapDataForYAML: form => {
        // transforming the keys fields
        form.keys = form.keys.map(field => ({ api_key: field.apikey, user_name: field.username }));
        return form;
      },
      displayForTrialUser: false
    }
  };
};
