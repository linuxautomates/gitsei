import {
  activeIntegrationStep,
  integrationCode,
  integrationComplete,
  integrationForm,
  integrationId,
  integrationName,
  integrationState,
  integrationType
} from "../actions/integrationActions";

export const mapIntegrationStatetoProps = state => {
  const store = state.integrationReducer.toJS();
  return {
    integration_type: store.integration_type,
    integration_sub_type: store.integration_sub_type,
    integration_step: store.integration_step,
    integration_name: store.integration_name,
    integration_state: store.integration_state,
    integration_code: store.integration_code,
    integration_form: store.integration_form || {},
    integration_id: store.integration_id
  };
};

export function mapIntegrationDispatchtoProps(dispatch) {
  return {
    activeIntegrationStep: step => dispatch(activeIntegrationStep(step)),
    integrationType: type => dispatch(integrationType(type)),
    integrationComplete: () => dispatch(integrationComplete()),
    integrationCode: code => dispatch(integrationCode(code)),
    integrationForm: formData => dispatch(integrationForm(formData)),
    integrationState: state => dispatch(integrationState(state)),
    integrationName: name => dispatch(integrationName(name)),
    integrationId: id => dispatch(integrationId(id))
  };
}
