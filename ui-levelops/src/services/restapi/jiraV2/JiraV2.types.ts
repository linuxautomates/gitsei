export interface jiraIntegrationPayload {
  metadata: {
    sensitive_fields: string;
  };
  clientKey: string;
  name: string;
  application: string;
}
