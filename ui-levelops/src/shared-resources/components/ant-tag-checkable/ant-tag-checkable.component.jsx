import React from "react";
import { Tag } from "antd";

const { CheckableTag } = Tag;

export class AntTagCheckableComponent extends React.PureComponent {
  state = { checked: true };

  handleChange = checked => {
    this.setState({ checked });
  };
  render() {
    const props = this.props;
    return <CheckableTag {...props} checked={this.state.checked} onChange={this.handleChange} />;
  }
}
