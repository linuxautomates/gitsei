import { Checkbox, Popconfirm, Select, Switch } from "antd";
import classNames from "classnames";
import React, { useMemo } from "react";
import { capitalizeFirstLetter } from "utils/stringUtils";
import { AntTextComponent as AntText } from "../ant-text/ant-text.component";
import { AntAlertComponent as AntAlert } from "../ant-alert/ant-alert.component";
import { default as AntIcon } from "../ant-icon/ant-icon.component";
import { AntPopoverComponent as AntPopover } from "../ant-popover/ant-popover.component";
import "./CustomFormItemLable.scss";

const { Option } = Select;

export type WithCheckBoxesProps = {
  showCheckboxes: boolean;
  checkboxes?: Array<{ label: string; key: string; value: boolean }>;
  onCheckBoxChange?: (key: string, value: boolean) => void;
};

export type WithSwitchProps = {
  showSwitch: boolean;
  showSwitchText?: boolean;
  switchText?: string;
  switchValue?: boolean;
  disabled?: boolean;
  onSwitchValueChange?: (value: boolean) => void;
};

export type switchWithDropdownProps = {
  showSwitchWithDropdown: boolean;
  checkboxProps?: { value: boolean; disabled: boolean; text: string };
  selectProps?: {
    options: Array<{ label: string; value: string }>;
    onSelectChange: (key: string | undefined) => void;
    value: any;
    disabled: boolean;
  };
};

export type showInfoProps = {
  showInfo: boolean;
  description: string;
};

export type withDeleteProps = {
  showDelete: boolean;
  key?: string;
  onDelete?: (key: string, parentKey?: string) => void;
  parentKey?: string;
};

interface CustomFormItemLabelProps {
  label: string;
  withSwitch?: WithSwitchProps;
  withCheckBoxes?: WithCheckBoxesProps;
  switchWithDropdown?: switchWithDropdownProps;
  withInfo?: showInfoProps;
  withDelete?: withDeleteProps;
  required?: boolean;
}

const CustomFormItemLabel: React.FC<CustomFormItemLabelProps> = (props: CustomFormItemLabelProps) => {
  const newLabelStyle = useMemo(
    () => ({
      height: "3.7rem",
      flexDirection: "column",
      alignItems: "flex-start"
    }),
    []
  );

  const newActionStyle = useMemo(
    () => ({
      width: "100%",
      height: "100%",
      justifyContent: "space-between",
      alignItems: "flex-end"
    }),
    []
  );

  const deleteIconStyle = useMemo(
    () => ({
      fontSize: "15px",
      margin: "0 0.5rem"
    }),
    []
  );

  return (
    <div
      data-testid="custom-form-item-label"
      className={"custom-form-item-label"}
      style={props.switchWithDropdown?.showSwitchWithDropdown ? (newLabelStyle as any) : {}}>
      <div className={classNames(props.withDelete?.showDelete && "w-100 flex justify-space-between align-center")}>
        <span>
          {props.label}
          {props.required && <span style={{ color: "red", fontSize: "18px" }}>*</span>}
        </span>
        {props.withDelete?.showDelete && (
          <Popconfirm
            title={"Are you sure you want to delete this filter?"}
            okText={"Yes"}
            onConfirm={() => props.withDelete?.onDelete?.(props?.withDelete?.key || "")}>
            <AntIcon style={deleteIconStyle} type={"delete"} />
          </Popconfirm>
        )}
      </div>
      <div
        className="custom-label-actions"
        style={props.switchWithDropdown?.showSwitchWithDropdown ? (newActionStyle as any) : {}}>
        {props.withInfo?.showInfo && (
          <AntPopover
            overlayClassName="custom-form-item-label__popover"
            placement="topRight"
            content={<AntAlert message={props.withInfo?.description} type="info" showIcon />}
            arrowPointAtCenter>
            <AntIcon type="info-circle" className="custom-form-item-label__info-icon" />
          </AntPopover>
        )}
        {props.switchWithDropdown?.showSwitchWithDropdown && (
          <div
            data-testid="custom-form-item-label-switchWithDropdown"
            className={"flex mr-10"}
            style={{ width: "20rem", justifyContent: "start", height: "100%", alignItems: "center" }}>
            <div className={"flex"} style={{ alignItems: "center" }}>
              <Checkbox
                data-testid="custom-form-item-label-switchWithDropdown-checkbox"
                className="mr-5"
                disabled={props.switchWithDropdown?.checkboxProps?.disabled}
                checked={props.switchWithDropdown?.checkboxProps?.value}
                onChange={e =>
                  props.switchWithDropdown?.selectProps?.onSelectChange?.(
                    e.target.checked ? props.switchWithDropdown?.selectProps?.options[0].value : undefined
                  )
                }
              />
              <AntText style={{ margin: 0 }} className="action-text-select">
                {capitalizeFirstLetter(props.switchWithDropdown?.checkboxProps?.text || "") + " :"}
              </AntText>
            </div>
            <Select
              data-testid="custom-form-item-label-switchWithDropdown-checkbox"
              disabled={props.switchWithDropdown?.selectProps?.disabled}
              style={{ width: "7rem", color: "#5f5f5f", paddingTop: "2px" }}
              value={props.switchWithDropdown?.selectProps?.value}
              onChange={(e: string | undefined) => props.switchWithDropdown?.selectProps?.onSelectChange?.(e)}>
              {props.switchWithDropdown?.selectProps?.options.map(option => (
                <Option key={option.value} value={option.value} style={{ fontWeight: 500 }}>
                  {capitalizeFirstLetter(option.label)}
                </Option>
              ))}
            </Select>
          </div>
        )}
        {props.withSwitch?.showSwitch && (
          <div className={"flex direction-column"} style={{ alignItems: "center" }}>
            <Switch
              data-testid="custom-form-item-label-withSwitch-switch"
              title={props.withSwitch?.switchText || "Exclude"}
              onChange={checked => props.withSwitch?.onSwitchValueChange?.(checked)}
              checked={props.withSwitch?.switchValue}
              disabled={props.withSwitch?.disabled}
              size={"small"}
            />
            {props.withSwitch?.showSwitchText && (
              <AntText className="action-text">{props.withSwitch?.switchText || "Exclude"}</AntText>
            )}
          </div>
        )}
        {props.withCheckBoxes?.showCheckboxes &&
          props.withCheckBoxes?.checkboxes?.map((checkbox: any, index: number) => {
            return (
              <div key={`checkbox-${index}`} className={"flex direction-column mx-10"} style={{ alignItems: "center" }}>
                <Checkbox
                  key={checkbox.key}
                  checked={checkbox.value}
                  onChange={(e: any) => props.withCheckBoxes?.onCheckBoxChange?.(checkbox.key, e.target.checked)}
                />
                <AntText className="action-text">{checkbox.label}</AntText>
              </div>
            );
          })}
      </div>
    </div>
  );
};

export default CustomFormItemLabel;
