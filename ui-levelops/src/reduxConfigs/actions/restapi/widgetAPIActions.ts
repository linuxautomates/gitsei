import {
  DORA_LEAD_TIME_FOR_CHANGE_GET_DATA,
  DORA_LEAD_TIME_GET_DATA,
  DORA_MEAN_TIME_TO_RESTORE_GET_DATA,
  widgetAPIActions
} from "reduxConfigs/actions/actionTypes";

export interface getWidgetAPIActionType {
  type: string;
  reportType: string;
  widgetId: string;
  filter: any;
}

export const getWidgetaDataAction = (reportType: string, widgetId: string, filter: any): getWidgetAPIActionType => ({
  type: widgetAPIActions.GET_DATA,
  reportType,
  widgetId,
  filter
});

export const getDoraLeadTimeWidgetDataAction = (
  reportType: string,
  widgetId: string,
  filter: any
): getWidgetAPIActionType => ({
  type: DORA_LEAD_TIME_GET_DATA,
  reportType,
  widgetId,
  filter
});

export interface getWidgetDataFailedActionType {
  type: string;
  widgetId: string;
  error: any;
}
export const getWidgetDataFailedAction = (widgetId: string, error: any): getWidgetDataFailedActionType => ({
  type: widgetAPIActions.GET_DATA_FAILED,
  widgetId,
  error
});

export interface getWidgetDataSuccessActionType {
  type: string;
  widgetId: string;
  data: any;
}
export const getWidgetDataSuccessAction = (widgetId: string, data: any): getWidgetDataSuccessActionType => ({
  type: widgetAPIActions.GET_DATA_SUCCESS,
  widgetId,
  data
});

export const getLeadTimeWidgetDataAction = (
  reportType: string,
  widgetId: string,
  filter: any
): getWidgetAPIActionType => ({
  type: DORA_LEAD_TIME_FOR_CHANGE_GET_DATA,
  reportType,
  widgetId,
  filter
});

export const getMeanTimeWidgetDataAction = (
  reportType: string,
  widgetId: string,
  filter: any
): getWidgetAPIActionType => ({
  type: DORA_MEAN_TIME_TO_RESTORE_GET_DATA,
  reportType,
  widgetId,
  filter
});
