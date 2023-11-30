import { put, select, take, takeLatest } from "redux-saga/effects";

import { DASHBOARD_WIDGET_REMOVE } from "reduxConfigs/actions/actionTypes";
import { widgetBulkDeleteCall, widgetDeleteCall } from "../../actions/restapi/widgetActions";
import { getWidget, getWidgets } from "../../selectors/widgetSelector";
import { RestWidget } from "../../../classes/RestDashboards";
import { setEntities } from "../../actions/restapi/restapiActions";

function* deleteWidgetSaga(action: any) {
  const { widgetId } = action;
  const WIDGET_DELETED_ACTION = "WIDGET_DELETED_ACTION";

  let widget: RestWidget = yield select(getWidget, { widget_id: widgetId });
  const updatedWidgets: any = [];

  try {
    const dashboardId = widget.dashboard_id;
    if (!widget.draft) {
      if (widget.isComposite) {
        const childWidgetIds = [...widget.children];
        childWidgetIds.push(widgetId);
        yield put(widgetBulkDeleteCall(dashboardId, widgetId, childWidgetIds, WIDGET_DELETED_ACTION));
      } else {
        yield put(widgetDeleteCall(dashboardId, widgetId, WIDGET_DELETED_ACTION));
      }

      yield take(WIDGET_DELETED_ACTION);

      widget = yield select(getWidget, { widget_id: widgetId });
      if (widget.error) {
        console.log(widget.error);
      }

      if (widget.isComposite) {
        const childWidgetIds = widget.children;
        const childWidgets: RestWidget[] = yield select(getWidgets, { widget_ids: childWidgetIds });
        childWidgets.map((childWidget: RestWidget) => {
          childWidget.deleting = false;
          childWidget.deleted = true;
          updatedWidgets.push(childWidget.json);
        });
      }
    }
    widget.deleting = false;
    widget.deleted = true;
    updatedWidgets.push(widget.json);
    yield put(setEntities(updatedWidgets, "widgets"));
  } catch (e) {
    console.error("Failed to delete widget", e);
  }
}

export function* deleteWidgetSagaWatcher() {
  yield takeLatest(DASHBOARD_WIDGET_REMOVE, deleteWidgetSaga);
}
