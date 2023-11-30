import { cloneDeep, forEach, get, isEqual, set, unset } from "lodash";
import { all, call, put, select, takeLatest } from "redux-saga/effects";

import { DASHBOARD_UPDATE } from "reduxConfigs/actions/actionTypes";
import { getDashboard, selectedDashboard } from "../../selectors/dashboardSelector";
import { restapiEffectSaga } from "../restapiSaga";
import { getWidgetsByDashboardId } from "../../selectors/widgetSelector";
import { RestDashboard, RestWidget } from "../../../classes/RestDashboards";
import {
  dashboardSet,
  dashboardsGet,
  loadDashboardIntegrations,
  restapiClear,
  setEntities,
  setSelectedEntity
} from "reduxConfigs/actions/restapi";
import { _selectedDashboardIntegrations } from "reduxConfigs/selectors/integrationSelector";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { getWidgetTimeConstant } from "./helper";
import { emitEvent } from "dataTracking/google-analytics";
import { AnalyticsCategoryType, WidgetsActions } from "dataTracking/analytics.constants";
import { isDashboardTimerangeEnabled } from "helper/dashboard.helper";

function* dashboardUpdateSaga(action: any): any {
  const { dashboardId, form } = action;
  let newWidget: any = undefined;
  try {
    const selecteddashboard = yield select(selectedDashboard);
    const selectedDashboarQuery = get(selecteddashboard, "query", {});
    const _dashboard: RestDashboard = yield select(getDashboard, { dashboard_id: dashboardId });
    //@ts-ignore
    const selected_dashboard_integrations: any = yield select(_selectedDashboardIntegrations);
    const dashboard: any = { ..._dashboard?.json, query: selectedDashboarQuery };
    const isOUIDsChanged = !isEqual(
      get(action, ["form", "metadata", "ou_ids"], []),
      get(dashboard, ["metadata", "ou_ids"], [])
    );

    const dashboard_time_range = form.metadata.hasOwnProperty("dashboard_time_range")
      ? form.metadata.dashboard_time_range
      : isDashboardTimerangeEnabled(_dashboard.metadata);
    const widgets: RestWidget[] = yield select(getWidgetsByDashboardId, { dashboard_id: dashboardId });
    forEach(Object.keys(form || {}), key => {
      if (["metadata", "query"].includes(key)) {
        const newData = cloneDeep(dashboard[key]);
        forEach(Object.keys(form?.[key] || {}), formKey => {
          newData[formKey] = form?.[key]?.[formKey];
        });
        dashboard[key] = newData;
      } else {
        dashboard[key] = form?.[key];
      }
    });
    let leftOutKeys: any = {};
    forEach(Object.keys(dashboard), key => {
      if (!Object.keys(form || {}).includes(key)) {
        leftOutKeys[key] = dashboard?.[key];
      }
    });

    if (action?.dashboardSetting || isOUIDsChanged) {
      const updatedWidgets: any = [];
      const updatedIds: any = [];

      widgets.forEach((widget: any) => {
        let widgetMetaData = { ...widget.metadata };
        const widget_time_object = getWidgetTimeConstant(widgetMetaData, !!dashboard_time_range);
        widgetMetaData.dashBoard_time_keys = widget_time_object;
        const widget_time_update_keys = Object.keys(widget_time_object) || [];
        if (widget_time_update_keys.length) {
          updatedIds.push(widget.id);
          set(widget, ["metadata"], widgetMetaData);
        }
        updatedWidgets.push(widget);
      });

      dashboard.widgets = updatedWidgets.map((w: RestWidget) => {
        const widgetJson = w.json;
        if (newWidget === undefined && widgetJson.draft) {
          newWidget = widgetJson;
        }
        unset(widgetJson, "draft");
        return widgetJson;
      });
      unset(dashboard, "draft");

      const _widgets = updatedWidgets.map((widget: RestWidget) => {
        return widget.json;
      });

      let clearWidgets: any = [];
      forEach(updatedIds, (id: string) => {
        const curWidget = _widgets.find((widget: any) => widget.id === id);
        if (curWidget) {
          clearWidgets.push(curWidget);
        }
      });

      yield all(
        clearWidgets.reduce((acc: any, widget: any) => {
          return [
            ...acc,
            put(
              restapiClear(getWidgetConstant(widget.type, "uri"), getWidgetConstant(widget.type, "method"), widget.id)
            ),
            put(
              restapiClear(
                getWidgetConstant(widget.type, "uri"),
                getWidgetConstant(widget.type, "method"),
                `${widget.id}-preview`
              )
            )
          ];
        }, [])
      );

      yield put(setEntities(_widgets, "widgets"));
    } else {
      dashboard.widgets = widgets.map((w: RestWidget) => {
        const widgetJson = w.json;
        if (newWidget === undefined && widgetJson.draft) {
          newWidget = widgetJson;
        }
        unset(widgetJson, "draft");
        return widgetJson;
      });
      unset(dashboard, "draft");
    }
    yield put(dashboardSet(dashboardId, dashboard));
    yield call(restapiEffectSaga, { uri: "dashboards", method: "update", id: dashboardId, data: dashboard });

    const previous_integrations = get(selected_dashboard_integrations, ["records"], []).map(
      (integration: any) => integration.id
    );
    const updated_integrations = get(dashboard, ["query", "integration_ids"], []);
    if (!isEqual(previous_integrations.sort(), updated_integrations.sort())) {
      yield all(
        widgets.map((widget: any) =>
          put(restapiClear(getWidgetConstant(widget.type, "uri"), getWidgetConstant(widget.type, "method"), widget.id))
        )
      );
      yield put(loadDashboardIntegrations(dashboardId));
    }
    if (newWidget) {
      // GA event WIDGET
      emitEvent(AnalyticsCategoryType.WIDGETS, WidgetsActions.CREATED, newWidget?.type);
    }
    if (newWidget || action?.clearDashboard) {
      yield put(restapiClear("dashboards", "get", dashboardId));
    }
    if (action?.dashboardSetting) {
      yield put(dashboardsGet(dashboardId));
    }
  } catch (e) {
    console.error("Failed to delete widget", e);
  }
}

export function* dashboardUpdateSagaWatcher() {
  yield takeLatest(DASHBOARD_UPDATE, dashboardUpdateSaga);
}
