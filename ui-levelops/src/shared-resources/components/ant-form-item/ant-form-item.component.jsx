import React from "react";
import { Form } from "antd";

export const AntFormItemComponent = props => {
  return <Form.Item {...props} />;
};

// AntFormItemComponent.propTypes = {
//     colon: PropTypes.bool,
//     extra: PropTypes.any,
//     hasFeedback: PropTypes.bool,
//     help: PropTypes.any,
//     htmlFor: PropTypes.string,
//     label: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
//     labelCol: PropTypes.object,
//     labelAlign: PropTypes.oneOf("left", "right"),
//     required: PropTypes.bool,
//     validateStatus: PropTypes.string,
//     wrapperCol: PropTypes.object
// };

// AntFormItemComponent.defaultProps = {
//     colon: false,
//     extra: "",
//     hasFeedback: false,
//     help: "",
//     htmlFor: "",
//     label: "",
//     labelCol: PropTypes.object,
//     labelAlign: "right",
//     required: false,
//     validateStatus: "",
//     wrapperCol: PropTypes.object
// };
