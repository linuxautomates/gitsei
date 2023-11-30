import { devRawStatGraphAPIActions } from "reduxConfigs/actions/actionTypes";

export interface getWidgetGraphAPIActionType {
  type: string;
  widgetId: string;
  filter: any;
}

export const getWidgetGraphDataAction = (widgetId: string, filter: any): getWidgetGraphAPIActionType => ({
  type: devRawStatGraphAPIActions.GET_GRAPH_DATA,
  widgetId,
  filter
});

export interface getWidgetGraphDataFailedActionType {
  type: string;
  widgetId: string;
  error: any;
}
export const getWidgetGraphDataFailedAction = (widgetId: string, error: any): getWidgetGraphDataFailedActionType => ({
  type: devRawStatGraphAPIActions.GET_GRAPH_DATA_FAILED,
  widgetId,
  error
});

export interface getWidgetGraphDataSuccessActionType {
  type: string;
  widgetId: string;
  data: any;
}
export const getWidgetGraphDataSuccessAction = (widgetId: string, data: any): getWidgetGraphDataSuccessActionType => ({
  type: devRawStatGraphAPIActions.GET_GRAPH_DATA_SUCCESS,
  widgetId,
  data
});
