import { RestWidget } from "classes/RestDashboards";
import { cloneDeep, forEach, get } from "lodash";
import { put, takeLatest, select, call } from "redux-saga/effects";
import { COPY_WIDGET_TO_DASHBOARD } from "reduxConfigs/actions/actionTypes";
import { dashboardWidgetAdd, setEntities, setSelectedEntity } from "reduxConfigs/actions/restapi";
import { _dashboardsGetSelector } from "reduxConfigs/selectors/dashboardSelector";
import { getWidget, widgetsSelector } from "reduxConfigs/selectors/widgetSelector";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { OrganizationUnitService, WorkspaceService } from "services/restapi";
import { v1 as uuid } from "uuid";
import { loadDashboardSaga } from "../dashboards/loadDashboardSaga";
import queryString from "query-string";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

function* copyWidgetEffectSaga(action: {
  type: string;
  payload: {
    uri: string;
    dashboard_id: string;
    widget_id: string;
  };
}): any {
  const dashboardId = action.payload.dashboard_id || "";
  const widgetId = action.payload.widget_id || "";
  const uri = action.payload.uri;
  const workspaceService = new WorkspaceService();
  const orgService = new OrganizationUnitService();
  const OU = queryString.parseUrl(window.location.href, { parseFragmentIdentifier: true })?.query?.OU;
  const workspaceId = queryString.parseUrl(window.location.href, { parseFragmentIdentifier: true })
    ?.query?.workspace_id;

  try {
    const selectedWorkspace = yield select(getSelectedWorkspace);
    const data = {
      dashboard_id: dashboardId,
      workspace_id: workspaceId ?? selectedWorkspace?.id
    };

    const response = yield call(workspaceService.categoriesList, data, "workspace_categories");

    const categoriesList = get(response, ["data", "records"], [])?.map((category: any) => category?.id);
    const filters = {
      filter: { ou_category_id: categoriesList, dashboard_id: parseInt(dashboardId) }
    };

    const orgResponse = yield call(orgService.list as any, filters as any);
    const sortedData = get(orgResponse, ["data", "records"], []).sort((OU1: any, OU2: any) => {
      const path1 = OU1?.path?.split("/");
      const path2 = OU2?.path?.split("/");
      return path1?.length - path2?.length;
    });
    const topOUInTree = sortedData?.find((ou: any) => ou?.id === OU) ?? get(sortedData, "0", {});
    let ou_category_id = "";
    if (!topOUInTree.parent_ref_id) {
      ou_category_id = topOUInTree.ou_id;
    } else {
      const filters = {
        filter: {
          ou_id: topOUInTree?.ou_id ?? ""
        }
      };

      const parentListResponse = yield call(orgService.parentList as any, filters as any);
      const data = [...get(parentListResponse, ["data", "records"], []), { ...topOUInTree }].sort(
        (OU1: any, OU2: any) => {
          const path1 = OU1?.path?.split("/");
          const path2 = OU2?.path?.split("/");
          return path1?.length - path2?.length;
        }
      );
      ou_category_id = data?.map((item: any) => item?.ou_id)?.join(",");
    }

    yield call(loadDashboardSaga, { id: dashboardId, OU: topOUInTree?.id });
    const dashboards = yield select(_dashboardsGetSelector);
    const dashboard = get(dashboards, [dashboardId, "data"], {});
    const widget: RestWidget = yield select(getWidget, { widget_id: widgetId });
    let widgets = yield select(widgetsSelector);
    let _widgets = Object.values(widgets);
    const dashboardSpecifcWidgets: any[] = _widgets.filter((w: any) => w.dashboard_id === dashboardId && !w?.hidden);
    const copyWidget = RestWidget.newInstance(dashboard, uuid(), widget.widget_type, widget.name, false, widget.type);
    copyWidget.dashboard_id = dashboardId;
    copyWidget.query = cloneDeep(widget.query);
    copyWidget.metadata = cloneDeep(widget.metadata);
    if (dashboard.metadata.hasOwnProperty("dashboard_time_range") && !dashboard.metadata.dashboard_time_range) {
      copyWidget.metadata = {
        ...copyWidget.metadata,
        dashBoard_time_keys: Object.keys(copyWidget.metadata?.dashBoard_time_keys || []).reduce(
          (acc, key) => ({
            ...acc,
            [key]: {
              use_dashboard_time: dashboard.metadata.dashboard_time_range
            }
          }),
          {}
        )
      };
    }
    copyWidget.order = (dashboardSpecifcWidgets || []).length + 1;
    const widgetChildren: string[] = widget.children || [];
    let newWidgetChildren: string[] = [];
    forEach(_widgets, (widget: any) => {
      if (widgetChildren.includes(widget.id)) {
        const newWidgetChild = cloneDeep(widget);
        newWidgetChild.dashboard_id = dashboardId;
        newWidgetChild.id = uuid();
        newWidgetChildren.push(newWidgetChild.id);
        _widgets.push(newWidgetChild);
      }
    });
    copyWidget.children = newWidgetChildren;
    yield put(dashboardWidgetAdd(dashboardId, copyWidget.json));
    yield put(setEntities(_widgets, "widgets"));
    yield put(
      setSelectedEntity(uri, {
        [dashboardId]: {
          id: copyWidget.id,
          OU: topOUInTree,
          ou_category_id: ou_category_id,
          workspace_id: workspaceId ?? selectedWorkspace?.id
        }
      })
    );
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to copy widget.",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

export function* copyWidgetSagaWatcher() {
  yield takeLatest(COPY_WIDGET_TO_DASHBOARD, copyWidgetEffectSaga);
}
