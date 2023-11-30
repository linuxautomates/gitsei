import React from "react";
import { Checkbox } from "antd";

export const AntCheckboxGroupComponent = props => {
  return <Checkbox.Group {...props} />;
};

// AntCheckboxComponent.propTypes = {
//     autoFocus: PropTypes.bool,
//     checked: PropTypes.bool,
//     defaultChecked: PropTypes.bool,
//     disabled: PropTypes.bool,
//     indeterminate: PropTypes.bool,
//     onChange: PropTypes.func
// };

// AntCheckboxComponent.defaultProps = {
//     autoFocus: false,
//     checked: false,
//     defaultChecked: false,
//     disabled: false,
//     indeterminate: false,
//     onChange: () => null
// };
