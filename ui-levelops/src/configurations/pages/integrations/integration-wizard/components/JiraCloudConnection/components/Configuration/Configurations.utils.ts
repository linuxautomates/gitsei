import { FormikErrors } from "formik";
import { ATLASSIAN_CONNECT_JWT, JIRA_SELFMANAGED_CONFIGURATION_FIELDS } from "./Configuration.constants";
import { JiraCloudConnectionDTO } from "./Configuration.types";

export function vaidateCloudJiraConfigurations(formData: JiraCloudConnectionDTO): FormikErrors<JiraCloudConnectionDTO> {
  let errors: FormikErrors<JiraCloudConnectionDTO> = {};
  const { name } = formData || {};

  if (!name) {
    errors[JIRA_SELFMANAGED_CONFIGURATION_FIELDS.NAME] = "Name is required";
  }

  return errors;
}

export function getJiraIntegrationPayload(clientKey: string) {
  return {
    metadata: {
      sensitive_fields: ""
    },
    clientKey,
    // Todo - this name has to be updated from the formik data
    name: "jira-connection",
    application: "jira",
    method: ATLASSIAN_CONNECT_JWT
  };
}
