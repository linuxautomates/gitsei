import { formClear, formInitialize, formUpdateField, formUpdateObj } from "../actions/formActions";
import {
  getApikeyForm,
  getAssessmentTemplateForm,
  getGithubMappingForm,
  getStateForm,
  getJiraMappingForm,
  getKBForm,
  getMappingForm,
  getPropelForm,
  getPolicyForm,
  getProductForm,
  getSectionForm,
  getSmartTicketTemplateForm,
  getStagesForm,
  getTemplateForm,
  getUserForm,
  getWorkItemForm
} from "../selectors/formSelector";

export const mapFormStateToProps = (state, props) => {
  return {
    form_state: state.formReducer
  };
};

export const mapStatesFormStateToProps = state => {
  return {
    state_form: getStateForm(state)
  };
};

export const mapUserFormStateToProps = state => {
  return {
    user_form: getUserForm(state)
  };
};

export const mapKBFormStateToProps = state => {
  return {
    kb_form: getKBForm(state)
  };
};

export const mapTemplateStateToProps = state => {
  return {
    template_form: getTemplateForm(state)
  };
};

export const mapPolicyFormStatetoProps = state => {
  return {
    policy_form: getPolicyForm(state)
  };
};

export const mapAssessmentTemplateFormStatetoProps = state => {
  return {
    assessment_template_form: getAssessmentTemplateForm(state)
  };
};

export const mapSectionFormStatetoProps = state => {
  return {
    section_form: getSectionForm(state)
  };
};

export const mapProductFormStatetoProps = state => {
  return {
    product_form: getProductForm(state)
  };
};

export const mapJiraMappingFormStatetoProps = state => {
  return {
    jira_mapping_form: getJiraMappingForm(state)
  };
};

export const mapGithubMappingFormStatetoProps = state => {
  return {
    github_mapping_form: getGithubMappingForm(state)
  };
};

export const mapMappingFormStatetoProps = state => {
  return {
    mapping_form: getMappingForm(state)
  };
};

export const mapStagesFormStatetoProps = state => {
  return {
    stages_form: getStagesForm(state)
  };
};

export const mapApikeyFormStatetoProps = state => {
  return {
    apikey_form: getApikeyForm(state)
  };
};

export const mapSTTFormStatetoProps = state => {
  return {
    stt_form: getSmartTicketTemplateForm(state)
  };
};

export const mapPropelFormStatetoProps = state => {
  return {
    propel_form: getPropelForm(state)
  };
};

export const mapWorkItemFormStatetoProps = state => {
  return {
    workItem: getWorkItemForm(state)
  };
};

export const mapFormDispatchToPros = dispatch => {
  return {
    formUpdateField: (formName, formField, formValue) => dispatch(formUpdateField(formName, formField, formValue)),
    formClear: formName => dispatch(formClear(formName)),
    formInitialize: formName => dispatch(formInitialize(formName)),
    formUpdateObj: (formName, obj) => dispatch(formUpdateObj(formName, obj))
  };
};
