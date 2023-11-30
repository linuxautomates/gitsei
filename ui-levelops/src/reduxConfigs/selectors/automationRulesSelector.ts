import { get } from "lodash";

export const getAutomationRuleCreateSelector = (state: any) => {
  return get(state.restapiReducer, ["automation_rules", "create"], {});
};

export const getAutomationRuleDeleteSelector = (state: any) => {
  return get(state.restapiReducer, ["automation_rules", "delete"], {});
};

export const getAutomationRuleGetSelector = (state: any) => {
  return get(state.restapiReducer, ["automation_rules", "get"], {});
};

export const getAutomationRuleUpdateSelector = (state: any) => {
  return get(state.restapiReducer, ["automation_rules", "update"], {});
};
