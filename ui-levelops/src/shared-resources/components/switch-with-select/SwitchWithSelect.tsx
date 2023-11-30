import React from "react";
import { AntCheckboxComponent as AntCheckbox } from "../ant-checkbox/ant-checkbox.component";
import { AntTextComponent as AntText } from "../ant-text/ant-text.component";
import { AntSelectComponent as AntSelect } from "../ant-select/ant-select.component";
import { AntRadioComponent as AntRadio } from "../ant-radio/ant-radio.component";
import { AntRadioGroupComponent as AntRadioGroup } from "../ant-radio-group/ant-radio-group.component";
import { capitalizeFirstLetter } from "utils/stringUtils";

interface SwitchWithSelectProps {
  selectType: "select" | "radio";
  data_testId: string;
  checkboxProps: any;
  selectProps: any;
}

const SwitchWithSelect: React.FC<SwitchWithSelectProps> = props => {
  const { selectType, data_testId } = props;

  return (
    <div data-testid={data_testId} className={"flex mr-10"} style={{ justifyContent: "start", alignItems: "center" }}>
      <div className={"flex"} style={{ alignItems: "center" }}>
        <AntCheckbox
          data-testid="custom-form-item-label-switchWithDropdown-checkbox"
          className="mr-5"
          disabled={props.checkboxProps?.disabled}
          checked={props.checkboxProps?.value}
          onChange={(e: any) => props.checkboxProps?.onCheckboxChange?.(e.target.checked)}
        />
        <AntText style={{ margin: 0 }} className="action-text-select">
          {capitalizeFirstLetter(props.checkboxProps?.text || "") + ":"}
          &nbsp;&nbsp;
        </AntText>
      </div>
      {selectType === "radio" ? (
        <AntRadioGroup
          data-testid="switchWithSelect-ant-radio-group"
          disabled={props.selectProps?.disabled}
          value={props.selectProps?.value}
          onChange={(e: any) => props.selectProps?.onSelectChange?.(e.target.value)}>
          {(props.selectProps?.options || []).map((option: any) => {
            return (
              <AntRadio key={option.value} value={option.value}>
                {capitalizeFirstLetter(option.label)}
              </AntRadio>
            );
          })}
        </AntRadioGroup>
      ) : (
        <AntSelect
          data-testid="custom-form-item-label-switchWithDropdown-checkbox"
          disabled={props.selectProps?.disabled}
          style={{ width: "7rem", color: "#5f5f5f", paddingTop: "2px" }}
          value={props.selectProps?.value}
          onChange={(e: any) => props.selectProps?.onSelectChange?.(e.target.value)}
          options={props.selectProps?.options}
        />
      )}
    </div>
  );
};

SwitchWithSelect.defaultProps = {
  selectType: "select"
};

export default SwitchWithSelect;
