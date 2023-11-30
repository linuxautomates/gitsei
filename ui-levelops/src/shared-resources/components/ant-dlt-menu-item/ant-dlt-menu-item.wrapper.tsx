import React from "react";
import { Menu, Popconfirm } from "antd";
import { PopconfirmProps } from "antd/lib/popconfirm";

export class DeleteMenuItemWrapper extends React.Component<PopconfirmProps> {
  render() {
    const { onConfirm, title, children, ...passedProps } = this.props;

    return (
      <Popconfirm title={title} onConfirm={onConfirm}>
        <Menu.Item {...passedProps}>{children}</Menu.Item>
      </Popconfirm>
    );
  }
}
