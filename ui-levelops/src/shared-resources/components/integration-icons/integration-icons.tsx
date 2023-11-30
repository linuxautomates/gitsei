import React from "react";
import { Icon } from "@harness/uicore";
import { ICON_PATH } from "./icon-path.config";
import { getIntegrationUrlMap } from "constants/integrations";
import { get } from "lodash";

const handleSize = (size?: string): number => {
  switch (size) {
    case "small":
      return 20;
    case "large":
      return 30;
    case "medium":
      return 24;
    case "extra-large":
      return 56;
    default:
      return 24;
  }
};

interface IIntegrationIconComponents {
  key?: string;
  type: string;
  size?: string;
  style?: React.CSSProperties;
  className?: string;
}

export const IntegrationIconComponents = (props: IIntegrationIconComponents) => {
  let check_for_exact_icon_path = get(getIntegrationUrlMap(), [props.type, "check_for_exact_icon_path"], false);

  const commonProps = {
    ...props,
    style: {
      display: "block",
      width: `${handleSize(props.size)}px`,
      height: `${handleSize(props.size)}px`,
      ...props.style
    }
  };

  return ICON_PATH.filter(icon =>
    check_for_exact_icon_path ? props.type === icon.temp_props.type : props.type.includes(icon.temp_props.type)
  ).map((icon, index) => {
    if (icon.icon_name) {
      return <Icon key={index} name={icon.icon_name} {...commonProps} size={handleSize(props.size)} />;
    }

    return (
      <span key={index} {...commonProps}>
        {icon.icon_path}
      </span>
    );
  });
};
