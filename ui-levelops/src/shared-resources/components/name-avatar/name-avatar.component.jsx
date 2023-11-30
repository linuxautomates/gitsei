import React from "react";
import * as PropTypes from "prop-types";
import { Avatar, Tooltip } from "antd";

const alphaVal = s => s.toLowerCase().charCodeAt(0) - 97;

export const NameAvatarComponent = props => {
  if (!props.name) {
    return null;
  }

  const avatar = (
    <Avatar
      key={props.name}
      size="small"
      className={`background-color-${(alphaVal(props.name) % 5) + 1} ${props.showTooltip ? "f-12" : ""}`}>
      {(props.name?.substring(0, 2) || "").toUpperCase()}
    </Avatar>
  );

  if (!props.showTooltip) {
    return avatar;
  }

  return (
    <Tooltip title={props.name} placement={"bottom"}>
      {avatar}
    </Tooltip>
  );
};

NameAvatarComponent.propTypes = {
  showTooltip: PropTypes.bool,
  name: PropTypes.string
};
NameAvatarComponent.defaultProps = {
  showTooltip: true
};
