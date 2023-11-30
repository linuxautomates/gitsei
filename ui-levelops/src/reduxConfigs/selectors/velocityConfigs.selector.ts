import { find, get, map } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";
import { RestVelocityConfigs } from "../../classes/RestVelocityConfigs";
import { RestWorkflowProfile } from "classes/RestWorkflowProfile";

export const VELOCITY_CONFIGS: string = "velocity_configs";
export const VELOCITY_CONFIG_LIST_ID: string = "velocity_configs_list_id";
export const VELOCITY_CONFIG_LIST_ID_DEFAULT: string = "velocity_configs_list_id_default";
export const VELOCITY_CONFIG_SEARCH_ID: string = "velocity_configs_search_id";
export const FILTER_VALUES_LIST_ID: string = "filter_values_list_id";
export const VELOCITY_CONFIG_BASIC_TEMPLATE: string = "velocity_configs_basic_template";
export const VELOCITY_CONFIG_BASIC_TEMPLATE_FIXED_STAGE: string = "velocity_configs_basic_template_fixed_stage";

const getID = createParameterSelector((params: any) => params.id || "0");
const getConfigID = createParameterSelector((params: any) => params.config_id || "0");

const velocityConfigsSelector = createSelector(restapiState, (data: any) => {
  return get(data, [VELOCITY_CONFIGS], {});
});

//list
const _velocityConfigsListSelector = createSelector(velocityConfigsSelector, (config: any) => {
  return get(config, ["list"], {});
});

export const velocityConfigsListSelector = createSelector(
  _velocityConfigsListSelector,
  getID,
  (listState: any, listId: string) => {
    return get(listState, [listId], []);
  }
);

export const velocityConfigsListDataSelector = createSelector(velocityConfigsListSelector, data => data);

export const velocityConfigsRestListSelector = createSelector(velocityConfigsListSelector, (response: any) => {
  return map(get(response, ["data", "records"], []), record =>
    record.is_new ? new RestWorkflowProfile(record) : new RestVelocityConfigs(record)
  );
});

//get
const _velocityConfigsGetSelector = createSelector(velocityConfigsSelector, (config: any) => {
  return get(config, ["get"], {});
});

export const velocityConfigsGetSelector = createSelector(
  _velocityConfigsGetSelector,
  getConfigID,
  (getState: any, id: string) => {
    return get(getState, [id, "data"], {});
  }
);

export const velocityConfigsRestGetSelector = createSelector(velocityConfigsGetSelector, (record: any) => {
  return new RestVelocityConfigs(record);
});

//create
const _velocityConfigsCreateSelector = createSelector(velocityConfigsSelector, (config: any) => {
  return get(config, ["create", "new", "data"], {});
});

export const velocityConfigsRestCreateSelector = createSelector(_velocityConfigsCreateSelector, (record: any) => {
  return new RestVelocityConfigs(record);
});

//update
export const _velocityConfigsUpdateSelector = createSelector(velocityConfigsSelector, (config: any) => {
  return get(config, ["update"], {});
});

export const velocityConfigsUpdateSelector = createSelector(
  _velocityConfigsUpdateSelector,
  getID,
  (config: any, id: string) => {
    return get(config, [id], { loading: true, error: false });
  }
);

//delete
export const velocityConfigsDeleteSelector = createSelector(velocityConfigsSelector, (config: any) => {
  return get(config, ["delete"], {});
});

const _velocityConfigsBaseTemplateSelector = createSelector(velocityConfigsSelector, (config: any) => {
  return get(config, ["baseConfig"], {});
});

export const velocityConfigsBaseTemplateSelector = createSelector(
  _velocityConfigsBaseTemplateSelector,
  getID,
  (getState: any, id: string) => {
    return get(getState, [id], {});
  }
);

export const defaultRestVelocityConfigSelector = createSelector(
  velocityConfigsRestListSelector,
  (configs: Array<RestVelocityConfigs | RestWorkflowProfile>) => {
    return (configs ?? []).find(config => !config.is_new && config.defaultConfig);
  }
);

export const getVeloCityConfigById = createSelector(_velocityConfigsListSelector, getID, (state, id) => {
  return find(get(state[VELOCITY_CONFIG_LIST_ID], ["data", "records"], []), record => record.id === id);
});
