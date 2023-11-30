import React from "react";
import { AntButtonGroup } from "shared-resources/components";
import { Button } from "antd";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Button Group",
  component: AntButtonGroup
};

export const ButtonGroup = () => (
  <AntButtonGroup>
    <Button type="primary">First</Button>
    <Button type="primary">Second</Button>
    <Button type="primary">Third</Button>
  </AntButtonGroup>
);
