import { put, select, takeLatest } from "redux-saga/effects";

import { DASHBOARD_WIDGET_REORDER } from "reduxConfigs/actions/actionTypes";
import { RestWidget } from "../../../classes/RestDashboards";
import { WidgetOrderConfig } from "../../../dashboard/components/rearrange-grid/helper";
import { setEntities } from "../../actions/restapi/restapiActions";
import { getWidgetsFromType } from "../../../dashboard/helpers/dashboardWidgetTypeFilter.helper";
import { getWidgetsByDashboardId } from "../../selectors/widgetSelector";

const resetWidgetOrder = (widgets: RestWidget[], widgetOrderList: WidgetOrderConfig[]) => {
  widgets.forEach((widget: RestWidget) => {
    const widgetConfig = widgetOrderList.find((widgetConfig: WidgetOrderConfig) => widgetConfig.id === widget.id);
    if (widgetConfig) {
      widget.order = widgetConfig.order;
      widget.width = widgetConfig.width;
    }
  });
  return widgets;
};

function* changeWidgetsOrderSaga(action: any): any {
  const { dashboardId, widgetOrderList, widgetType } = action;
  try {
    const widgets: RestWidget[] = yield select(getWidgetsByDashboardId, { dashboard_id: dashboardId });
    const selectedWidgets = getWidgetsFromType(widgets, widgetType, true);
    const reOrderedWidgets = resetWidgetOrder(selectedWidgets, widgetOrderList);
    const _widgets = widgets.map((widget: RestWidget) => {
      const isChanged = reOrderedWidgets.find(reOrderedWidget => reOrderedWidget.id === widget.id);
      if (isChanged) {
        widget.order = isChanged.order;
      }
      return widget.json;
    });
    yield put(setEntities(_widgets, "widgets"));
  } catch (e) {
    console.error("Failed change widget order", e);
  }
}

export function* changeWidgetsOrderSagaWatcher() {
  yield takeLatest(DASHBOARD_WIDGET_REORDER, changeWidgetsOrderSaga);
}
