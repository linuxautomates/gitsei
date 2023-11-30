import React, { useState, useEffect, useRef } from "react";
import { Input } from "antd";
import { InputProps } from "antd/lib/input/Input";
import AntIcon from "../ant-icon/ant-icon.component";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import classnames from "classnames";

import "./ant-editable-input.style.scss";

export interface AntEditableInputProps extends Omit<InputProps, "onChange"> {
  editMode: boolean;
  onChange: (value: InputProps["value"]) => any;
  onStart: () => any;
}

const AntEditableInput: React.FC<AntEditableInputProps> = props => {
  const { value, onPressEnter } = props;

  const [inputValue, setInputValue] = useState<InputProps["value"]>(value || "");

  // This second destructuring
  // is to create an inputProps
  // that I can pass to the Input
  // component.
  const { editMode, onChange, onStart, className, style, ...inputProps } = props;

  const inputRef = useRef(null);
  useEffect(() => {
    if (inputRef.current && editMode) {
      // @ts-ignore
      inputRef.current.focus();
    }
  }, [editMode]);

  useEffect(() => {
    if (!inputValue) {
      onStart && onStart();
    }
  }, []);

  const onInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    setInputValue(value);
  };

  const onSubmit = () => {
    onChange(inputValue);
    // @ts-ignore
    onPressEnter && onPressEnter();
  };

  if (editMode) {
    return (
      <Input
        {...inputProps}
        className={classnames("ant-editable-input", className)}
        value={inputValue}
        ref={inputRef}
        onChange={onInputChange}
        onPressEnter={onSubmit}
        onBlur={onSubmit}
        style={{ position: "relative", display: "block", ...style }}
        suffix={
          <AntIcon
            className="ant-editable-input__enter-icon"
            type="enter"
            theme="outlined"
            style={{
              position: "absolute",
              right: "0px",
              top: "50%",
              transform: "translate(0px, -50%)"
            }}
          />
        }
      />
    );
  } else {
    return (
      <div className="ant-editable-input__display" style={{ ...style }}>
        <div className={"ant-editable-input__display-value"}>{value}</div>
        <AntButton
          className="ant-editable-input__edit-button"
          type="link"
          icon="edit"
          theme="outlined"
          size="small"
          onClick={() => onStart()}
        />
      </div>
    );
  }
};

export default AntEditableInput;
