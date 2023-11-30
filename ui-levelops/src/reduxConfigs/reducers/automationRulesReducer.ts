import {
  PERSIST_AUTOMATION_RULE_FORM,
  SET_ATTEMPT_TO_SUBMIT_AUTOMATION_RULE_FORM,
  AutomationRulesActionTypes
} from "../actions/automationRulesActions";

const ID_FOR_UNCREATED_AUTOMATION_RULE = "create";
const INITIAL_STATE = {};

export default function automationRulesReducer(
  state = INITIAL_STATE,
  action: AutomationRulesActionTypes = {} as AutomationRulesActionTypes
) {
  switch (action.type) {
    case PERSIST_AUTOMATION_RULE_FORM: {
      const { automationRuleId, formData } = action.payload || ({} as AutomationRulesActionTypes["payload"]);
      const id = automationRuleId || ID_FOR_UNCREATED_AUTOMATION_RULE;
      return {
        ...state,
        [id]: {
          // @ts-ignore
          ...state[id],
          formData
        }
      };
    }
    case SET_ATTEMPT_TO_SUBMIT_AUTOMATION_RULE_FORM: {
      const { automationRuleId, attempted_to_submit } = action.payload || ({} as AutomationRulesActionTypes["payload"]);
      const id = automationRuleId || ID_FOR_UNCREATED_AUTOMATION_RULE;
      return {
        ...state,
        [id]: {
          // @ts-ignore
          ...state[id],
          attempted_to_submit
        }
      };
    }
    default:
      return state;
  }
}
