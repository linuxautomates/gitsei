import React from "react";
import { AntBadgeComponent as AntBadge } from "../ant-badge/ant-badge.component";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import { Icon } from "antd";
import "../../../assets/sass/light-bootstrap-dashboard-pro-react.scss";

export default {
  title: "Ant Badge",
  component: AntBadge
};

export const Badge = () => (
  <>
    <AntBadge count={5}>
      <AntButton type="primary">Text</AntButton>
    </AntBadge>
    <br />
    <br />
    <AntBadge count={0} showZero="true">
      <AntButton type="primary">Text</AntButton>
    </AntBadge>
  </>
);

export const OverflowBadge = () => (
  <>
    <AntBadge count={99} overflowCount={20}></AntBadge>
  </>
);

export const BadgeStates = () => (
  <>
    <AntBadge status="success" text="Success" />
    <br />
    <AntBadge status="default" text="Default" />
    <br />
    <AntBadge status="processing" text="Processing" />
    <br />
    <AntBadge status="warning" text="Warning" />
    <br />
    <AntBadge status="error" text="Error" />
  </>
);

export const IconBadge = () => (
  <>
    <AntBadge dot>
      <Icon type="notification" />
    </AntBadge>
    <br />
    <AntBadge count={0} dot>
      <Icon type="notification" />
    </AntBadge>
    <br />
    <AntBadge dot>
      <a href="#">Link something</a>
    </AntBadge>
  </>
);
const colors = [
  "pink",
  "red",
  "yellow",
  "orange",
  "cyan",
  "green",
  "blue",
  "purple",
  "geekblue",
  "magenta",
  "volcano",
  "gold",
  "lime",
  "#ff0",
  "#ccc",
  "#90a"
];
export const CustomColorBadge = () => (
  <>
    {colors.map(color => (
      <div key={color}>
        <AntBadge color={color} text={color} />
      </div>
    ))}
  </>
);
