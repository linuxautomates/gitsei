import { Menu } from "antd";
import React from "react";
import AntIconComponent from "../../../../../shared-resources/components/ant-icon/ant-icon.component";

interface DashboardConfigureActionMenuProps {
  handleMenuClick: (param: any) => void;
  menuData: {
    key: string;
    value: string;
    icon_type?: string;
    menu_class?: string;
  }[];
  disableDropdownOption?: (item: any) => boolean;
}

const ActionButtonMenu: React.FC<DashboardConfigureActionMenuProps> = ({
  handleMenuClick,
  menuData,
  disableDropdownOption = (item: any) => false
}) => {
  return (
    <Menu onClick={handleMenuClick}>
      {menuData &&
        menuData.map(menu => {
          return (
            <Menu.Item key={menu.key} className={menu?.menu_class} disabled={disableDropdownOption(menu.key)}>
              {menu.icon_type && <AntIconComponent type={menu.icon_type} />}
              {menu.value}
            </Menu.Item>
          );
        })}
    </Menu>
  );
};

export default React.memo(ActionButtonMenu);
