import { FormikErrors } from "formik";
import { JIRA_SELFMANAGED_CONFIGURATION_FIELDS } from "./Configuration.constants";
import { JiraSelfManagedDTO } from "./Configuration.types";

export function validateSelfManagedJiraConfigurations(formData: JiraSelfManagedDTO): FormikErrors<JiraSelfManagedDTO> {
  let errors: FormikErrors<JiraSelfManagedDTO> = {};
  const { identifier, name, jiraURL, username, apiKey } = formData || {};

  if (!name) {
    errors[JIRA_SELFMANAGED_CONFIGURATION_FIELDS.NAME] = "Name is required";
  }
  if (!jiraURL) {
    errors[JIRA_SELFMANAGED_CONFIGURATION_FIELDS.JIRA_URL] = "JIRA Url is required";
  }
  if (!username) {
    errors[JIRA_SELFMANAGED_CONFIGURATION_FIELDS.USERNAME] = "Username is required";
  }
  if (!apiKey) {
    errors[JIRA_SELFMANAGED_CONFIGURATION_FIELDS.APIKEY] = "API Key is required";
  }

  return errors;
}
