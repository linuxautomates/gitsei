import { devRawStatGraphAPIActions } from "reduxConfigs/actions/actionTypes";
import {
  getWidgetGraphAPIActionType,
  getWidgetGraphDataFailedActionType,
  getWidgetGraphDataSuccessActionType
} from "reduxConfigs/actions/widgetGraphAPIActions";
import { createReducer } from "../createReducer";

const INITIAL_STATE = {};

const getWidgetGraphData = (state: any, action: getWidgetGraphAPIActionType) => ({
  ...state,
  [action.widgetId]: {
    isLoading: true
  }
});
const getWidgetGraphDataFailed = (state: any, action: getWidgetGraphDataFailedActionType) => ({
  ...state,
  [action.widgetId]: {
    isLoading: false,
    error: action.error
  }
});
const getWidgetGraphDataSuccess = (state: any, action: getWidgetGraphDataSuccessActionType) => ({
  ...state,
  [action.widgetId]: {
    isLoading: false,
    data: action.data
  }
});

const widgetGraphAPIReducer = createReducer(INITIAL_STATE, {
  [devRawStatGraphAPIActions.GET_GRAPH_DATA]: getWidgetGraphData,
  [devRawStatGraphAPIActions.GET_GRAPH_DATA_FAILED]: getWidgetGraphDataFailed,
  [devRawStatGraphAPIActions.GET_GRAPH_DATA_SUCCESS]: getWidgetGraphDataSuccess
});

export default widgetGraphAPIReducer;
