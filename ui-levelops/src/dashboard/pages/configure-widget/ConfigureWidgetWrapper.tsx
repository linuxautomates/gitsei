import { Empty, notification } from "antd";
import MultiTimeWidgetConfiguration from "configurable-dashboard/components/configure-widget/widget-configuration/MultiTimeSeriesWidgetConfiguration";
import { LEVELOPS_MULTITIME_SERIES_REPORT } from "dashboard/constants/applications/multiTimeSeries.application";
import React, { useEffect, useMemo, useState } from "react";
import { RouteComponentProps, withRouter, useParams } from "react-router-dom";
import { RestWidget } from "../../../classes/RestDashboards";
import MultiWidgetConfiguration from "../../../configurable-dashboard/components/configure-widget/widget-configuration/MultiWidgetConfiguration";
import SingleWidgetConfiguration from "../../../configurable-dashboard/components/configure-widget/widget-configuration/SingleWidgetConfiguration";
import { dashboardWidgetChildrenSelector, getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { WebRoutes } from "../../../routes/WebRoutes";
import { WidgetType } from "../../helpers/helper";
import { WidgetPayloadContext } from "../context";
import { widgetValidationHelper } from "dashboard/helpers/widgetValidation.helper";
import { getWidgetConstant } from "../../constants/widgetConstants";
import { WIDGET_VALIDATION_FUNCTION, WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE } from "../../constants/filter-name.mapping";
import {
  VELOCITY_CONFIG_LIST_ID,
  velocityConfigsRestListSelector
} from "reduxConfigs/selectors/velocityConfigs.selector";
import { JIRA_MANAGEMENT_TICKET_REPORT, LEAD_TIME_REPORTS } from "dashboard/constants/applications/names";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { ProjectPathProps } from "classes/routeInterface";
interface ConfigureWidgetPageProps extends RouteComponentProps {
  widgetId: string;
  dashboardId: string;
  setHeader: (
    dashboardId: string,
    dashboardName: string,
    widgetId: string,
    widgetName: string,
    validWidget: boolean,
    errorMessage: string
  ) => void;
}

const ConfigureWidgetWrapper: React.FC<ConfigureWidgetPageProps> = ({
  dashboardId,
  widgetId,
  history,
  setHeader,
  location
}) => {
  const dashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });
  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const projectParams = useParams<ProjectPathProps>();

  const childWidgets: RestWidget[] = useParamSelector(dashboardWidgetChildrenSelector, {
    dashboard_id: dashboardId,
    widget_id: widgetId
  });

  const velocityConfigsListState = useParamSelector(velocityConfigsRestListSelector, {
    id: VELOCITY_CONFIG_LIST_ID
  });
  // ENTITLEMENT FOR JIRA RELESE PROFILE
  const velocityJiraReleaseProfile = useHasEntitlements(
    Entitlement.VELOCITY_JIRA_RELEASE_PROFILE,
    EntitlementCheckType.AND
  );

  const [payload, setPayload] = useState({});
  const [errorMessage, seterrorMessage] = useState<string>("");

  const type = widget?.type;

  const redirectToDashboard = () =>
    history.replace(WebRoutes.dashboard.details(projectParams, dashboardId, location?.search));

  const widgetType = widget?.widget_type || "";
  const isComposite = widgetType === WidgetType.COMPOSITE_GRAPH && type !== LEVELOPS_MULTITIME_SERIES_REPORT;
  const isMultiTimeSeries = widgetType === WidgetType.COMPOSITE_GRAPH && type === LEVELOPS_MULTITIME_SERIES_REPORT;

  const checkforJiraProfileOnly = useMemo(() => {
    if (
      ((velocityJiraReleaseProfile && type === LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT) ||
        type === JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT) &&
      widget?.query?.velocity_config_id
    ) {
      let checkForJiraProfile = velocityConfigsListState.filter(
        (profile: any) => widget.query.velocity_config_id === profile.id && profile.jira_only
      );
      if (checkForJiraProfile?.length <= 0) {
        return true;
      }
    }
    return false;
  }, [type, velocityJiraReleaseProfile, widget, velocityConfigsListState]);

  const validWidget = useMemo(() => {
    seterrorMessage("");
    if (isMultiTimeSeries) return childWidgets.length > 1 && !childWidgets.find((widget: RestWidget) => !widget.type);
    if (isComposite) return childWidgets.length > 0 && !childWidgets.find((widget: RestWidget) => !widget.type);
    const widgetValidationFunction = getWidgetConstant(type || "", WIDGET_VALIDATION_FUNCTION);
    if (widgetValidationFunction) {
      return widgetValidationHelper(widget);
    }
    const widgetValidationWithErrorFunction = getWidgetConstant(type || "", WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE);
    if (widgetValidationWithErrorFunction) {
      const validateData =  widgetValidationWithErrorFunction(widget);
      if(validateData){
        seterrorMessage(validateData?.errorMessage || "")
        return validateData?.saveWidget;
      }
    }
    if (checkforJiraProfileOnly) {
      seterrorMessage("Please select a JIRA only based workflow profile under the settings tab of this report.");
      return false;
    }
    return !!type;
  }, [isComposite, childWidgets, widget, type, checkforJiraProfileOnly]);

  const dashboardName = dashboard?.name;
  const widgetName = widget?.name;

  const widgetPayloadContext = useMemo(() => ({ payload, setPayload }), [payload]);

  useEffect(() => {
    if (!widget) {
      redirectToDashboard();
    }
    if (dashboard && widget) {
      setHeader(dashboardId, dashboardName, widgetId, widgetName, validWidget, errorMessage);
    }
  }, [dashboardId, dashboardName, widgetId, widgetName, validWidget, errorMessage]);

  useEffect(() => {
    if (!widget) {
      notification.error({ message: "Widget not found" });
      redirectToDashboard();
    }
  }, []);

  const content = useMemo(() => {
    if (isComposite) return <MultiWidgetConfiguration widgetId={widgetId} dashboardId={dashboardId} />;
    if (isMultiTimeSeries) return <MultiTimeWidgetConfiguration widgetId={widgetId} dashboardId={dashboardId} />;
    return <SingleWidgetConfiguration widgetId={widgetId} dashboardId={dashboardId} />;
  }, [isComposite, isMultiTimeSeries]);

  if (!widget) {
    return <Empty />;
  }

  return <WidgetPayloadContext.Provider value={widgetPayloadContext}>{content}</WidgetPayloadContext.Provider>;
};

export default React.memo(withRouter(ConfigureWidgetWrapper));
