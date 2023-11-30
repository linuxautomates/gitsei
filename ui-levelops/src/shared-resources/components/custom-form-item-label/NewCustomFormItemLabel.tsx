import { Checkbox, Popconfirm, Select, Switch, Tooltip } from "antd";
import classNames from "classnames";
import React, { useMemo } from "react";
import { capitalizeFirstLetter } from "utils/stringUtils";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import { AntTextComponent as AntText } from "../ant-text/ant-text.component";
import { AntAlertComponent as AntAlert } from "../ant-alert/ant-alert.component";
import { default as AntIcon } from "../ant-icon/ant-icon.component";
import { AntPopoverComponent as AntPopover } from "../ant-popover/ant-popover.component";
import {
  showInfoProps,
  switchWithDropdownProps,
  WithCheckBoxesProps,
  withDeleteProps,
  WithSwitchProps
} from "./CustomFormItemLabel";
import "./NewCustomFormItemLabel.scss";
import { NOT_ALLOWED_FILTER_LAST_SPRINT } from "dashboard/graph-filters/components/Constants";

const { Option } = Select;

interface CustomFormItemLabelProps {
  label: string;
  withSwitch?: WithSwitchProps;
  withCheckBoxes?: WithCheckBoxesProps;
  switchWithDropdown?: switchWithDropdownProps;
  withInfo?: showInfoProps;
  withDelete?: withDeleteProps;
  required?: boolean;
  lastSprint?: boolean;
}

const NewCustomFormItemLabel: React.FC<CustomFormItemLabelProps> = (props: CustomFormItemLabelProps) => {
  const deleteIconStyle = useMemo(
    () => ({
      fontSize: "15px",
      margin: "0 0.5rem"
    }),
    []
  );

  const showActions = props.switchWithDropdown?.showSwitchWithDropdown || props.withSwitch?.showSwitch;

  const hasOnly1Action =
    (props.switchWithDropdown?.showSwitchWithDropdown && !props.withSwitch?.showSwitch) ||
    (props.withSwitch?.showSwitch && !props.switchWithDropdown?.showSwitchWithDropdown);

  return (
    <div className={"new-custom-form-item-label"}>
      <div className={"w-100 flex justify-space-between align-center"}>
        <span>
          {props.label}
          {props.required && <span style={{ color: "red", fontSize: "18px" }}>*</span>}
        </span>
        <div className={"self-align-end"}>
          {props.withInfo?.showInfo && (
            <AntPopover
              overlayClassName="custom-form-item-label__popover"
              placement="topRight"
              content={<AntAlert message={props.withInfo?.description} type="info" showIcon />}
              arrowPointAtCenter>
              <AntIcon type="info-circle" className="custom-form-item-label__info-icon" />
            </AntPopover>
          )}
          {props.withDelete?.showDelete && (
            <Popconfirm
              title={"Are you sure you want to delete this filter?"}
              okText={"Yes"}
              onConfirm={() => props.withDelete?.onDelete?.(props?.withDelete?.key || "")}>
              <AntIcon style={deleteIconStyle} type={"delete"} />
            </Popconfirm>
          )}
        </div>
      </div>
      {showActions && (
        <div
          className={classNames(
            "new-custom-label-actions direction-row-reverse",
            {
              "justify-space-between": !hasOnly1Action
            },
            { "justify-end": hasOnly1Action && !props.withSwitch?.showSwitch }
          )}>
          {/** Added below button as a fix for PROP-1334 to add an empty eventlistner so that the last
           *   event which gets bubbled is empty and does not affect the UI
           */}
          <AntButton onClick={() => {}} className="placeholder-button"></AntButton>
          {props.withSwitch?.showSwitch && (
            <div className={"flex direction-column align-center"}>
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
          {props.switchWithDropdown?.showSwitchWithDropdown && (
            <Tooltip title={props.lastSprint ? NOT_ALLOWED_FILTER_LAST_SPRINT : ""}>
              <div
                className={"flex mr-10"}
                style={{ width: "20rem", justifyContent: "start", height: "100%", alignItems: "center" }}>
                <div className={"flex"} style={{ alignItems: "center" }}>
                  <Checkbox
                    className="mr-5"
                    disabled={props.lastSprint ? true : props.switchWithDropdown?.checkboxProps?.disabled}
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
            </Tooltip>
          )}
        </div>
      )}
    </div>
  );
};

export default React.memo(NewCustomFormItemLabel);
