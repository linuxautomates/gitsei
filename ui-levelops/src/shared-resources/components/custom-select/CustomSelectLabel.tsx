import { Popover } from "antd";
import classNames from "classnames";
import React from "react";
import { AntButton, SvgIcon } from "..";
import "./CustomSelect.scss";

interface CustomSelectLabelProps {
  label: string;
  required?: boolean;
  renderConditions?: React.ReactNode;
}

class CustomSelectLabel extends React.Component<CustomSelectLabelProps, {}> {
  constructor(props: CustomSelectLabelProps) {
    super(props);
  }

  render() {
    return (
      <div className={"custom-select-label"}>
        <span className={classNames({ "custom-select-label_required": this.props.required })}>
          {this.props.label.toUpperCase()}
        </span>
        {this.props.renderConditions && (
          <Popover placement={"bottomLeft"} content={this.props.renderConditions} trigger="click">
            <AntButton className={"widget-extras"}>
              <SvgIcon icon={"widgetFiltersIcon"} />
            </AntButton>
          </Popover>
        )}
      </div>
    );
  }
}
export default CustomSelectLabel;
