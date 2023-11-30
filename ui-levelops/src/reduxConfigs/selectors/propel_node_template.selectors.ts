import { get } from "lodash";

export const getPropelNodeTemplateListSelector = (state: any) => {
  return get(state.restapiReducer, ["propel_node_templates", "list"], {});
};

export const getPropelNodeTemplateGetSelector = (state: any) => {
  return get(state.restapiReducer, ["propel_node_templates", "get"], {});
};
