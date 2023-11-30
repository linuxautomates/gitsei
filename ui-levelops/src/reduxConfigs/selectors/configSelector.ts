import { ConfigStateType } from "configurations/configuration-types/authentication-types";
import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const CONFIGS = "configs";

export const getConfigListSelector = (state: any) => {
  return get(state.restapiReducer, ["configs", "list"], {});
};

export const getConfigUpdateSelector = (state: any) => {
  return get(state.restapiReducer, ["configs", "update"], {});
};

const getConfigKey = createParameterSelector((params: any) => params.config_key);

const configSelector = createSelector(restapiState, (data: any) => {
  return get(data, [CONFIGS], {});
});

// new configs list selector
export const configListSelector = createSelector(configSelector, (configs: any) => {
  return get(configs, ["list", "0"], { loading: true, error: true });
});

export const configListRecordsSelector = createSelector(configListSelector, (configsListState: any) => {
  return get(configsListState, ["data", "records"], []);
});

// To get config state
/*
  To use : useParamSelector(getConfigStateByKey,{config_key})
 */
export const getConfigStateByKey = createSelector(
  configListRecordsSelector,
  getConfigKey,
  (records: ConfigStateType[], configKey: string) => {
    return (records || []).find(record => record.name === configKey);
  }
);
