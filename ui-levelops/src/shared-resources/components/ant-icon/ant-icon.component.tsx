import React from "react";
import { Icon } from "antd";
import { IconProps } from "antd/lib/icon";

const AntIconComponent = (props: IconProps) => {
  return (
    // the icon is inside a <span> otherwise tooltips and such won't show
    <span>
      <Icon {...props} />
    </span>
  );
};

export default React.memo(AntIconComponent);
