import { fromJS } from "immutable";
import { initialIntegrationsState } from "./default-states/integrations.default";
import {
  ACTIVE_INTEGRATION_STEP,
  INTEGRATION_CODE,
  INTEGRATION_COMPLETE,
  INTEGRATION_FORM,
  INTEGRATION_ID,
  INTEGRATION_NAME,
  INTEGRATION_TYPE,
  integrationsActions
} from "reduxConfigs/actions/actionTypes.js";
import { INTEGRATION_SUB_TYPE } from "../actions/actionTypes";

const INITIAL_STATE = fromJS({
  integration_type: null,
  integration_sub_type: null,
  integration_step: 0,
  integration_name: null,
  integration_code: null,
  integration_form: {},
  integration_id: null,
  create: {
    type: "",
    credentials: {},
    information: {},
    state: "",
    step: 0
  }
});

const integrationReducer = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case INTEGRATION_TYPE:
      return state.setIn(["integration_type"], action.payload);

    case INTEGRATION_SUB_TYPE:
      return state.setIn(["integration_sub_type"], action.payload);

    case ACTIVE_INTEGRATION_STEP:
      return state.setIn(["integration_step"], action.payload);

    case INTEGRATION_COMPLETE:
      return INITIAL_STATE;

    case INTEGRATION_NAME:
      return { ...state, integration_name: action.payload };

    case INTEGRATION_CODE:
      return state.setIn(["integration_code", action.payload]);

    case INTEGRATION_FORM:
      const newIntegrationForm = { ...state.getIn(["integration_form"]), ...action.payload };
      return state.setIn(["integration_form"], newIntegrationForm);

    case INTEGRATION_ID:
      return { ...state, integration_id: action.payload };
    case integrationsActions.SET_INTEGRATION_TYPE:
      return state.setIn(["create", "type"], action.payload);
    case integrationsActions.SET_INTEGRATION_STATE:
      return state.setIn(["create", "state"], action.payload);
    case integrationsActions.SET_INTEGRATION_STEP:
      return state.setIn(["create", "step"], action.payload);
    case integrationsActions.SET_INTEGRATION_CREDENTIALS:
      return state.setIn(["create", "credentials", action.payload.field], action.payload.value);
    case integrationsActions.SET_INTEGRATION_INFORMATION:
      return state.setIn(["create", "information", action.payload.field], action.payload.value);
    case integrationsActions.RESET_CREATE_STORE:
      return state.set("create", { ...initialIntegrationsState.create });
    default:
      return state;
  }
};

export default integrationReducer;
