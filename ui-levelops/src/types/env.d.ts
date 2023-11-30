declare namespace APP_ENV {
  interface ENV {
    readonly NODE_ENV: "development" | "production" | "test";
    readonly REACT_APP_API_URL: string;
    readonly REACT_APP_API_VERSION: string;
    readonly REACT_APP_API_VERSION_2: string;
    readonly REACT_APP_MODE: string;
    readonly REACT_APP_UI_URL: string;
    readonly REACT_APP_BUGSNAG_API_KEY: string;
    readonly REACT_APP_GA_TRACKER: string;
    readonly REACT_APP_TINYMCE_API_KEY: string;
    readonly REACT_APP_DASHBOARD: string;
    readonly REACT_APP_SDLC_FLOWS: string;
    readonly REACT_APP_STT: string;
    readonly REACT_APP_WORKFLOWS: string;
    readonly REACT_APP_SIGNATURES: string;
    readonly REACT_APP_VIOLATION_LOGS: string;
    readonly REACT_APP_TOOLS: string;
    readonly REACT_APP_GITHUB_CLIENT_ID: string;
    readonly REACT_APP_JIRA_CLIENT_ID: string;
    readonly REACT_APP_SLACK_CLIENT_ID: string;
    readonly REACT_APP_BITBUCKET_CLIENT_ID: string;
    readonly REACT_APP_SALESFORCE_CLIENT_ID: string;
    readonly REACT_APP_GITLAB_CLIENT_ID: string;
    readonly REACT_APP_AZURE_DEVEOPS_CLIENT_ID: string;
    readonly REACT_APP_MS_TEAMS_CLIENT_ID: string;
    readonly REACT_APP_PROPEL_SPLIT_JIRA_CUSTOM_FIELD_INTEGRATION: string;
    readonly REACT_APP_ALL_ACCESS_USERS: string;
  }
}
