import { get } from "lodash";
import { createSelector } from "reselect";
import { createParameterSelector } from "./selector";
import { restapiState } from "./restapiSelector";
import { RestDashboard, RestWidget } from "../../classes/RestDashboards";
import { WidgetType } from "dashboard/helpers/helper";
import { DEFAULT_DASHBOARD_KEY } from "../../dashboard/constants/constants";
import { getWidgetsByDashboardId } from "./widgetSelector";
import { COPY_DESTINATION_DASHBOARD_NODE } from "dashboard/constants/filter-key.mapping";
import { dashboardAccess } from "dashboard/components/dashboard-settings-modal/helper";

const DASHBOARDS = "dashboards";

const getID = createParameterSelector((params: any) => params.dashboard_id);
const getWidgetType = createParameterSelector((params: any) => params.widget_type);
const getWidgetId = createParameterSelector((params: any) => params?.widget_id);

export const dashboardsSelector = createSelector(restapiState, (data: any) => {
  return get(data, [DASHBOARDS], {});
});

const _selectedDashboard = createSelector(restapiState, (data: any) => {
  return get(data, "selected-dashboard", undefined);
});

export const selectedDashboard = createSelector(_selectedDashboard, (dashboard: any) => {
  if (!dashboard) {
    return null;
  }
  return new RestDashboard(dashboard);
});

export const _dashboardsListSelector = createSelector(dashboardsSelector, (dashboards: any) => {
  return get(dashboards, ["list"], {});
});

export const _dashboardsGetSelector = createSelector(dashboardsSelector, (dashboards: any) => {
  return get(dashboards, ["get"], {});
});

export const _dashboardsCreateSelector = createSelector(dashboardsSelector, (dashboards: any) => {
  return get(dashboards, ["create", "0"], { loading: true, error: true });
});

export const dashboardsCreateSelector = createSelector(dashboardsSelector, (dashboards: any) => {
  return get(dashboards, ["create"], { loading: true, error: true });
});

export const _dashboardsUpdateSelector = createSelector(dashboardsSelector, (dashboards: any) => {
  return get(dashboards, ["update"], {});
});

export const _dashboardsDeleteSelector = createSelector(dashboardsSelector, (dashboards: any) => {
  return get(dashboards, ["delete"], {});
});

export const dashboardsGetSelector = createSelector(_dashboardsGetSelector, getID, (dashboards: any, id: string) => {
  return get(dashboards, [id], { loading: true, error: false });
});

export const dashboardsUpdateSelector = createSelector(
  _dashboardsUpdateSelector,
  getID,
  (dashboards: any, id: string) => {
    return get(dashboards, [id], { loading: true, error: false });
  }
);

export const getDashboard = createSelector(dashboardsGetSelector, (state: any) => {
  const dashboard = get(state, ["data"], undefined);
  return new RestDashboard(dashboard);
});

export const dashboardWidgetsSelector = createSelector(getWidgetsByDashboardId, (widgets: any) => {
  return widgets;
});

export const _dashboardWidgetSelector = createSelector(
  dashboardWidgetsSelector,
  getWidgetId,
  (widgets: any[], id: string) => {
    return (widgets || []).find(widget => widget.id === id);
  }
);

export const dashboardWidgetChildrenSelector = createSelector(
  dashboardWidgetsSelector,
  getWidgetId,
  (widgets: RestWidget[], id: string) => {
    const widget: RestWidget | undefined = widgets.find(w => w.id === id);
    if (!widget) {
      return [];
    }
    return (widget.children || [])
      .map((child: string) => {
        const wChild = widgets.find((w: { id: string }) => w.id === child);
        if (wChild) {
          return wChild;
        }
      })
      .filter((childWidget: RestWidget | undefined) => !!childWidget);
  }
);

export const dashboardWidgetsDataSelector = createSelector(
  getWidgetsByDashboardId,
  getWidgetType,
  (widgets: RestWidget[], widget_type: string) => {
    let _widgets = [
      ...widgets.filter(
        (widget: { widget_type: string; hidden: boolean }) => widget.widget_type.includes(widget_type) && !widget.hidden
      )
    ];

    if (widget_type === WidgetType.GRAPH) {
      const configWidgets = widgets.filter((widget: any) => widget.widget_type === WidgetType.CONFIGURE_WIDGET);
      _widgets = [..._widgets, ...configWidgets];
    }

    return {
      widgets: _widgets.sort((a, b) => a.order - b.order),
      stats_count: _widgets.filter((w: any) => w.widget_type.includes(WidgetType.STATS)).length
    };
  }
);

export const dashboardGraphWidgetsSelector = createSelector(dashboardWidgetsSelector, (widgets: RestWidget[]) => {
  return widgets
    .filter(
      (widget: { widget_type: string; hidden: boolean }) =>
        (widget.widget_type.includes(WidgetType.GRAPH) || widget.widget_type === WidgetType.CONFIGURE_WIDGET) &&
        !widget.hidden
    )
    .sort((a: any, b: any) => a.order - b.order);
});

export const dashboardStatWidgetsSelector = createSelector(dashboardWidgetsSelector, (widgets: RestWidget[]) => {
  return widgets
    .filter(
      (widget: { widget_type: string; hidden: boolean }) =>
        widget.widget_type.includes(WidgetType.STATS) && !widget.hidden
    )
    .sort((a: any, b: any) => a.order - b.order);
});

export const defaultDashboardSelector = createSelector(_dashboardsListSelector, (dashboardList: any) => {
  return get(dashboardList, [DEFAULT_DASHBOARD_KEY], { loading: true, error: false });
});

const _copyDashboardGetStatus = createSelector(restapiState, (data: any) => {
  return get(data, [COPY_DESTINATION_DASHBOARD_NODE], {});
});

export const copyDashboardGetStatus = createSelector(
  _copyDashboardGetStatus,
  getID,
  (data: any, dashboard_id: string) => {
    return get(data, [dashboard_id], false);
  }
);

export const isDashboardHasAccessSelector = createSelector(_selectedDashboard, (dashboard: any) => {
  return dashboardAccess(dashboard);
});

export const customDataLoadingSelector = createSelector(restapiState, (data: any) => {
  return get(data, ["custom-data-loading", "loading"], true);
});
