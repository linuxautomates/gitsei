import { get } from "lodash";
import { createSelector } from "reselect";
import { createParameterSelector } from "./selector";

export const widgetGraphAPIState = (state: any) => state.widgetGraphAPIReducer;

const getWidgetId = createParameterSelector((params: any) => params.widgetId);

export const getWidgetGraphDataSelector = createSelector(
  widgetGraphAPIState,
  getWidgetId,
  (state: any, widgetId: string) => get(state, [widgetId], { isLoading: true })
);
