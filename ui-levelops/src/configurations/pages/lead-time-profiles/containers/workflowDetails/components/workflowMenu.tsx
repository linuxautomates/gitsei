import { Icon, Menu, Tooltip } from "antd";
import React, { useMemo } from "react";
import { WORKFLOW_PROFILE_SUB_MENU_SECTION, WORKFLOW_PROFILE_MENU } from "./constant";

interface WorkflowProfileMenuProp {
  onChange: (selectedMenu: any) => void;
  exclamationFlag: boolean;
  LTFCAndMTTRSupport?: boolean;
  tabComponent: WORKFLOW_PROFILE_MENU;
}

const WorkflowProfileMenu: React.FC<WorkflowProfileMenuProp> = ({
  onChange,
  exclamationFlag,
  LTFCAndMTTRSupport,
  tabComponent
}) => {
  return (
    <Menu onClick={onChange} className="workflow-profile-container-menu" defaultSelectedKeys={[tabComponent]}>
      <Menu.Item className="workflow-profile-container-menu-item" key={WORKFLOW_PROFILE_MENU.CONFIGURATION}>
        CONFIGURATION
      </Menu.Item>
      {WORKFLOW_PROFILE_SUB_MENU_SECTION.map((section: any) => (
        <Menu.Item className="workflow-profile-container-menu-item sub-menu" key={section}>
          {section.toUpperCase()}
        </Menu.Item>
      ))}
      <Menu.Item className="workflow-profile-container-menu-item-icon" key={WORKFLOW_PROFILE_MENU.ASSOCIATIONS}>
        ASSOCIATION
        {exclamationFlag && (
          <Tooltip
            placement="right"
            title={"Some of the collections associated have been impacted due to a change in the integration"}>
            <Icon type="warning" className="icon-style" theme="twoTone" twoToneColor="rgb(223, 165, 42)" />
          </Tooltip>
        )}
      </Menu.Item>
    </Menu>
  );
};

export default WorkflowProfileMenu;
