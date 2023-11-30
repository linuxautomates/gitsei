export const PERSIST_AUTOMATION_RULE_FORM = "PERSIST_AUTOMATION_RULE_FORM";
export const SET_ATTEMPT_TO_SUBMIT_AUTOMATION_RULE_FORM = "SET_ATTEMPT_TO_SUBMIT_AUTOMATION_RULE_FORM";

export interface PersistAutomationRuleFormPayload {
  formData: { [key: string]: any };
  attempted_to_submit?: boolean;
  automationRuleId?: string;
}

interface PersistAutomationRuleFormAction {
  type: typeof PERSIST_AUTOMATION_RULE_FORM;
  payload: PersistAutomationRuleFormPayload;
}

interface SetAttemptToSubmitAutomationRuleFormAction {
  type: typeof SET_ATTEMPT_TO_SUBMIT_AUTOMATION_RULE_FORM;
  payload: {
    attempted_to_submit: boolean;
    automationRuleId: string;
  };
}

export type AutomationRulesActionTypes = PersistAutomationRuleFormAction | SetAttemptToSubmitAutomationRuleFormAction;

export function persistAutomationRuleForm(payload: PersistAutomationRuleFormAction["payload"]) {
  return {
    type: PERSIST_AUTOMATION_RULE_FORM,
    payload
  };
}

export function setAttemptToSubmitAutomationRuleForm(payload: SetAttemptToSubmitAutomationRuleFormAction["payload"]) {
  return {
    type: SET_ATTEMPT_TO_SUBMIT_AUTOMATION_RULE_FORM,
    payload
  };
}
