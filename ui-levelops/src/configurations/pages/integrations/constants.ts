export const ADD_TO_WORKSPACE_NOTIFICATION_KEY = "ADD_TO_WORKSPACE_NOTIFICATION_KEY";

export const DEFAULT_URL_YAML = "https://api.propelo.ai";

export const INTEGRATIONS_METADATA_OVERRIDE_VALUES: { [key: string]: any } = {
  gitlab: { fetch_users: true }
};

export interface SatelliteYAMLInterface {
  jira?: { allow_unsafe_ssl: boolean };
  satellite: {
    tenant: string;
    api_key: string;
    url: string;
  };
  integrations: Array<Object>;
}
