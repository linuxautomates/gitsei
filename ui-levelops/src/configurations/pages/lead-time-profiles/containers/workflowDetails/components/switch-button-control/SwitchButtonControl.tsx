import React, { useMemo } from "react";
import { AntButton, SvgIcon } from "shared-resources/components";
import "./SwitchButtonControl.scss";

interface SwitchButtonOptionProps {
  key: string;
  label: string;
  icon?: string;
}

interface SwitchButtonControlProps {
  value: string,
  onChange: (value: string) => void;
  options: Array<SwitchButtonOptionProps>
}

const SwitchButtonControl: React.FC<SwitchButtonControlProps> = ({
  value,
  onChange,
  options
}) => {
  const setType = (newValue: string) => useMemo(() => value === newValue ? "primary" : "default", [value]);
  const onClickHandler = (value: string) => () => onChange(value);

  return (
    <div className="switch-button-control">
      {options?.map(({key, label, icon}: SwitchButtonOptionProps) => (
        <AntButton
          type={setType(key)}
          onClick={onClickHandler(key)}
          shape="round"
          className={value === key ? "btn-selected" : "btn"}
        >
          {icon && <SvgIcon icon={icon} className="btn-icon" />} {label}
        </AntButton>
      ))}
    </div>
  );
}

export default SwitchButtonControl;