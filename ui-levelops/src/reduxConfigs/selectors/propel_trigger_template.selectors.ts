import { get } from "lodash";

export const getPropelTriggerTemplateListSelector = (state: any) => {
  return get(state.restapiReducer, ["propel_trigger_templates", "list"], {});
};

export const getPropelTriggerTemplateGetSelector = (state: any) => {
  return get(state.restapiReducer, ["propel_trigger_templates", "get"], {});
};
