import React from "react";
import * as PropTypes from "prop-types";

import "./no-data.style.scss";

export class NoDataComponent extends React.PureComponent {
  render() {
    const { className } = this.props;

    return <div className={className}>{this.props.message}</div>;
  }
}

NoDataComponent.propTypes = {
  message: PropTypes.string,
  className: PropTypes.string
};

NoDataComponent.defaultProps = {
  message: "No data is available",
  className: "no-data"
};
