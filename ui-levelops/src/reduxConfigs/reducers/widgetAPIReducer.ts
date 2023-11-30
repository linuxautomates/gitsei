import { widgetAPIActions } from "reduxConfigs/actions/actionTypes";
import {
  getWidgetAPIActionType,
  getWidgetDataFailedActionType,
  getWidgetDataSuccessActionType
} from "reduxConfigs/actions/restapi/widgetAPIActions";
import { createReducer } from "../createReducer";

const INITIAL_STATE = {};

const getWidgetData = (state: any, action: getWidgetAPIActionType) => ({
  ...state,
  [action.widgetId]: {
    isLoading: true
  }
});
const getWidgetDataFailed = (state: any, action: getWidgetDataFailedActionType) => ({
  ...state,
  [action.widgetId]: {
    isLoading: false,
    error: action.error
  }
});
const getWidgetDataSuccess = (state: any, action: getWidgetDataSuccessActionType) => ({
  ...state,
  [action.widgetId]: {
    isLoading: false,
    data: action.data
  }
});

const widgetAPIReducer = createReducer(INITIAL_STATE, {
  [widgetAPIActions.GET_DATA]: getWidgetData,
  [widgetAPIActions.GET_DATA_FAILED]: getWidgetDataFailed,
  [widgetAPIActions.GET_DATA_SUCCESS]: getWidgetDataSuccess
});

export default widgetAPIReducer;
