import { all, put, select, takeLatest } from "redux-saga/effects";

import { DASHBOARD_WIDGET_BULK_UPDATE } from "reduxConfigs/actions/actionTypes";
import { restapiClear, setEntities } from "../../actions/restapi/restapiActions";
import { getWidgetsByDashboardId } from "../../selectors/widgetSelector";
import { updateLayoutWithNewApplicationFilters } from "../../../dashboard/components/dashboard-application-filters/helper";
import { cloneDeep, forEach } from "lodash";
import { newDashboardUpdate } from "reduxConfigs/actions/restapi";
import { RestWidget } from "classes/RestDashboards";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { OU_USER_FILTER_DESIGNATION_KEY } from "dashboard/components/dashboard-application-filters/constants";
import { sanitizeFilters } from "utils/filtersUtils";

function* bulkUpdateWidgetSaga(action: any): any {
  const { dashboardId, filters } = action;
  try {
    const widgets = yield select(getWidgetsByDashboardId, { dashboard_id: dashboardId });
    const [updatedWidgets, updatedIds] = updateLayoutWithNewApplicationFilters(
      cloneDeep(widgets || []),
      filters.global_filters
    );
    const _widgets = updatedWidgets.map((widget: RestWidget) => {
      return widget.json;
    });

    let sanitizedFilters = {};

    forEach(Object.keys(filters.global_filters), key => {
      sanitizedFilters = {
        ...sanitizedFilters,
        [key]: sanitizeFilters(filters.global_filters[key])
      };
    });

    sanitizedFilters = sanitizeFilters(sanitizedFilters);

    let form: any = {
      widgets: _widgets,
      metadata: {
        global_filters: sanitizedFilters,
        jira_or_query: sanitizeFilters(filters.jira_or_filters),
        [OU_USER_FILTER_DESIGNATION_KEY]: filters[OU_USER_FILTER_DESIGNATION_KEY]
      }
    };

    let clearWidgets: any = [];
    forEach(updatedIds, (id: string) => {
      const curWidget = _widgets.find(widget => widget.id === id);
      if (curWidget) {
        clearWidgets.push(curWidget);
      }
    });

    // Fix for widgets not updating on changing global filters
    yield all(
      clearWidgets.map((widget: any) =>
        put(restapiClear(getWidgetConstant(widget.type, "uri"), getWidgetConstant(widget.type, "method"), widget.id))
      )
    );

    yield put(setEntities(_widgets, "widgets"));
    yield put(newDashboardUpdate(dashboardId, form));
  } catch (e) {
    console.error("Failed change widget order", e);
  }
}

export function* bulkUpdateWidgetSagaWatcher() {
  yield takeLatest(DASHBOARD_WIDGET_BULK_UPDATE, bulkUpdateWidgetSaga);
}
