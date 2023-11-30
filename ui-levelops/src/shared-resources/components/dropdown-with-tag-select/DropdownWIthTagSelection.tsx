import React, { useState, useEffect, useCallback } from "react";
import { Icon } from "antd";
import { dropdownWithTagSelectConfig } from "dashboard/dashboard-types/Dashboard.types";
import { AntIcon } from "..";
import SelectRestapi from "shared-resources/helpers/select-restapi/select-restapi";
import { optionType } from "dashboard/dashboard-types/common-types";
import "./dropdownWithTagSelection.styles.scss";
export interface DropdownWithTagSelectionComponentProps {
  label: string;
  showDropdown: boolean;
  dropdownValue: any;
  dropdownConfig: dropdownWithTagSelectConfig;
  onFilerValueChange: (selectedValue: optionType) => void;
}

const DropdownWithTagSelectionComponent: React.FC<DropdownWithTagSelectionComponentProps> = ({
  label,
  showDropdown,
  dropdownValue,
  dropdownConfig,
  onFilerValueChange
}) => {
  const [value, setValue] = useState<string>("");

  useEffect(() => {
    if (!value) {
      setValue(dropdownValue);
    }
  }, []);

  useEffect(() => {
    if (!showDropdown && !!value) {
      setValue("");
    }
  }, [showDropdown]);

  const handleOUChange = useCallback(
    (selectedValue: optionType) => {
      setValue(selectedValue?.value);
      onFilerValueChange(selectedValue);
    },
    [onFilerValueChange]
  );

  return (
    <div className="dropdown-with-tag-selection" style={{ display: `${showDropdown ? "" : "none"}` }}>
      <div className="label-container">
        <Icon type="cluster" />
        <span className="label">{label}</span>
      </div>
      <div className="dropdown-container">
        <SelectRestapi
          suffixIcon={
            <AntIcon
              type="down"
              style={{ fontSize: "1rem", color: `${!!value ? "#404040" : "#8C8C8C"}`, fill: "solid" }}
            />
          }
          id={"dropdown-tag-select"}
          uuid={dropdownConfig.uuid || "0"}
          placeholder={dropdownConfig.placeholder || ""}
          uri={dropdownConfig.uri}
          value={value}
          showSearch={dropdownConfig?.showSearch ?? false}
          searchField={dropdownConfig.searchField || ""}
          specialKey={dropdownConfig?.specialKey || "id"}
          showSpinnerWhenLoading={dropdownConfig.showSpinnerWhenLoading}
          onChange={handleOUChange}
          filterOptionMethod={dropdownConfig?.filterOptionMethod}
          transformOptions={dropdownConfig.transformOptions}
          createOption={false}
          mode={"default"}
          useOnSelect={dropdownConfig.useOnSelect}
          dropdownClassName={dropdownConfig.dropdownClassName}
          allowClear={dropdownConfig.allowClear}
          className={`${dropdownConfig.className} ${!!value ? "" : "show-place-holder"}`}
        />
      </div>
    </div>
  );
};

export default DropdownWithTagSelectionComponent;
