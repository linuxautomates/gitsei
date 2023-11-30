import React, { useMemo } from "react";
import { Dropdown, Menu } from "antd";
import "./DashboardHeader.scss";
import AntIconComponent from "shared-resources/components/ant-icon/ant-icon.component";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { useIsDashboardReadonly } from "custom-hooks/HarnessPermissions/useIsDashboardReadonly";

interface DashboardDatePickerProps {
  dashboard: Record<any, any>;
  setDefault: (dashboard: Record<any, any>) => void;
  showClone?: (id: Record<any, any>) => void;
  deleteDashboard?: (id: Record<any, any>) => void;
  isDefaultDisabled?: boolean;
}

const DashboardSearchPopoverActions: React.FC<DashboardDatePickerProps> = ({
  isDefaultDisabled,
  setDefault,
  deleteDashboard = () => {},
  showClone = () => {},
  dashboard
}) => {
  const memoizedTrigger: any = useMemo(() => ["click"], []);
  const menuOverlay = useMemo(
    () => (
      <Menu>
        <Menu.Item
          disabled={isDefaultDisabled}
          onClick={() => {
            setDefault(dashboard);
          }}>
          Set as default
        </Menu.Item>
        <Menu.Item
          onClick={() => {
            showClone(dashboard?.dashboard_id);
          }}>
          Clone
        </Menu.Item>
        <Menu.Item
          onClick={() => {
            deleteDashboard(dashboard?.dashboard_id);
          }}>
          Delete
        </Menu.Item>
      </Menu>
    ),
    [dashboard, isDefaultDisabled]
  );
  const listActions = useMemo(
    () => (
      <Dropdown overlay={menuOverlay} trigger={memoizedTrigger}>
        <AntIconComponent type="setting" />
      </Dropdown>
    ),
    [dashboard, isDefaultDisabled]
  );

  const oldAccess = getRBACPermission(PermeableMetrics.DASHBOARD_LIST_DROPDOWN_ACTIONS);
  const isDashboardReadonly = useIsDashboardReadonly();
  const hasAccess = window.isStandaloneApp ? oldAccess : !isDashboardReadonly;

  return <>{hasAccess && listActions} </>;
};
export default React.memo(DashboardSearchPopoverActions);
