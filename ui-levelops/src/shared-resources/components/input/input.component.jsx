import React from "react";
import * as PropTypes from "prop-types";
import { TooltipComponent as Tooltip } from "../tooltip/tooltip.component";
import "./input.style.scss";

export class InputComponent extends React.PureComponent {
  constructor(props) {
    super(props);

    this.onChangeInputHandler = this.onChangeInputHandler.bind(this);
  }

  onChangeInputHandler(event) {
    this.props.onChangeEvent(event.target.value);
  }

  getInputClasses() {
    const classes = [];
    const { className } = this.props;
    if ((this.props.value && this.props.value.toString().length) || this.props.value === 0) {
      classes.push(`${className}--has-value`);
    }

    if (this.props.hasError) {
      classes.push(`${className}--error`);
    }

    if (this.props.isReadOnly) {
      classes.push(`${className}--readonly`);
    }

    if (this.props.label) {
      classes.push(`${className}--has-label`);
    }

    return classes.join(" ");
  }

  render() {
    const { className, label, prefix, name, errorMessage, hasError } = this.props;
    let labelText = label;
    if (this.props.isRequired) {
      labelText = (
        <div className="flex justify-start">
          <Tooltip tooltip="This field is required">
            {label} <span style={{ color: "var(--red)" }}>*</span>
          </Tooltip>
        </div>
      );
    }

    const errorMessageClass = this.props.type === "textarea" ? "error-message-textarea" : "error-message";

    return (
      <div className={`${className} ${this.getInputClasses()}`}>
        {this.props.type === "textarea" ? (
          <textarea
            id={`${prefix}-${name}`}
            rows={this.props.rows || 4}
            value={this.props.value}
            name={name}
            autoComplete={this.props.autocomplete}
            onChange={this.onChangeInputHandler}
            onFocus={this.props.onFocusEvent}
            onBlur={this.props.onBlurEvent}
            readOnly={this.props.isReadOnly}
            onClick={this.props.onInputClickEvent}
            placeholder={this.props.placeholder}
            ref={this.props.inputRef}
          />
        ) : (
          <input
            id={`${prefix}-${name}`}
            type={this.props.type}
            value={this.props.value}
            name={name}
            autoComplete={this.props.autocomplete}
            onChange={this.onChangeInputHandler}
            onFocus={this.props.onFocusEvent}
            onBlur={this.props.onBlurEvent}
            readOnly={this.props.isReadOnly}
            onClick={this.props.onInputClickEvent}
            placeholder={this.props.placeholder}
            ref={this.props.inputRef}
            disabled={this.props.isReadOnly === undefined ? false : this.props.isReadOnly}
          />
        )}

        {label && <label htmlFor={`${prefix}-${name}`}>{labelText}</label>}
        {hasError && errorMessage && <div className={`${className}__${errorMessageClass}`}>{errorMessage}</div>}
      </div>
    );
  }
}

InputComponent.propTypes = {
  className: PropTypes.string,
  onInputClickEvent: PropTypes.func,
  placeholder: PropTypes.string,
  label: PropTypes.string,
  prefix: PropTypes.string,
  name: PropTypes.string,
  errorMessage: PropTypes.string,
  autocomplete: PropTypes.string,
  type: PropTypes.string,
  inputRef: PropTypes.any,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  onChangeEvent: PropTypes.func,
  onFocusEvent: PropTypes.func,
  onBlurEvent: PropTypes.func,
  isReadOnly: PropTypes.bool
};

InputComponent.defaultProps = {
  className: "custom-input",
  onInputClickEvent: () => {},
  placeholder: "",
  type: "text",
  autocomplete: "off",
  value: "",
  onChangeEvent: () => {},
  onFocusEvent: () => {},
  onBlurEvent: () => {},
  isReadOnly: false,
  prefix: ""
};
