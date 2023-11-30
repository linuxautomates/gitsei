import {
  ACTIVE_INTEGRATION_STEP,
  INTEGRATION_CODE,
  INTEGRATION_COMPLETE,
  INTEGRATION_FORM,
  INTEGRATION_ID,
  INTEGRATION_NAME,
  INTEGRATION_STATE,
  INTEGRATION_SUB_TYPE,
  INTEGRATION_TYPE,
  integrationsActions
} from "./actionTypes";

export const integrationType = type => ({ type: INTEGRATION_TYPE, payload: type });

export const activeIntegrationStep = step => ({ type: ACTIVE_INTEGRATION_STEP, payload: step });

export const integrationComplete = () => ({ type: INTEGRATION_COMPLETE });

export const integrationName = name => ({ type: INTEGRATION_NAME, payload: name });

export const integrationState = state => ({ type: INTEGRATION_STATE, payload: state });

export const integrationCode = code => ({ type: INTEGRATION_CODE, payload: code });

export const integrationForm = formdict => ({ type: INTEGRATION_FORM, payload: formdict });

export const integrationId = id => ({ type: INTEGRATION_ID, payload: id });

export const integrationSubType = type => ({ type: INTEGRATION_SUB_TYPE, payload: type });

export const setIntegrationTypeAction = type => ({
  type: integrationsActions.SET_INTEGRATION_TYPE,
  payload: type
});

export const setIntegrationCredentialAction = (field, value) => ({
  type: integrationsActions.SET_INTEGRATION_CREDENTIALS,
  payload: { field, value }
});

export const setIntegrationInformationAction = (field, value) => ({
  type: integrationsActions.SET_INTEGRATION_INFORMATION,
  payload: { field, value }
});

export const setIntegrationsStepAction = step => ({
  type: integrationsActions.SET_INTEGRATION_STEP,
  payload: step
});

export const setIntegrationsStateAction = state => ({
  type: integrationsActions.SET_INTEGRATION_STATE,
  payload: state
});

export const resetCreateStoreAction = () => ({
  type: integrationsActions.RESET_CREATE_STORE
});
