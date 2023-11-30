import React, { MouseEventHandler } from "react";
import { AntParagraph } from "shared-resources/components";
import "./trigger.style.scss";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

interface TriggerProps {
  icon: JSX.Element;
  description: string;
  onClick: MouseEventHandler<HTMLDivElement>;
}

export const Trigger = (props: TriggerProps) => {
  const [hasCreateAccess] = useConfigScreenPermissions();
  return (
    <div className="trigger" onClick={hasCreateAccess ? props.onClick : undefined}>
      <div className="trigger__left">{props.icon}</div>
      <div className="trigger__right">
        <AntParagraph title={props.description} ellipsis={{ rows: 2 }}>
          {props.description}
        </AntParagraph>
      </div>
    </div>
  );
};
