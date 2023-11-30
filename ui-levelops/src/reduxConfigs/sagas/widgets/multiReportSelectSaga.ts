import { LEVELOPS_MULTITIME_SERIES_REPORT } from "dashboard/constants/applications/multiTimeSeries.application";
import { put, select, takeLatest } from "redux-saga/effects";
import {
  MULTI_REPORT_WIDGET_REPORT_ADD,
  MULTI_REPORT_WIDGET_REPORT_DELETE,
  MULTI_REPORT_WIDGET_REPORT_NAME_UPDATE
} from "reduxConfigs/actions/actionTypes";
import { updateMultiTimeSeriesReport, updateWidgetFiltersForReport } from "utils/widgetUtils";
import { v1 as uuid } from "uuid";
import { RestDashboard, RestWidget } from "../../../classes/RestDashboards";
import { getReportNameByKey } from "../../../utils/reportListUtils";
import { deleteEntities, setEntities, setEntity, widgetUpdate } from "../../actions/restapi";
import { dashboardWidgetChildrenSelector, getDashboard } from "../../selectors/dashboardSelector";
import { getWidget } from "../../selectors/widgetSelector";

function* multiWidgetReportAddSaga(action: { type: string; widgetId: string; selected_report: string }) {
  const { widgetId, selected_report } = action;

  let widget: RestWidget = yield select(getWidget, { widget_id: widgetId });
  const dashboardId = widget.dashboard_id;
  let dashboard: RestDashboard = yield select(getDashboard, { dashboard_id: dashboardId });
  const childWidgets: RestWidget[] = yield select(dashboardWidgetChildrenSelector, {
    widget_id: widgetId,
    dashboard_id: dashboardId
  });

  try {
    const newWidgetId = uuid();
    const graphWidget: RestWidget = RestWidget.newInstance(dashboard, newWidgetId, "graph", "", true, selected_report);
    graphWidget.name = `Report ${childWidgets.length + 1} - ${getReportNameByKey(selected_report)}`;
    graphWidget.isMultiTimeSeriesReport = widget.type === LEVELOPS_MULTITIME_SERIES_REPORT;
    let updatedWidget = updateWidgetFiltersForReport(
      graphWidget,
      graphWidget.type,
      dashboard?.global_filters,
      dashboard
    );
    if (updatedWidget.isMultiTimeSeriesReport) {
      const multiTimeSeries = widget?.metadata?.multi_series_time || "quarter";
      updatedWidget = updateMultiTimeSeriesReport(updatedWidget, multiTimeSeries);
    }
    const _children = widget.children;
    _children.push(newWidgetId);
    widget.children = _children;
    yield put(setEntities([updatedWidget.json, widget.json], "widgets"));
  } catch (e) {
    console.error("Failed to update widget", e);
  }
}

function* multiWidgetReportDeleteSaga(action: any) {
  const { widgetId, deleted_report } = action;

  let widget: RestWidget = yield select(getWidget, { widget_id: widgetId });
  const dashboardId = widget.dashboard_id;
  const childWidgets: RestWidget[] = yield select(dashboardWidgetChildrenSelector, {
    widget_id: widgetId,
    dashboard_id: dashboardId
  });

  try {
    const _children = childWidgets.filter((w: RestWidget) => w.id !== deleted_report).map((w: RestWidget) => w.id);
    widget.children = _children;
    yield put(deleteEntities([deleted_report], "widgets"));
    yield put(widgetUpdate(widgetId, { ...widget.json }));
  } catch (e) {
    console.error("Failed to update widget", e);
  }
}

function* multiWidgetReportNameUpateSaga(action: any) {
  const { widgetId, report_name } = action;

  const widget: RestWidget = yield select(getWidget, { widget_id: widgetId });

  try {
    if (widget) {
      widget.name = report_name;
      yield put(setEntity(widget.json, "widgets", widgetId));
    }
  } catch (e) {
    console.error("Failed to update widget", e);
  }
}

export function* multiWidgetReportAddSagaWatcher() {
  yield takeLatest(MULTI_REPORT_WIDGET_REPORT_ADD, multiWidgetReportAddSaga);
}

export function* multiWidgetReportDeleteSagaWatcher() {
  yield takeLatest(MULTI_REPORT_WIDGET_REPORT_DELETE, multiWidgetReportDeleteSaga);
}

export function* multiWidgetReportNameUpdateSagaWatcher() {
  yield takeLatest(MULTI_REPORT_WIDGET_REPORT_NAME_UPDATE, multiWidgetReportNameUpateSaga);
}
