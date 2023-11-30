import { RestIntegrations } from "classes/RestIntegrations";
import { IntegrationAuthTypes } from "configurations/pages/integrations/gitlab-integration/helpers";
import { forEach, get, unset } from "lodash";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { sanitizeObject } from "utils/commonUtils";
import {
  getAuthorizationConfigs,
  INTEGRATION_BASED_ADDITIONAL_OPTIONS_MAP,
  SelfOnboardingFormFields
} from "../constants";
import { IntegrationCreatePayloadParamsType } from "../types/integration-step-components-types";

const APPLICATIONS_MULTIPLE_API_KEY = [IntegrationTypes.GITHUB, IntegrationTypes.GITHUB_ACTIONS];

export const initialFormValues = (integration: string) => {
  const allOptions = INTEGRATION_BASED_ADDITIONAL_OPTIONS_MAP[integration];
  const initial: any = {};
  const initiallyUncheckedValues: string[] = ["is_push_based"];
  if (integration === IntegrationTypes.GITHUB) {
    initiallyUncheckedValues.push("fetch_projects");
  }
  if (allOptions) {
    Object.keys(allOptions)
      .filter(key => !initiallyUncheckedValues.includes(key))
      .forEach(key => (initial[key] = true));
  }
  return initial;
};

export const getIntegrationCreatePayload = (args: IntegrationCreatePayloadParamsType) => {
  const { selfOnboardingForm, application, code, state, extraPayload, isUpdate } = args;

  let payload: any = {
    metadata: {
      ...(selfOnboardingForm ?? {})
    }
  };

  const isOAUTH = code && state;
  const getFromSelfOnboardingForm = (key: string) => get(selfOnboardingForm, [key], undefined);
  let repos = get(payload, ["metadata", SelfOnboardingFormFields.REPOS], []);
  const ingestAllRepos = get(payload, ["metadata", SelfOnboardingFormFields.INGEST_ALL_REPOS], false);
  repos = ingestAllRepos ? "" : Array.isArray(repos) ? repos.join(",") : repos;

  let isMultipleAPIKeys = APPLICATIONS_MULTIPLE_API_KEY.includes(application as IntegrationTypes) ? true : false;

  let method = IntegrationAuthTypes.API_KEY;
  if (isOAUTH) {
    method = IntegrationAuthTypes.OAUTH;
  } else {
    if (isMultipleAPIKeys) {
      method = IntegrationAuthTypes.MULTIPLE_API_KEYS;
    }
  }

  payload = {
    ...payload,
    metadata: {
      ...get(payload, ["metadata"], {}),
      [SelfOnboardingFormFields.REPOS]: repos
    },
    description: getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_DESCRIPTION),
    name: getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_NAME),
    satellite: getFromSelfOnboardingForm(SelfOnboardingFormFields.SATELLITE_INTEGRATION),
    application,
    method,
    start_ingestion: false
  };

  if (isOAUTH) {
    payload = {
      ...payload,
      state,
      code
    };
  } else {
    /**
     * github allows for more than 1 apikey
     */
    if (isMultipleAPIKeys) {
      payload = {
        ...payload,
        keys: [
          {
            apikey: getFromSelfOnboardingForm(SelfOnboardingFormFields.PERSONAL_ACCESS_TOKEN)
          }
        ]
      };
    } else {
      payload = {
        ...payload,
        apikey: getFromSelfOnboardingForm(SelfOnboardingFormFields.PERSONAL_ACCESS_TOKEN)
      };
    }
  }

  const staticURL = get(getAuthorizationConfigs(), [application], { default_url: "" }).default_url;
  const metadataURL = get(payload, ["metadata", "url"], "");
  if (metadataURL || staticURL) {
    payload = {
      ...payload,
      url: metadataURL || staticURL
    };
  }

  if (Object.keys(extraPayload).length) {
    payload = {
      ...payload,
      ...extraPayload
    };
  }

  const keysToUnset = [
    SelfOnboardingFormFields.INTEGRATION_NAME,
    SelfOnboardingFormFields.PERSONAL_ACCESS_TOKEN,
    SelfOnboardingFormFields.SATELLITE_INTEGRATION,
    SelfOnboardingFormFields.INTEGRATION_ID,
    SelfOnboardingFormFields.INTEGRATION_URL,
    SelfOnboardingFormFields.INTEGRATION_DESCRIPTION,
    SelfOnboardingFormFields.VALID_NAME
  ];

  forEach(keysToUnset, key => {
    unset(payload, ["metadata", key]);
  });

  if (isUpdate) {
    /**
     * Code needs to be removed from this PUT call
     * the backend thinks it’s a new code and tries the generate a new access token but that fails because the code was already used
     */
    unset(payload, ["code"]);
  }

  const restIntegration = new RestIntegrations();
  restIntegration.application = payload.application;
  restIntegration.formData = sanitizeObject(payload);
  restIntegration.description = payload.description;
  restIntegration.method = payload.method;
  restIntegration.name = payload.name;
  return restIntegration;
};

export const getIntialSelfOnboardingFormState = (integrationInfo: any) => {
  const isSatellite = get(integrationInfo, ["satellite"], "");
  const repos = get(integrationInfo, ["metadata", "repos"], "");
  return sanitizeObject({
    ...get(integrationInfo, ["metadata"], {}),
    name: get(integrationInfo, ["name"]),
    url: get(integrationInfo, ["url"]),
    description: get(integrationInfo, ["description"]),
    repos: isSatellite ? repos : repos.split(",") ?? []
  });
};

export const getIntegrationUpdatePayload = (selfOnboardingForm: any) => {
  let payload: any = {
    metadata: {
      ...(selfOnboardingForm ?? {})
    }
  };

  const getFromSelfOnboardingForm = (key: string) => get(selfOnboardingForm, [key], undefined);
  const url = get(payload, ["metadata", SelfOnboardingFormFields.INTEGRATION_URL], "");
  let repos = get(payload, ["metadata", SelfOnboardingFormFields.REPOS], []);
  const ingestAllRepos = get(payload, ["metadata", SelfOnboardingFormFields.INGEST_ALL_REPOS], false);
  repos = ingestAllRepos ? "" : Array.isArray(repos) ? repos.join(",") : repos;
  payload = {
    ...payload,
    metadata: {
      ...get(payload, ["metadata"], {}),
      [SelfOnboardingFormFields.REPOS]: repos
    },
    url,
    description: getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_DESCRIPTION),
    name: getFromSelfOnboardingForm(SelfOnboardingFormFields.INTEGRATION_NAME),
    start_ingestion: true
  };

  const keysToUnset = [
    SelfOnboardingFormFields.INTEGRATION_NAME,
    SelfOnboardingFormFields.INTEGRATION_DESCRIPTION,
    SelfOnboardingFormFields.INTEGRATION_ID,
    SelfOnboardingFormFields.INTEGRATION_URL,
    SelfOnboardingFormFields.VALID_NAME
  ];

  forEach(keysToUnset, key => {
    unset(payload, ["metadata", key]);
  });

  const restIntegration = new RestIntegrations();
  restIntegration.formData = sanitizeObject(payload);
  restIntegration.name = payload.name;
  restIntegration.description = payload.description;
  return restIntegration;
};
