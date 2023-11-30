import { RestCommTemplate } from "classes/RestCommTemplate";
import { RestProduct } from "classes/RestProduct";
import { RestSection } from "classes/RestQuestionnaire";
import { RestPolicy } from "classes/RestPolicy";
import { RestGithubMapping, RestJiraMapping, RestMapping } from "../../classes/RestProduct";
import { RestApikey } from "../../classes/RestApikey";
import { RestSmartTicketTemplate } from "../../classes/RestSmartTicketTemplate";
import { RestTriageRule } from "../../classes/RestTriageRule";
import { RestState } from "classes/RestState";
import { RestAutomationRule } from "classes/RestAutomationRule";
import { get } from "lodash";
import { v1 as uuid } from "uuid";

export const getFormStore = store => {
  //return store.formReducer.toJS();
  return store.formReducer;
};

export const getStateForm = store => {
  const forms = getFormStore(store);
  const stateForm = forms.state_form || new RestState();
  return {
    name: stateForm.name
  };
};

export const getUserForm = store => {
  const forms = getFormStore(store);
  const userForm = forms.user_form || {};
  return {
    email: userForm.email || "",
    notify_user: userForm.notify_user === undefined,
    first_name: userForm.first_name || "",
    last_name: userForm.last_name || "",
    user_type: userForm.user_type || "ADMIN",
    password_auth_enabled: userForm.password_auth_enabled === undefined ? true : userForm.password_auth_enabled,
    saml_auth_enabled: userForm.saml_auth_enabled === undefined ? false : userForm.saml_auth_enabled,
    mfa_enabled: userForm.mfa_enabled === undefined ? false : userForm.mfa_enabled,
    mfa_enrollment_end: userForm.mfa_enrollment_end,
    mfa_reset_at: userForm.mfa_reset_at,
    mfa_enforced: userForm.mfa_enforced,
    scopes: userForm?.scopes || {},
    metadata: userForm?.metadata || {
      workspaces: {
        [uuid()]: {
          workspaceId: "",
          orgUnitIds: []
        }
      }
    }
  };
};

export const getKBForm = store => {
  const forms = getFormStore(store);
  const KBForm = forms.kb_form || {};
  return {
    name: KBForm.name || "",
    tags: KBForm.tags || [],
    type: KBForm.type || "LINK",
    value: KBForm.value || "",
    metadata: KBForm.metadata || ""
  };
};

export const getTemplateForm = store => {
  const forms = getFormStore(store);
  const TemplateForm = forms.template_form || {};
  return {
    name: TemplateForm.name,
    message: TemplateForm.message || RestCommTemplate.DEFAULT_MESSAGE,
    bot_name: TemplateForm.bot_name || "",
    type: TemplateForm.type || RestCommTemplate.OPTIONS[0],
    email_subject: TemplateForm.email_subject || "",
    default: TemplateForm.default || false,
    event_type: TemplateForm.event_type || RestCommTemplate.EVENT_OPTIONS[0]
  };
};

export const getPolicyForm = store => {
  const forms = getFormStore(store);
  const PolicyForm = forms.policy_form || new RestPolicy();
  return PolicyForm;
};

export const getAssessmentTemplateForm = store => {
  const forms = getFormStore(store);
  const AssessmentTemplateForm = forms.assessment_template_form || {};
  return {
    name: AssessmentTemplateForm.name,
    sections: AssessmentTemplateForm.sections || [],
    section_ids: AssessmentTemplateForm.section_ids || [],
    tag_ids: AssessmentTemplateForm.tag_ids || [],
    low_risk_boundary: AssessmentTemplateForm.low_risk_boundary || 30,
    mid_risk_boundary: AssessmentTemplateForm.mid_risk_boundary || 70,
    risk_enabled: AssessmentTemplateForm.risk_enabled || false,
    kb_ids: AssessmentTemplateForm.kb_ids || []
  };
};

export const getSectionForm = store => {
  const forms = getFormStore(store);
  const sectionForm = forms.section_form || new RestSection();
  return sectionForm;
};

export const getProductForm = store => {
  const forms = getFormStore(store);
  const productForm = forms.product_form || new RestProduct();
  return productForm;
};

export const getJiraMappingForm = store => {
  const forms = getFormStore(store);
  const mappingForm = forms.jira_mapping_form || new RestJiraMapping();
  return mappingForm;
};

export const getGithubMappingForm = store => {
  const forms = getFormStore(store);
  const mappingForm = forms.github_mapping_form || new RestGithubMapping();
  return mappingForm;
};

export const getMappingForm = store => {
  const forms = getFormStore(store);
  const mappingForm = forms.mapping_form || new RestMapping();
  return mappingForm;
};

export const getStagesForm = store => {
  const forms = getFormStore(store);
  const stagesForms = forms.stages_form || [];
  return stagesForms;
};

export const getApikeyForm = store => {
  const forms = getFormStore(store);
  const apikeyForm = forms.apikey_form || new RestApikey();
  return apikeyForm;
};

export const getSmartTicketTemplateForm = store => {
  const forms = getFormStore(store);
  let newTicketTemplate = new RestSmartTicketTemplate();
  //newTicketTemplate.ticket_fields = defaultFields;
  const sttForm = forms.stt_form || newTicketTemplate;
  return sttForm;
};

export const getPropelForm = store => {
  const forms = getFormStore(store);
  const propelForm = forms.propel_form || undefined;
  return propelForm;
};

export const getWorkItemForm = store => {
  const forms = getFormStore(store);
  const workItemForm = forms.workitem_form || undefined;
  return workItemForm;
};

export const triageRuleForm = store => {
  const forms = getFormStore(store);
  const ruleForm = forms.triage_rule_form || new RestTriageRule();
  return ruleForm;
};

export const getformState = (state, formName) => {
  return get(state.formReducer, formName, undefined);
};

export const getAutomationRuleForm = store => {
  const forms = getFormStore(store);
  const automationRuleForm = forms.automation_rule_form || new RestAutomationRule();
  return automationRuleForm;
};
