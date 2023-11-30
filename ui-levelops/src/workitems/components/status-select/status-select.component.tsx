import React, { useState } from "react";
import { Menu, Dropdown, Button } from "antd";
import indexOf from "lodash/indexOf";
import { AntIcon, AntTag } from "shared-resources/components";

export const StatusSelectComponent = (props: any) => {
  let initialStatus = props.status || "Done";
  const statuses = ["To Do", "In Progress", "In Review", "Done"];
  let initialKey = indexOf(statuses, initialStatus);
  // const [status, setStatus] = useState(initialStatus);
  const [currentKey, setKey] = useState(initialKey);

  const handleMenuClick = (e: any) => {
    let newStatus = statuses[e.key];
    //update button text
    setKey(e.key);
    //send newStatus to backend:
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
