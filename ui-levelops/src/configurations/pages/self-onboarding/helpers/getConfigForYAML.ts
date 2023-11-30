import { forEach, get, unset } from "lodash";
import { sanitizeObject } from "utils/commonUtils";
import { SelfOnboardingFormFields } from "../constants";

export const getConfigForYAML = (integrationForm: any) => {
  const metadata = get(integrationForm, ["metadata"]);
  const keysToUnset = [SelfOnboardingFormFields.REPOS, SelfOnboardingFormFields.INGEST_ALL_REPOS];
  forEach(keysToUnset, key => {
    unset(metadata, [key]);
  });
  return sanitizeObject({
    id: get(integrationForm, [SelfOnboardingFormFields.INTEGRATION_ID]),
    application: get(integrationForm, ["application"]),
    url: get(integrationForm, ["url"]),
    metadata,
    authentication: get(integrationForm, ["method"]),
    keys: get(integrationForm, ["keys"])
  });
};
