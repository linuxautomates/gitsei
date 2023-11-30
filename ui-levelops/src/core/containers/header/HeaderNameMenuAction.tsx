import { Dropdown, Icon, Menu } from "antd";
import { map } from "lodash";
import React, { useMemo } from "react";

const HeaderNameMenuAction: React.FC<{
  menuItems: any[];
  handleClick: (key: string) => void;
}> = ({ menuItems, handleClick }) => {
  const renderMenu = useMemo(() => {
    if ((menuItems || []).length === 0) return null;
    return (
      <Menu onClick={e => handleClick(e.key)}>
        {map(menuItems, item => (
          <Menu.Item disabled={item.disabled || false} key={item.id}>
            {item.label || ""}
          </Menu.Item>
        ))}
      </Menu>
    );
  }, [menuItems]);

  return (
    <Dropdown overlay={renderMenu} trigger={["click"]}>
      <Icon type="more" />
    </Dropdown>
  );
};

export default React.memo(HeaderNameMenuAction);
