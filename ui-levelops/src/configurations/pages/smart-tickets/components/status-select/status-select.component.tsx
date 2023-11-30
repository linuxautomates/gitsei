import React, { useState } from "react";
import { Menu, Dropdown, Button } from "antd";
import indexOf from "lodash/indexOf";
import { AntIcon, AntTag } from "shared-resources/components";

export const StatusSelectComponent = (props: any) => {
  const initialStatus = props.status || "Done";
  const statuses = ["To Do", "In Progress", "In Review", "Done"];
  const initialKey = indexOf(statuses, initialStatus);
  const [currentKey, setKey] = useState(initialKey);

  const handleMenuClick = (e: any) => {
    setKey(e.key);
  };

  const menu = (
    <Menu onClick={handleMenuClick} selectedKeys={[currentKey.toString()]}>
      {statuses.map((status, i) => (
        <Menu.Item key={i}>
          <AntTag>{status}</AntTag>
        </Menu.Item>
      ))}
    </Menu>
  );
  return (
    <Dropdown overlay={menu} placement="bottomRight" trigger={["click"]}>
      <Button>
        <span style={{ marginRight: "5px" }}>{statuses[currentKey]}</span>
        <AntIcon type="down" />
      </Button>
    </Dropdown>
  );
};
