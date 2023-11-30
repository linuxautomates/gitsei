import React from "react";
import { Icon } from "antd";

import { SelectRestapiHelperWrapper as SelectRestapi } from "shared-resources/helpers/select-restapi/select-restapi.helper.wrapper";
import { AvatarWithTextComponent as AvatarWithText } from "../avatar-with-text/avatar-with-text.component";
import "./select-with-avatar.styles.scss";

interface SelectWithAvatarComponentProps {
  btnLabel: string;
  onChange: (data: any) => void;
  value: any;
  mode: string;
  uri: string;
  fetchData: () => void;
  searchField: string;
  labelinValue?: boolean;
  allowClear?: boolean;
  dropdownRender: (node: any, props: any) => any;
}

export class SelectWithAvatarComponent extends React.Component<SelectWithAvatarComponentProps> {
  constructor(props: SelectWithAvatarComponentProps) {
    super(props);
    this.state = {
      show_dropdown: false
    };
  }

  handleSelectBlur = () => {
    this.setState({
      show_dropdown: false
    });
  };

  handleRemove = (item: { key: string; label: string }) => {
    this.props.onChange(this.value.filter((v: { key: string; label: string }) => v.key !== item.key));
  };

  handleAdd = () => {
    this.setState({
      show_dropdown: true
    });
  };

  get value() {
    return this.props.value;
  }

  get assignees() {
    if (!this.value) {
      return null;
    }
    return this.value.map((v: any) => (
      <div className="selected-option">
        <AvatarWithText className="content" key={v.key} avatarText={v.label} text={v.label} />
        <Icon
          className="close-icon"
          type="close"
          onClick={() => {
            this.handleRemove(v);
          }}
        />
      </div>
    ));
  }

  get showDropdown() {
    // @ts-ignore
    return this.state.show_dropdown;
  }

  get select() {
    if (!this.showDropdown) {
      return (
        <div onClick={this.handleAdd} className="add-btn">
          <Icon className="add-icon" type="plus-circle" />
          {this.props.btnLabel}
        </div>
      );
    }
    return (
      <SelectRestapi
        {...this.props}
        className="dropdown"
        value={this.value}
        autoFocus
        onBlur={this.handleSelectBlur}
        onChange={this.props.onChange}
      />
    );
  }

  render() {
    return (
      <div className="select-with-avatar">
        {this.assignees}
        {this.select}
      </div>
    );
  }
}
