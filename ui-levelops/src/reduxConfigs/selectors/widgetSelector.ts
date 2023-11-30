import { filter, get } from "lodash";
import { createSelector } from "reselect";
import { createParameterSelector } from "./selector";
import { restapiState } from "./restapiSelector";
import { RestWidget } from "../../classes/RestDashboards";

const WIDGETS = "widgets";
export const SELECTED_CHILD_ID = "selected_child_id";

const getWidgetId = createParameterSelector((params: any) => params.widget_id);
const getWidgetIds = createParameterSelector((params: any) => params.widget_ids);
const getDashboardId = createParameterSelector((params: any) => params.dashboard_id);

export const widgetsSelector = createSelector(restapiState, (data: any) => {
  return get(data, [WIDGETS], {});
});

export const getWidgets = createSelector(widgetsSelector, getWidgetIds, (widgets: any[], widgetIds: string[]) => {
  return filter(widgets, function (widget: any) {
    return widgetIds.includes(widget.id) && !widget.deleted;
  });
});

export const getWidgetsByDashboardId = createSelector(
  widgetsSelector,
  getDashboardId,
  (widgets: any, dashboardId: string) => {
    const _widgets = Object.values(widgets);

    return filter(
      _widgets.map((w: any) => new RestWidget(w)),
      function (widget: any) {
        return widget.dashboard_id === dashboardId && !widget.deleted && RestWidget.isValidWidget(widget);
      }
    );
  }
);

const _getWidget = createSelector(widgetsSelector, getWidgetId, (widgets: any, widgetId: string) => {
  const _widget = widgets[widgetId];
  if (!_widget) {
    return null;
  }
  return _widget;
});

export const getWidget = createSelector(_getWidget, (widget: any) => {
  if (!widget) {
    return null;
  }
  return new RestWidget(widget);
});

// Discuss it later on... added by @hemant.
export const getChildWidget = createSelector(_getWidget, (widget: any) => {
  if (!widget) {
    return null;
  }
  return new RestWidget(widget);
});

export const getDrillDownWidget = createSelector(_getWidget, (widget: any) => {
  if (!widget) {
    return null;
  }
  return new RestWidget(widget);
});

export const getChildWidgetId = createSelector(restapiState, (state: any) => state[SELECTED_CHILD_ID]);

export const getSelectedChildWidget = createSelector(
  widgetsSelector,
  getChildWidgetId,
  (widgets: any, widgetId: string) => {
    const _widget = widgets[widgetId];
    if (!_widget) {
      return null;
    }
    return new RestWidget(_widget);
  }
);
