import React from "react";
import * as PropTypes from "prop-types";
import { TooltipComponent as Tooltip } from "shared-resources/components/tooltip/tooltip.component";
import "./select-wrapper.style.scss";

export class SelectWrapperHelper extends React.PureComponent {
  constructor(props) {
    super(props);
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

    if (this.props.label) {
      classes.push(`${className}--has-label`);
    }

    return classes.join(" ");
  }

  render() {
    const { className, label, prefix, name, errorMessage, hasError } = this.props;
    const selectStyles = {
      menu: styles => ({ ...styles, zIndex: 999 }),
      control: styles => ({ ...styles, padding: 0, margin: 0, minHeight: "fit-content", boxShadow: "none" }),
      input: styles => ({
        ...styles,
        "& input": {
          font: "inherit"
        },
        padding: 0,
        margin: 0
      })
    };
    let labelText = label;
    if (this.props.isRequired) {
      labelText = (
        <div className="flex justify-start">
          <Tooltip tooltip="This field is required">
            {label} <label style={{ color: "var(--red)" }}>*</label>
          </Tooltip>
        </div>
      );
    }

    const errorMessageClass = this.props.type === "textarea" ? "error-message-textarea" : "error-message";

    return (
      <div className={`${className} ${this.getInputClasses()}`} style={{ paddingBottom: "4.2px" }}>
        <div>{React.cloneElement(this.props.children, { styles: selectStyles })}</div>

        {label && (
          <div className="ant-form-item-label">
            <label htmlFor={`${prefix}-${name}`}>{labelText}</label>
          </div>
        )}
        {hasError && errorMessage && <div className={`${className}__${errorMessageClass}`}>{errorMessage}</div>}
      </div>
    );
  }
}

SelectWrapperHelper.propTypes = {
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

SelectWrapperHelper.defaultProps = {
  className: "custom-style",
  placeholder: "",
  type: "text",
  autocomplete: "off",
  value: "",
  onChangeEvent: () => {},
  onFocusEvent: () => {},
  onBlurEvent: () => {},
  prefix: ""
};
