import React from "react";
import { Badge } from "antd";

export const AntBadgeComponent = props => {
  return <Badge {...props} />;
};

// AntBadgeComponent.propTypes = {
//     color: PropTypes.string,
//     count: PropTypes.node,
//     dot: PropTypes.bool,
//     overflowCount: PropTypes.number,
//     showZero: PropTypes.bool,
//     status: PropTypes.oneOf(["success", "processing", "default", "error", "warning"]),
//     text: PropTypes.string,
//     title: PropTypes.string
// };

// AntBadgeComponent.defaultProps = {
//     color: "",
//     count: "",
//     dot: false,
//     overflowCount: 99,
//     showZero: false,
//     status: "",
//     text: "",
//     title: "",// Default will be count
// };
