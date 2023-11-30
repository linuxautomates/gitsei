import { all, put, select, takeLatest } from "redux-saga/effects";

import { DASHBOARD_TIME_UPDATE } from "reduxConfigs/actions/actionTypes";

import { forEach, get } from "lodash";
import { newDashboardUpdate, restapiClear, setEntities } from "reduxConfigs/actions/restapi";
import { RestWidget } from "classes/RestDashboards";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { getWidgetsByDashboardId } from "reduxConfigs/selectors/widgetSelector";
import { AnalyticsCategoryType, DataGlobalFiltersActions } from "dataTracking/analytics.constants";
import { emitEvent } from "dataTracking/google-analytics";
import { handleError } from "helper/errorReporting.helper";
import { issueContextTypes, severityTypes } from "bugsnag";

function* dashboardTimeUpdateWidgets(action: any) {
  const { dashboardId, filters } = action;
  //we should not update the query here so previous value should not replaced by the new value
  try {
    //@ts-ignore
    const widgets = yield select(getWidgetsByDashboardId, { dashboard_id: dashboardId });
    const updatedWidgets: any = [];
    const updatedIds: any = [];
    const dashboard_time_range_filter = filters.dashboard_time_range_filter;
    widgets.forEach((widget: any) => {
      const widgetMetaData = widget.metadata;
      const widget_time_object = widgetMetaData?.dashBoard_time_keys || {};
      const widget_time_update_keys = Object.keys(widget_time_object).filter(
        (item: any) => widget_time_object[item].use_dashboard_time
      );
      if (widget_time_update_keys.length) {
        updatedIds.push(widget.id);
      }
      updatedWidgets.push(widget);
    });

    const _widgets = updatedWidgets.map((widget: RestWidget) => {
      return widget.json;
    });
    const metaData = get(filters, ["metaData"], {});
    let form: any = {
      metadata: {
        ...metaData,
        dashboard_time_range_filter: dashboard_time_range_filter
      },
      widgets: _widgets
    };
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
          put(restapiClear(getWidgetConstant(widget.type, "uri"), getWidgetConstant(widget.type, "method"), widget.id)),
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
    yield put(newDashboardUpdate(dashboardId, form));
    // GA event date_global_filter
    emitEvent(
      AnalyticsCategoryType.DATA_GLOBAL_FILTERS,
      DataGlobalFiltersActions.UPDATED,
      action?.filters?.dashboard_time_range_filter
    );
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

export function* dashboardTimeUpdateWidgetsWatcher() {
  yield takeLatest(DASHBOARD_TIME_UPDATE, dashboardTimeUpdateWidgets);
}
