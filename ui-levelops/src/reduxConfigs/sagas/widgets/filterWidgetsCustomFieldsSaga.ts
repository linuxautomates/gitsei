import { RestWidget } from "classes/RestDashboards";
import widgetConstants from "dashboard/constants/widgetConstants";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import { forEach, get, isArray, map, unset } from "lodash";
import { all, put, select, takeLatest } from "redux-saga/effects";
import { FILTER_WIDGETS_CUSTOM_FIELDS } from "reduxConfigs/actions/actionTypes";
import { widgetUpdate } from "reduxConfigs/actions/restapi";
import { BaseActionType } from "reduxConfigs/actions/restapi/action.type";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { selectedDashboardFieldsList } from "reduxConfigs/selectors/fields-list.selector";
import { selectedDashboardIntegrations } from "reduxConfigs/selectors/integrationSelector";
import { getWidgetsByDashboardId } from "reduxConfigs/selectors/widgetSelector";

const getFieldListKey = (application: string, allIntegrations: any[]): string => {
  const selectedIntegrations = allIntegrations
    ?.filter(integration => integration.application === application)
    ?.map(integration => integration.id);
  return `${selectedIntegrations.sort().join()}_${application}`;
};

function* filterWidgetsCustomFieldsSaga(action: BaseActionType): any {
  const dashboard = yield select(selectedDashboard);
  const widgets = yield select(getWidgetsByDashboardId, { dashboard_id: dashboard.id });
  const updatedWidgets: Record<string, RestWidget> = {};

  const allFieldsListData = yield select(selectedDashboardFieldsList);
  const integrations = yield select(selectedDashboardIntegrations);
  forEach(widgets, widget => {
    const query = widget.query;
    const customParentKey = "custom_fields";
    const application = get(widgetConstants, [widget.type, "application"]);
    const customFields = query?.[customParentKey];
    const allCustomKeys = Object.keys(customFields ?? {});

    if (allCustomKeys.length) {
      const keysToUnset: string[] = [];
      const fieldListKey = getFieldListKey(application, integrations);
      const fieldList = allFieldsListData[fieldListKey];
      forEach(allCustomKeys, customKey => {
        const value = customFields?.[customKey];
        let isTimeBased = false;
        const itemFromFieldsList = (fieldList || []).find(
          (item: { field_key: string; field_type: string; name: string; field_id: number | string }) => {
            const fieldKey = item?.field_key || `customfield_${item?.field_id?.toString()}`;
            return fieldKey === customKey;
          }
        );

        if (itemFromFieldsList) {
          isTimeBased = CustomTimeBasedTypes.includes(itemFromFieldsList.field_type);
        }
        if (!isTimeBased && typeof value === "object" && !isArray(value)) {
          keysToUnset.push(customKey);
        }
      });
      if (keysToUnset.length) {
        let newQuery = widget.query;
        let updatedWidget = widget;
        forEach(keysToUnset, cKey => unset(newQuery, [customParentKey, cKey]));
        updatedWidget.query = newQuery;
        updatedWidgets[updatedWidget.id] = updatedWidget;
      }
    }
  });

  if (Object.keys(updatedWidgets).length) {
    yield all(
      map(Object.keys(updatedWidgets), wId => {
        const query = updatedWidgets[wId]?.query;
        put(widgetUpdate(wId, { query }));
      })
    );
  }
}

export function* filterWidgetsCustomFieldsSagaWatcher() {
  yield takeLatest(FILTER_WIDGETS_CUSTOM_FIELDS, filterWidgetsCustomFieldsSaga);
}
