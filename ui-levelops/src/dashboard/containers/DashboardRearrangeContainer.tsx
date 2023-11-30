import React, { useContext, useEffect, useState, useMemo } from "react";
import { notification, Row } from "antd";
import { RouteComponentProps, useHistory, useLocation, useParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import queryString from "query-string";
import "./DashboardRearrangeContainer.scss";
import Loader from "components/Loader/Loader";
import {
  dashboardsGetSelector,
  dashboardsUpdateSelector,
  getDashboard,
  selectedDashboard
} from "reduxConfigs/selectors/dashboardSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { RestDashboard, RestWidget } from "../../classes/RestDashboards";
import { resetWidgetsOrder, widgetDelete } from "reduxConfigs/actions/restapi/widgetActions";
import { useHeader } from "../../custom-hooks/useHeader";
import { newDashboardUpdate, restapiClear } from "reduxConfigs/actions/restapi";
import { generateDashboardLayoutPageBreadcrumbs } from "../../utils/dashboardUtils";
import WidgetRearrangeGridContainer from "./widget-rearrange-grid.container";
import { WebRoutes } from "../../routes/WebRoutes";
import { DashboardWidgetResolverContext } from "../pages/context";
import { getWidget, getWidgetsByDashboardId } from "reduxConfigs/selectors/widgetSelector";
import { getDashboardsPage } from "constants/routePaths";
import { ProjectPathProps } from "classes/routeInterface";

interface DashboardModifyProps {
  widgetId?: string;
}

const RESET_LAYOUT_ACTION_KEY = "action_reset_layout";
const BACK_ACTION_KEY = "action_back";
const PUBLISH_ACTION_KEY = "action_publish";
const CANCEL_ACTION_KEY = "action_cancel";

const ACTION_BUTTONS = {
  [CANCEL_ACTION_KEY]: {
    type: "ghost",
    label: "Cancel",
    hasClicked: false
  },
  [PUBLISH_ACTION_KEY]: {
    type: "primary",
    label: "Publish Widget",
    hasClicked: false,
    showProgress: false,
    progressLabel: "Publishing..."
  }
};

const DashboardRearrangeContainer: React.FC<DashboardModifyProps> = ({ widgetId = "" }) => {
  const dispatch = useDispatch();
  const [loading, setLoading] = useState(true);
  const [publishing, setPublishing] = useState<boolean>(false);
  const location = useLocation();
  const history = useHistory();
  const projectParams = useParams<ProjectPathProps>();

  const { pathname } = location;
  const parsedQuery = queryString.parse(location.search);
  const copyDashboardId = parsedQuery?.copy_from;
  const previousOUData = parsedQuery?.prev_OU;
  const workspaceId = parsedQuery?.workspace_id;
  const { dashboardId } = useContext(DashboardWidgetResolverContext);
  const { setupHeader, changeButtonState, onActionButtonClick } = useHeader(pathname);

  const { loading: dashboardLoading } = useParamSelector(dashboardsGetSelector, { dashboard_id: dashboardId });
  const { loading: dashboardUpdating } = useParamSelector(dashboardsUpdateSelector, { dashboard_id: dashboardId });
  const dashboard: RestDashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });
  const selecteddashboard = useSelector(selectedDashboard);
  const widgets = useParamSelector(getWidgetsByDashboardId, { dashboard_id: dashboardId });
  const search = `?OU=${
    parsedQuery?.prev_OU !== undefined ? parsedQuery?.prev_OU : parsedQuery?.OU
  }&workspace_id=${workspaceId}`;

  const redirectToDashboard = () =>
    history.push({ pathname: WebRoutes.dashboard.details(projectParams, dashboardId), search: search });

  useEffect(() => {
    if (!dashboardLoading && loading) {
      setLoading(false);
    }
  }, [dashboardLoading]);

  useEffect(() => {
    if (dashboardUpdating) {
      return;
    }
    if (publishing) {
      redirectToDashboard();
    }
  }, [dashboardUpdating, changeButtonState]);

  useEffect(() => {
    if (loading) {
      return;
    }

    if (!widgetId) {
      // delete ACTION_BUTTONS[DELETE_ACTION_KEY];
      ACTION_BUTTONS[PUBLISH_ACTION_KEY].label = "Save Layout";
    }

    const pageSetting = {
      title: "Configure Widget",
      action_buttons: ACTION_BUTTONS,
      bread_crumbs: generateDashboardLayoutPageBreadcrumbs(projectParams, dashboard, null, location?.search),
      showDivider: true,
      hideButtonPadding: true,
      withBackButton: true,
      goBack: true
    };
    setupHeader(pageSetting);
  }, [loading, dashboardLoading]);

  useEffect(() => {
    onActionButtonClick((action: string) => {
      switch (action) {
        case CANCEL_ACTION_KEY:
          const draftWidget: RestWidget = widgets.find((widget: RestWidget) => widget.draft === true);
          if (draftWidget) {
            const id: any = copyDashboardId || dashboardId;
            dispatch(widgetDelete(draftWidget.id));
            dispatch(restapiClear("dashboards", "get", id));
          }
          const redirectDashboardId = copyDashboardId || dashboardId;
          if (copyDashboardId) {
            const newSearch = `?OU=${previousOUData}&workspace_id=${workspaceId}`;
            history.push({
              pathname: WebRoutes.dashboard.details(projectParams, dashboardId),
              search: newSearch
            });
          } else {
            history.push({
              pathname: `${getDashboardsPage(projectParams)}/${redirectDashboardId}`,
              search: search
            });
          }
          return {
            hasClicked: false
          };
        case PUBLISH_ACTION_KEY:
          setPublishing(true);
          const _dashboard = { ...dashboard?.json, query: { ...(selecteddashboard?.query || {}) } };
          dispatch(newDashboardUpdate(dashboardId, _dashboard, false, true));
          return { hasClicked: false, showProgress: true, disabled: true };
        case RESET_LAYOUT_ACTION_KEY:
          dispatch(resetWidgetsOrder(dashboardId));
          const id: any = copyDashboardId || dashboardId;
          dispatch(restapiClear("dashboards", "get", id));
          return { hasClicked: false };
        default:
          return null;
      }
    });
  }, [onActionButtonClick]);

  const memoizedGutter: any = useMemo(() => [10, 10], []);

  if (loading) {
    return <Loader />;
  }

  return (
    <Row className="h-100 flex justify-center widget-rearrange-grid-container" gutter={memoizedGutter}>
      <WidgetRearrangeGridContainer dashboardId={dashboardId} />
    </Row>
  );
};

export default DashboardRearrangeContainer;
