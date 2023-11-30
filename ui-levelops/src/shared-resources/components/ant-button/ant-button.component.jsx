import React from "react";
import { Button } from "antd";

export const AntButtonComponent = props => {
  return <Button {...props} />;
};

// AntButtonComponent.propTypes = {
//     disabled: PropTypes.bool,
//     ghost: PropTypes.bool,
//     href: PropTypes.string,
//     htmlType: PropTypes.string,
//     icon: PropTypes.string,
//     loading: PropTypes.bool,//boolean | { delay: number }
//     shape: PropTypes.oneOf(["circle", "round", "default"]),
//     size: PropTypes.oneOf(["small", "large", "default"]),
//     target: PropTypes.string,
//     type: PropTypes.oneOf(["primary", "ghost", "dashed", "danger", "link", "default"]),
//     onClick: PropTypes.func,
//     block: PropTypes.bool
// };

// AntButtonComponent.defaultProps = {
//     disabled: false,
//     ghost: false,
//     htmlType: "button",
//     icon: "",
//     loading: false,//boolean | { delay: number }
//     target: "",
//     type: "",
//     onClick: () => null,
//     block: false
// };
