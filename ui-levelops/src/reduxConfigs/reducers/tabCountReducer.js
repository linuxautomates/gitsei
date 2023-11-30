import {
  WORKBENCH_TAB_CLEAR,
  WORKBENCH_TAB_DATA,
  WORKBENCH_TAB_ERROR,
  WORKBENCH_TAB_LOADING
} from "../actions/actionTypes";

const INITIAL_STATE = {
  loading: true,
  error: false,
  data: null
};

const tabCountReducer = (state = INITIAL_STATE, action) => {
  let newState = state;
  switch (action.type) {
    case WORKBENCH_TAB_DATA:
      newState.error = false;
      newState.loading = false;
      newState.data = action.data;
      return JSON.parse(JSON.stringify(newState));
    case WORKBENCH_TAB_LOADING:
      newState.loading = action.loading;
      newState.error = action.loading === true ? false : state.error;
      return JSON.parse(JSON.stringify(newState));
    case WORKBENCH_TAB_ERROR:
      newState.loading = action.error === true ? false : state.loading;
      newState.error = action.error;
      newState.data = action.error === true ? null : state.data;
      return JSON.parse(JSON.stringify(newState));
    case WORKBENCH_TAB_CLEAR:
      return INITIAL_STATE;
    default:
      return newState;
  }
};

export default tabCountReducer;
