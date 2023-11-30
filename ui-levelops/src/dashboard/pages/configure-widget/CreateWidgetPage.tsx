import React, { useCallback, useContext, useEffect } from "react";
import { RouteComponentProps, useHistory, useLocation, useParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { notification } from "antd";

import { RestWidget } from "../../../classes/RestDashboards";
import { useHeader } from "../../../custom-hooks/useHeader";
import { generateConfigureWidgetPageBreadcrumbs } from "../../../utils/dashboardUtils";
import ConfigureWidgetWrapper from "./ConfigureWidgetWrapper";
import { widgetDelete } from "reduxConfigs/actions/restapi/widgetActions";
import { WebRoutes } from "../../../routes/WebRoutes";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { DashboardWidgetResolverContext } from "../context";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { resetWidgetLibraryState } from "reduxConfigs/actions/widgetLibraryActions";
import { getRequiredField } from "reduxConfigs/selectors/requiredFieldSelector";
import { DashboardPathProps, ProjectPathProps } from "classes/routeInterface";

interface CreateWidgetPageProps {}

const NEXT_ACTION_KEY = "action_next";
const DELETE_ACTION_KEY = "action_delete";

const ACTION_BUTTONS = {
  [DELETE_ACTION_KEY]: {
    type: "ghost",
    label: "Cancel",
    hasClicked: false
  },
  [NEXT_ACTION_KEY]: {
    type: "primary",
    label: "Next: Place Widget",
    hasClicked: false,
    disabled: false,
    tooltip: ""
  }
};

const CreateWidgetPage: React.FC<CreateWidgetPageProps> = () => {
  const { dashboardId } = useContext(DashboardWidgetResolverContext);
  const dashboardParams = useParams<DashboardPathProps>();
  const location = useLocation();
  const history = useHistory();
  const projectParams = useParams<ProjectPathProps>();
  const { widgetId = "" } = dashboardParams;

  const { setupHeader, onActionButtonClick } = useHeader(location.pathname);

  const dispatch = useDispatch();

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });

  const requiredField = useSelector(getRequiredField);

  const handleSetHeader = useCallback(
    (
      dashboardId: string,
      dashboardName: string,
      widgetId: string,
      widgetName: string,
      validWidget: boolean,
      errorMessage: string
    ) => {
      ACTION_BUTTONS[NEXT_ACTION_KEY].disabled = !validWidget;
      ACTION_BUTTONS[NEXT_ACTION_KEY].tooltip = errorMessage ? errorMessage : "";
      const pageSetting = {
        title: "Configure Widget",
        action_buttons: ACTION_BUTTONS,
        bread_crumbs: generateConfigureWidgetPageBreadcrumbs(
          projectParams,
          dashboardId,
          dashboardName,
          widgetId,
          widgetName,
          location?.search
        ),
        showDivider: true,
        withBackButton: true,
        goBack: true
      };
      setupHeader(pageSetting);
    },
    []
  );

  useEffect(() => {
    return () => {
      dispatch(resetWidgetLibraryState());
      if (history.action === "POP") {
        dispatch(widgetDelete(widgetId));
      }
    };
  }, []);

  useEffect(() => {
    onActionButtonClick((action: string) => {
      switch (action) {
        case DELETE_ACTION_KEY:
          dispatch(widgetDelete(widgetId));
          return {
            hasClicked: false,
            disabled: true,
            showProgress: true
          };
        case NEXT_ACTION_KEY:
          if (requiredField?.is_required_error_field) {
            notification.error({ message: requiredField.required_field_msg });
            return {
              hasClicked: false
            };
          } else {
            history.push({
              pathname: WebRoutes.dashboard.widgets.widgetsRearrange(projectParams, dashboardId),
              search: location?.search || undefined
            });
            return {
              hasClicked: false
            };
          }

        default:
          return null;
      }
    });
  }, [onActionButtonClick]);

  const redirectToDashboard = () =>
    history.replace({
      pathname: WebRoutes.dashboard.details(projectParams, dashboardId),
      search: location?.search || undefined
    });
  const redirectToWidgetWizard = () =>
    history.replace({
      pathname: WebRoutes.dashboard.widgets.widgetsExplorer(projectParams, dashboardId),
      search: location?.search || undefined
    });

  useEffect(() => {
    if (!widget) {
      redirectToDashboard();
    }
    if (widget?.hasError) {
      notification.error({ message: widget.errorMessage });
    }
    if (widget?.deleted) {
      redirectToWidgetWizard();
    }
  }, [widget]);

  return <ConfigureWidgetWrapper widgetId={widgetId} dashboardId={dashboardId} setHeader={handleSetHeader} />;
};

export default CreateWidgetPage;
