import { get } from "lodash";

export const integrationConfigState = (state: any, id: string) => {
  const defaultState = { loading: true, error: true };
  return get(state.restapiReducer, ["jira_integration_config", "list", id], defaultState);
};
