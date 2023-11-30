import React, { useCallback, useMemo } from "react";
import { Col, Dropdown, Menu, Popconfirm, Row } from "antd";

import { DASHBOARD_ROUTES, getBaseUrl } from "../../../../constants/routePaths";
import Loader from "../../../../components/Loader/Loader";
import AntIconComponent from "../../../../shared-resources/components/ant-icon/ant-icon.component";
import { RBAC } from "constants/localStorageKeys";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { useParams } from "react-router-dom";
import { ProjectPathProps } from "classes/routeInterface";
import { useIsDashboardReadonly } from "custom-hooks/HarnessPermissions/useIsDashboardReadonly";
import { useDashboardPermissions } from "custom-hooks/HarnessPermissions/useDashboardPermissions";

interface DashboardRowProps {
  history: any;
  dashboard: any;
  actionDashId: string | undefined;
  onDefaultClick: (dashboardId: string) => void;
  onDeleteClick: (dashboardId: string) => void;
  onCloneClick: (dashboardId: string) => void;
}

const DashboardRow: React.FC<DashboardRowProps> = ({
  history,
  dashboard,
  actionDashId,
  onDefaultClick,
  onDeleteClick,
  onCloneClick
}) => {
  const memoizedTrigger: any = useMemo(() => ["click"], []);
  const memoizedIconStyle = useMemo(() => ({ width: "100%", height: "100%" }), []);
  const projectParams = useParams<ProjectPathProps>();

  const handleDefault = useCallback(() => onDefaultClick(dashboard.id), [dashboard.id]);
  const handleClone = useCallback(() => onCloneClick(dashboard.id), [dashboard.id]);
  const handleDelete = useCallback(() => onDeleteClick(dashboard.id), [dashboard.id]);
  const access = useDashboardPermissions();
  const dashboardAccess = window.isStandaloneApp ? [true, true, true] : access;

  const menuOverlay = useMemo(
    () => (
      <Menu>
        <Menu.Item disabled={dashboard.default} onClick={handleDefault}>
          Set as default
        </Menu.Item>
        {dashboardAccess[0] && <Menu.Item onClick={handleClone}>Clone</Menu.Item>}
        {dashboardAccess[2] && (
          <Menu.Item>
            <Popconfirm
              key={`row-action-${dashboard.id}`}
              title={"Do you want to delete this item?"}
              onConfirm={handleDelete}
              okText={"Yes"}
              cancelText={"No"}>
              Delete
            </Popconfirm>
          </Menu.Item>
        )}
      </Menu>
    ),
    [dashboard, onDefaultClick, onCloneClick, onDeleteClick, access]
  );

  const listActions = useMemo(
    () => (
      <Dropdown overlay={menuOverlay} trigger={memoizedTrigger}>
        <AntIconComponent style={memoizedIconStyle} type="setting" />
      </Dropdown>
    ),
    [dashboard, onDefaultClick, onCloneClick, onDeleteClick]
  );

  const handleDashboardClick = useCallback(
    () => history.push(`${getBaseUrl(projectParams)}${DASHBOARD_ROUTES.LIST}/${dashboard.id}`),
    [dashboard.id]
  );
  const memoizedColStyle: any = useMemo(
    () => ({
      padding: "0.5rem 0",
      overflow: "hidden",
      maxHeight: "38px",
      whiteSpace: "nowrap",
      textOverflow: "ellipsis"
    }),
    []
  );

  const rbac = localStorage.getItem(RBAC);
  const memorizedClassname = useMemo(() => {
    return rbac === "PUBLIC_DASHBOARD" && !dashboard.public
      ? "non-public-search-dash-row ant-row-middle"
      : "search-dash-row ant-row-middle";
  }, [rbac, dashboard]);

  const hasHarnessEditAccess = useIsDashboardReadonly();
  const hasEditAccess = window.isStandaloneApp
    ? getRBACPermission(PermeableMetrics.DASHBOARD_LIST_DROPDOWN_ACTIONS)
    : !hasHarnessEditAccess;

  return (
    <Row className={memorizedClassname}>
      <Col style={memoizedColStyle} onClick={handleDashboardClick} span={22}>
        {dashboard.name}
      </Col>
      {dashboard.id !== actionDashId && hasHarnessEditAccess && (
        <Col className="action-options" span={2}>
          {listActions}
        </Col>
      )}
      {dashboard.id === actionDashId && (
        <Col span={2}>
          <Loader />
        </Col>
      )}
    </Row>
  );
};

export default React.memo(DashboardRow);
