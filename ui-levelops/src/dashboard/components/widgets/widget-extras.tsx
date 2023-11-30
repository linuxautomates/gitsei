import React, { useCallback, useContext, useMemo, useState } from "react";
import { Button, Dropdown, Icon, Menu, notification } from "antd";
import { RouteComponentProps, withRouter, useParams } from "react-router-dom";
import { useDispatch } from "react-redux";
import { v1 as uuid } from "uuid";

import { RestDashboard, RestWidget } from "classes/RestDashboards";
import { WIDGET_DELETE_WARNING } from "constants/formWarnings";
import {
  dashboardWidgetClone,
  widgetCSVDownloadAction,
  widgetDelete
} from "reduxConfigs/actions/restapi/widgetActions";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { CONFIG_TABLE_ROUTES, getBaseUrl, VELOCITY_CONFIGS_ROUTES } from "constants/routePaths";
import { get } from "lodash";
import {
  ALL_VELOCITY_PROFILE_REPORTS,
  DEPRECATED_NOT_ALLOWED,
  ISSUE_MANAGEMENT_REPORTS
} from "dashboard/constants/applications/names";
import { WebRoutes } from "routes/WebRoutes";
import { AntBadge, AntTooltip, SvgIcon } from "shared-resources/components";
import ConfirmationModalComponent from "shared-resources/components/confirmation-modal/ConfirmationModalComponent";
import WidgetFilterPreviewWrapper from "../widget-filter-preview/WidgetFilterPreviewWrapper";
import { getFiltersCount } from "../widget-filter-preview/helper";
import { isSanitizedValue, sanitizeObject } from "../../../utils/commonUtils";
import widgetConstants from "../../constants/widgetConstants";
import { SelectWidgetToBeCopiedContext } from "dashboard/pages/context";
import { exportableBAReports, jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { getWidgetUri } from "shared-resources/containers/widget-api-wrapper/helper";
import { azureBAReports } from "dashboard/constants/bussiness-alignment-applications/constants";
import { getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { dateRangeFilterValue } from "../dashboard-view-page-secondary-header/helper";
import queryString from "query-string";
import { useLocation } from "react-router-dom";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { ProjectPathProps } from "classes/routeInterface";
import { useDashboardPermissions } from "custom-hooks/HarnessPermissions/useDashboardPermissions";
import { useParentProvider } from "contexts/ParentProvider";
import { removeLastSlash } from "utils/regexUtils";
const { SubMenu } = Menu;

interface WidgetExtrasProps extends RouteComponentProps {
  widgetId: string;
  dashboardId: string;
}

const WidgetExtras: React.FC<WidgetExtrasProps> = props => {
  const { widgetId, dashboardId } = props;
  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const dashboard: RestDashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const ouId = queryParamOU || dashboard?.ou_ids?.[0];
  const isStat = widget.widget_type.includes("stats");
  const tableId = widget.tableId; // id of table if widget chartType is table
  const clickEnabled = widget.isChartClickEnabled; // determines whether chart associated with widget is clickable or not
  const dispatch = useDispatch();
  const [showDeleteModal, setShowDeleteModal] = useState<boolean>(false);
  const { setWidgetToBeCopied } = useContext(SelectWidgetToBeCopiedContext);
  const isDeprecatedAndNotAllowed = get(widgetConstants, [widget?.type, DEPRECATED_NOT_ALLOWED], false);
  const projectParams = useParams<ProjectPathProps>();
  const onOk = useCallback(() => {
    dispatch(widgetDelete(widgetId));
    setShowDeleteModal(false);
  }, []);
  const {
    utils: { getLocationPathName }
  } = useParentProvider();
  const onCancel = useCallback(() => {
    setShowDeleteModal(false);
  }, []);

  const handleClone = () => {
    const cloneId = uuid();
    dispatch(dashboardWidgetClone(dashboardId, widgetId, cloneId));
    props.history.push({
      pathname: WebRoutes.dashboard.widgets.details(projectParams, dashboardId, cloneId),
      search: location?.search || undefined
    });
  };

  // action to handle open report in case of table widgets
  const handleOpenReport = () => {
    let _url = `${getBaseUrl()}${CONFIG_TABLE_ROUTES.EDIT}?id=${tableId}`;
    const query = widget.query || {};
    if (!!ouId) {
      query["ou_id"] = ouId;
    }
    if (Object.keys(query).length) {
      const dashboardTimeKeysObject = get(widget, ["metadata", "dashBoard_time_keys"], {});
      if (Object.keys(dashboardTimeKeysObject)?.length > 0) {
        Object.keys(dashboardTimeKeysObject).forEach(key => {
          if (!!dashboardTimeKeysObject[key]?.use_dashboard_time) {
            const dashboardTimeRangeValue = get(dashboard, ["metadata", "dashboard_time_range_filter"], undefined);
            query[key] = dateRangeFilterValue(dashboardTimeRangeValue);
          }
        });
      }
      const tableFilters = JSON.stringify(query);
      _url = `${_url}&filters=${tableFilters}`;
    }
    window.open(`${removeLastSlash(getLocationPathName?.())}${_url}`);
  };

  const renderTitle = useMemo(() => {
    const uri = get(widgetConstants, [widget?.type, "supported_filters", "uri"], "default_uri");
    const count = getFiltersCount(sanitizeObject(widget?.query || {}), widget.type, uri) || 0;
    return (
      <div className="filters-container">
        <SvgIcon icon={"widgetFiltersIcon"} />
        <span>Filters</span>
        <AntBadge count={count || 0} className="filters-count" />
      </div>
    );
  }, [widget]);

  const getDynamicURI = useCallback(() => {
    return getWidgetUri(widget?.type, getWidgetConstant(widget?.type, "uri"), widget?.query, widget?.metadata);
  }, [widget]);

  const getMethod = useMemo(() => getWidgetConstant(widget?.type, "method"), [widget?.type]);

  const handleMenuClick = useCallback(
    payload => {
      switch (payload.key) {
        case "edit":
          dispatch(restapiClear(getDynamicURI(), getWidgetConstant(widget?.type, "method"), widgetId));
          props.history.push({
            pathname: WebRoutes.dashboard.widgets.details(projectParams, dashboardId, widgetId),
            search: location?.search
          });
          break;
        case "clone":
          handleClone();
          break;
        case "delete":
          setShowDeleteModal(true);
          break;
        case "open_report":
          handleOpenReport();
          break;
        case "edit_velocity_profile":
          handleEditClick();
          break;
        case "copyTo":
          setWidgetToBeCopied(widgetId);
          break;
        case "edit_effort_investment_profile":
          handleEditProfileClick();
          break;
        case "export":
          //need to fix it later
          dispatch(widgetCSVDownloadAction(dashboardId, widgetId, { OU: queryParamOU }));
          break;
      }
    },
    [handleClone, getDynamicURI, queryParamOU, location?.search]
  );

  const onIconClick = useCallback(e => {
    e.preventDefault();
  }, []);

  const velocityConfigId = useMemo(() => {
    if (widget) {
      return get(widget.query, ["velocity_config_id"], "default");
    }
    return "";
  }, [widget]);

  const handleEditClick = useCallback(() => {
    if (velocityConfigId) {
      props.history.push(`${getBaseUrl()}${VELOCITY_CONFIGS_ROUTES.EDIT}?configId=${velocityConfigId}`);
    }
  }, [widget]);

  const isBAReportType = [
    ...Object.values(jiraBAReportTypes),
    ...azureBAReports.filter(value => value !== ISSUE_MANAGEMENT_REPORTS.AZURE_PROGRAM_PROGRESS_REPORT)
  ].includes(widget?._type);

  const handleEditProfileClick = useCallback(() => {
    const scheme = get(widget, ["query", "ticket_categorization_scheme"], undefined);
    if (isSanitizedValue(scheme)) props.history.push(WebRoutes.ticket_categorization.scheme.edit(scheme, "basic_info"));
    else notification.error({ message: "You don't have Effort Investment Profile Selected" });
  }, [widget]);

  const renderMenu = useMemo(() => {
    return (
      <Menu onClick={handleMenuClick} className="widget-actions-menu">
        {/* {isStat && widget.description && (
          <Menu.Item key={"info"}>
            <AntTooltip title={widget.description} trigger="hover">
              <Icon type="info-circle" theme="outlined" onClick={onIconClick} />
              Description
            </AntTooltip>
          </Menu.Item>
        )} */}
        <Menu.Item key={"edit"}>
          <Icon type="edit" onClick={onIconClick} />
          Edit
        </Menu.Item>
        <Menu.Item disabled={isDeprecatedAndNotAllowed} key={"clone"}>
          <Icon type="copy" onClick={onIconClick} />
          Clone
        </Menu.Item>
        <Menu.Item disabled={isDeprecatedAndNotAllowed} key="copyTo">
          <Icon type="export" onClick={onIconClick} />
          Copy to
        </Menu.Item>
        <Menu.Item key={"delete"}>
          <Icon type="delete" onClick={onIconClick} />
          Delete
        </Menu.Item>
        {exportableBAReports.includes(widget?.type) && (
          <Menu.Item key={"export"}>
            <Icon type="export" onClick={onIconClick} />
            Export
          </Menu.Item>
        )}
        {ALL_VELOCITY_PROFILE_REPORTS.includes(widget.type) && (
          <Menu.Item key={"edit_velocity_profile"}>Edit workflow profile</Menu.Item>
        )}
        {!!tableId && !!clickEnabled && (
          <Menu.Item key={"open_report"}>
            <Icon type="folder-open" onClick={onIconClick} />
            Open Report
          </Menu.Item>
        )}
        {isStat && (
          <SubMenu key="filters" className="filters-sub-menu" popupOffset={[32, 0]} title={renderTitle}>
            <Menu.ItemGroup
              key={"filters-popup"}
              onClick={({ domEvent }) => domEvent.preventDefault()}
              className="filters-item">
              <WidgetFilterPreviewWrapper widgetId={widgetId} dashboardId={dashboardId} />
            </Menu.ItemGroup>
          </SubMenu>
        )}
        {isBAReportType && (
          <Menu.Item key={"edit_effort_investment_profile"}>
            <Icon type="edit" onClick={onIconClick} />
            Edit Investment Profile
          </Menu.Item>
        )}
      </Menu>
    );
  }, [handleClone, widget]);

  const renderDeleteModal = useMemo(() => {
    return (
      <ConfirmationModalComponent
        text={WIDGET_DELETE_WARNING}
        onCancel={onCancel}
        onOk={onOk}
        visiblity={showDeleteModal}
      />
    );
  }, [showDeleteModal]);

  const oldAccess = getRBACPermission(PermeableMetrics.ADMIN_WIDGET_EXTRAS);
  const permissions = useDashboardPermissions();
  const hasEditAccess = window.isStandaloneApp ? oldAccess : permissions[1];
  return (
    <>
      {hasEditAccess && (
        <Dropdown overlay={renderMenu} trigger={["click"]} placement="bottomRight">
          <Button className="widget-extras">
            <Icon type={"more"} />
          </Button>
        </Dropdown>
      )}
      {renderDeleteModal}
    </>
  );
};

export default withRouter(React.memo(WidgetExtras));
