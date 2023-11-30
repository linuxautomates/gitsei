import React from "react";
import { Button } from "antd";

const ButtonGroup = Button.Group;

export const AntButtonGroupComponent = props => {
  return <ButtonGroup {...props} />;
};

// AntButtonGroupComponent.propTypes = {
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

// AntButtonGroupComponent.defaultProps = {
//     disabled: false,
//     ghost: false,
//     href: "",
//     htmlType: "button",
//     icon: "",
//     loading: false,//boolean | { delay: number }
//     shape: "",
//     size: "",
//     target: "",
//     type: "",
//     onClick: () => null,
//     block: false
// };
