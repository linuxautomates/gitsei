import React from "react";
import { Alert } from "antd";

export const AntAlertComponent = props => {
  return <Alert {...props} />;
};

// AntAlertComponent.propTypes = {
//     afterClose: PropTypes.func,
//     banner: PropTypes.bool,
//     closable: PropTypes.bool,
//     closeText: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
//     description: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
//     icon: PropTypes.node,
//     message: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
//     showIcon: PropTypes.bool,
//     type: PropTypes.string,
//     onClose: PropTypes.func
// };

// AntAlertComponent.defaultProps = {
//     afterClose: () => null,
//     banner: false,
//     closable: false,
//     closeText: "",
//     description: "",
//     icon: "",
//     message: "",
//     showIcon: true,
//     type: "",
//     onClose: () => null
// };
