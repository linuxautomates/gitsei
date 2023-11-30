import { get } from "lodash";
import { createSelector } from "reselect";
import { createParameterSelector } from "./selector";

export const widgetAPIState = (state: any) => state.widgetAPIReducer;

const getWidgetId = createParameterSelector((params: any) => params.widgetId);

export const getWidgetDataSelector = createSelector(widgetAPIState, getWidgetId, (state: any, widgetId: string) =>
  get(state, [widgetId], { isLoading: true })
);
