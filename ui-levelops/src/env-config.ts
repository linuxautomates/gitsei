const envConfig = {
  get: (key: string) => {
    switch (key) {
      case "NODE_ENV":
        return window._env_?.NODE_ENV ?? process.env.NODE_ENV;
      case "API_URL":
        return window._env_?.REACT_APP_API_URL ?? process.env.REACT_APP_API_URL;
      case "API_VERSION":
        return window._env_?.REACT_APP_API_VERSION ?? process.env.REACT_APP_API_VERSION;
      case "API_VERSION_2":
        return window._env_?.REACT_APP_API_VERSION_2 ?? process.env.REACT_APP_API_VERSION_2;
      case "UI_URL":
        return window._env_?.REACT_APP_UI_URL ?? process.env.REACT_APP_UI_URL;
      case "APP_MODE":
        return window._env_?.REACT_APP_MODE ?? process.env.REACT_APP_MODE;
      case "BUGSNAG_API_KEY":
        return window._env_?.REACT_APP_BUGSNAG_API_KEY ?? process.env.REACT_APP_BUGSNAG_API_KEY;
      case "GA_TRACKER":
        return window._env_?.REACT_APP_GA_TRACKER ?? process.env.REACT_APP_GA_TRACKER;
      case "TINYMCE_API_KEY":
        return window._env_?.REACT_APP_TINYMCE_API_KEY ?? process.env.REACT_APP_TINYMCE_API_KEY;
      case "DASHBOARD":
        return window._env_?.REACT_APP_DASHBOARD ?? process.env.REACT_APP_DASHBOARD;
      case "SDLC_FLOWS":
        return window._env_?.REACT_APP_SDLC_FLOWS ?? process.env.REACT_APP_SDLC_FLOWS;
      case "STT":
        return window._env_?.REACT_APP_STT ?? process.env.REACT_APP_STT;
      case "WORKFLOWS":
        return window._env_?.REACT_APP_WORKFLOWS ?? process.env.REACT_APP_WORKFLOWS;
      case "SIGNATURES":
        return window._env_?.REACT_APP_SIGNATURES ?? process.env.REACT_APP_SIGNATURES;
      case "VIOLATION_LOGS":
        return window._env_?.REACT_APP_VIOLATION_LOGS ?? process.env.REACT_APP_VIOLATION_LOGS;
      case "TOOLS":
        return window._env_?.REACT_APP_TOOLS ?? process.env.REACT_APP_TOOLS;
      case "GITHUB_CLIENT_ID":
        return window._env_?.REACT_APP_GITHUB_CLIENT_ID ?? process.env.REACT_APP_GITHUB_CLIENT_ID;
      case "JIRA_CLIENT_ID":
        return window._env_?.REACT_APP_JIRA_CLIENT_ID ?? process.env.REACT_APP_JIRA_CLIENT_ID;
      case "SLACK_CLIENT_ID":
        return window._env_?.REACT_APP_SLACK_CLIENT_ID ?? process.env.REACT_APP_SLACK_CLIENT_ID;
      case "BITBUCKET_CLIENT_ID":
        return window._env_?.REACT_APP_BITBUCKET_CLIENT_ID ?? process.env.REACT_APP_BITBUCKET_CLIENT_ID;
      case "SALESFORCE_CLIENT_ID":
        return window._env_?.REACT_APP_SALESFORCE_CLIENT_ID ?? process.env.REACT_APP_SALESFORCE_CLIENT_ID;
      case "GITLAB_CLIENT_ID":
        return window._env_?.REACT_APP_GITLAB_CLIENT_ID ?? process.env.REACT_APP_GITLAB_CLIENT_ID;
      case "AZURE_DEVEOPS_CLIENT_ID":
        return window._env_?.REACT_APP_AZURE_DEVEOPS_CLIENT_ID ?? process.env.REACT_APP_AZURE_DEVEOPS_CLIENT_ID;
      case "MS_TEAMS_CLIENT_ID":
        return window._env_?.REACT_APP_MS_TEAMS_CLIENT_ID ?? process.env.REACT_APP_MS_TEAMS_CLIENT_ID;
      case "PROPEL_SPLIT_JIRA_CUSTOM_FIELD_INTEGRATION":
        return (
          window._env_?.REACT_APP_PROPEL_SPLIT_JIRA_CUSTOM_FIELD_INTEGRATION ??
          process.env.REACT_APP_PROPEL_SPLIT_JIRA_CUSTOM_FIELD_INTEGRATION
        );
      case "ALL_ACCESS_USERS":
        return window._env_?.REACT_APP_ALL_ACCESS_USERS ?? process.env.REACT_APP_ALL_ACCESS_USERS;
      default:
        return "";
    }
  }
};

export default envConfig;
