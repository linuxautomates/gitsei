import React from "react";
import { AntCheckbox } from "shared-resources/components";
import { DEFAULT_LABEL } from "./constants";

interface DrilldownViewMissingCheckboxProps {
  title?: string;
  onClick: void;
  value: string;
}

const DrilldownViewMissingCheckbox: React.FC<DrilldownViewMissingCheckboxProps> = props => {
  return (
    <AntCheckbox checked={props.value} onChange={props.onClick}>
      {props?.title ?? DEFAULT_LABEL}
    </AntCheckbox>
  );
};

export default DrilldownViewMissingCheckbox;
