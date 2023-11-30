import { Dropdown, Menu } from "antd";
import React, { useMemo } from "react";
import { AntCheckbox, AntCol, AntRow } from "shared-resources/components";
import AntIconComponent from "shared-resources/components/ant-icon/ant-icon.component";
import TooltipWithTruncatedTextComponent from "shared-resources/components/tooltip-with-truncated-text/TooltipWithTruncatedTextComponent";
import { truncateAndEllipsis } from "utils/stringUtils";

interface OUDashboardsRowComponentProps {
  dashboard: any;
  selected: boolean;
  onDefaultClick: (dashboardsId: string) => void;
  handleChangeSelectedDashboards: (dashboardsId: string, selected: boolean) => void;
}

const OUDashboardsRowComponent: React.FC<OUDashboardsRowComponentProps> = (props: OUDashboardsRowComponentProps) => {
  const { dashboard, selected, handleChangeSelectedDashboards, onDefaultClick } = props;
  const menuOverlay = useMemo(
    () => (
      <Menu>
        <Menu.Item disabled={dashboard.default} onClick={() => onDefaultClick(dashboard?.id)}>
          Set as default
        </Menu.Item>
      </Menu>
    ),
    [dashboard, onDefaultClick]
  );

  const listActions = useMemo(
    () => (
      <Dropdown overlay={menuOverlay} trigger={["click"]}>
        <AntIconComponent type="setting" />
      </Dropdown>
    ),
    [dashboard, onDefaultClick]
  );
  return (
    <AntRow className="ou-dashboard-row" id={dashboard?.id}>
      <AntCol className="dashboard-select-checkbox" span={22}>
        <AntCheckbox
          onChange={(e: any) => handleChangeSelectedDashboards(dashboard?.id, e.target.checked)}
          checked={selected}>
          <TooltipWithTruncatedTextComponent title={dashboard?.name} allowedTextLength={20} />
        </AntCheckbox>
      </AntCol>
      <AntCol className="action-options" span={2}>
        {listActions}
      </AntCol>
    </AntRow>
  );
};

export default OUDashboardsRowComponent;
