import React from "react";
import * as PropTypes from "prop-types";
import { Icon } from "antd";
import { AntTable } from "shared-resources/components";

const columns = [
  {
    title: "Status",
    dataIndex: "success",
    key: "success",
    render: text => <Icon type={text ? "check-circle" : "close-circle"} style={{ color: text ? "green" : "red" }} />
  },
  {
    title: "Check",
    dataIndex: "name",
    key: "name"
  },
  {
    title: "Exception",
    dataIndex: "exception",
    key: "exception"
  }
];

export class PreFlightCheckComponent extends React.PureComponent {
  render() {
    if (!this.props.checks || this.props.checks.length === 0) {
      return "";
    }
    return <AntTable bordered={true} columns={columns} dataSource={this.props.checks} pagination={false} />;
  }
}

PreFlightCheckComponent.propTypes = {
  checks: PropTypes.array.isRequired
};

PreFlightCheckComponent.defaultProps = {
  checks: []
};
