import { call, put, select, takeLatest } from "redux-saga/effects";
import {
  DASHBOARD_REMOTE_WIDGET_UPDATE,
  WIDGET_DRILLDOWN_COLUMNS_UPDATE,
  WIDGET_METADATA_FIELD_UPDATE,
  WIDGET_SELECTED_COLUMNS_UPDATE,
  WIDGET_TABLE_FILTERS_UPDATE
} from "reduxConfigs/actions/actionTypes";
import LocalStoreService from "../../../services/localStoreService";
import { getWidget, widgetsSelector } from "../../selectors/widgetSelector";
import { RestWidget } from "../../../classes/RestDashboards";
import { cloneDeep, forEach, get } from "lodash";
import { WidgetType } from "dashboard/helpers/helper";
import { widgetUpdate, _widgetUpdateCall } from "reduxConfigs/actions/restapi";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { USER_ROLE_TYPE_ADMIN } from "../../../classes/RestUsers";
import { setEntity } from "../../actions/restapi";
import { notification } from "antd";
import { getIsStandaloneApp } from "helper/helper";

function* updateWidgetSaga(action: { type: string; widgetId: string; form: any }) {
  const { widgetId, form } = action;
  try {
    let widget: RestWidget = yield select(getWidget, { widget_id: widgetId });
    // @ts-ignore
    let widgets = yield select(widgetsSelector);
    forEach(Object.keys(form || {}), key => {
      const value = form?.[key];
      if (key === "metadata") {
        let newMetadata = cloneDeep(widget.metadata);
        forEach(Object.keys(value || {}), mKey => {
          const metaValue = value?.[mKey];
          if (
            mKey === "widget_type" &&
            widget.metadata.widget_type === WidgetType.COMPOSITE_GRAPH &&
            metaValue !== WidgetType.COMPOSITE_GRAPH
          ) {
            // Delete child of COMPOSITE_GRAPH on widget_type change...
            const _children = get(widget, ["metadata", "children"], []);
            _children.forEach((id: string) => delete widgets[id]);
            newMetadata["children"] = [];
          }
          newMetadata[mKey] = metaValue;
        });
        widget.metadata = newMetadata;
      } else {
        (widget as any)[key] = action.form?.[key];
      }
    });
    yield put(widgetUpdate(widgetId, form));
    yield put(_widgetUpdateCall(widget.dashboard_id, widget));
  } catch (e) {
    handleError({
      bugsnag: {
        // @ts-ignore
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

export function* updateWidgetSagaWatcher() {
  yield takeLatest(DASHBOARD_REMOTE_WIDGET_UPDATE, updateWidgetSaga);
}

function* widgetDrilldownColumnUpdateSaga(action: any) {
  const { widgetId, drilldownColumn } = action;
  try {
    const widget: RestWidget = yield select(getWidget, { widget_id: widgetId });
    widget.drilldown_columns = drilldownColumn;
    yield call(processWidgetUpdateForSelectedColumnUpdate, action, widget);
  } catch (e) {
    handleError({
      bugsnag: {
        // @ts-ignore
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

export function* widgetDrilldownColumnUpdateSagaWatcher() {
  yield takeLatest(WIDGET_DRILLDOWN_COLUMNS_UPDATE, widgetDrilldownColumnUpdateSaga);
}

function* widgetSelectedColumnUpdateSaga(action: any) {
  const { widgetId, selectedColumns } = action;
  try {
    const widget: RestWidget = yield select(getWidget, { widget_id: widgetId });
    widget.selected_columns = selectedColumns;
    yield call(processWidgetUpdateForSelectedColumnUpdate, action, widget);
  } catch (e) {
    handleError({
      bugsnag: {
        // @ts-ignore
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

// @ts-ignore
function* processWidgetUpdateForSelectedColumnUpdate(action, widget) {
  try {
    const ls = new LocalStoreService();
    const userRole = ls.getUserRbac();

    /**
     * admins are allowed to update/save the widget through DB, all other roles cannot
     */
    if (!getIsStandaloneApp() || userRole === USER_ROLE_TYPE_ADMIN) {
      yield put(_widgetUpdateCall(widget.dashboard_id, widget));
    } else {
      yield put(setEntity(widget, action.uri, widget.dashboard_id));

      // Display notification for the user that the action is not saved
      notification.info({
        message:
          "The columns to display will not be saved, if you want to save the columns to display please contact your Admin"
      });
    }
  } catch (e) {
    handleError({
      bugsnag: {
        // @ts-ignore
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

export function* widgetSelectedColumnUpdateSagaWatcher() {
  yield takeLatest(WIDGET_SELECTED_COLUMNS_UPDATE, widgetSelectedColumnUpdateSaga);
}

function* widgetTableFiltersUpdateSaga(action: any) {
  const { widgetId, filters } = action;
  try {
    const widget: RestWidget = yield select(getWidget, { widget_id: widgetId });
    widget.table_filters = filters;
    yield put(_widgetUpdateCall(widget.dashboard_id, widget));
  } catch (e) {
    handleError({
      bugsnag: {
        // @ts-ignore
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

export function* widgetTableFiltersUpdateSagaWatcher() {
  yield takeLatest(WIDGET_TABLE_FILTERS_UPDATE, widgetTableFiltersUpdateSaga);
}

function* widgetMetaDataUpdateSaga(action: any) {
  const { widgetId, value } = action;
  try {
    const widget: RestWidget = yield select(getWidget, { widget_id: widgetId });
    widget.metadata = {
      ...(widget.metadata || {}),
      harness_profile_stacks: value
    };
    yield put(_widgetUpdateCall(widget.dashboard_id, widget));
  } catch (e) {
    handleError({
      bugsnag: {
        // @ts-ignore
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

export function* widgetMetaDataUpdateSagaWatcher() {
  yield takeLatest(WIDGET_METADATA_FIELD_UPDATE, widgetMetaDataUpdateSaga);
}
