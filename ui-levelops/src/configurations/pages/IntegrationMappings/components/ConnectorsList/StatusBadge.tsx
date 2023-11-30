import React from "react";
import { Text } from "@harness/uicore";
import { FontVariation } from "@harness/design-system";
import "./StatusBadge.scss";
import { color, backgroundColor, badgeIcon, iconProps } from "./StatusBadge.utils";

interface StatusBadgeProps {
  status: "healthy" | "failed" | "warning" | "unknown";
}

const StatusBadge = ({ status }: StatusBadgeProps) => {
  return (
    <Text
      font={{ variation: FontVariation.UPPERCASED, weight: "bold" }}
      color={color[status]}
      background={backgroundColor[status]}
      icon={badgeIcon[status]}
      padding={{ top: "xsmall", bottom: "xsmall", left: "small", right: "small" }}
      iconProps={iconProps[status]}
      className={"statusBadge"}>
      {status}
    </Text>
  );
};

export default StatusBadge;
