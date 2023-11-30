import React from "react";
import { Input, InputNumber } from "antd";

const { TextArea, Search } = Input;
const InputGroup = Input.Group;

export const AntInputComponent = props => {
  const renderInput = () => {
    if (props.type === "search") {
      return <Search {...props} />;
    }
    if (props.type === "textarea") {
      return <TextArea autoSize {...props} />;
    }
    if (props.type === "password") {
      return <Input.Password {...props} />;
    }
    if (props.type === "group") return <InputGroup {...props} />;

    if (props.type === "number") return <InputNumber {...props} />;

    return <Input {...props} />;
  };

  return <>{renderInput()}</>;
};
