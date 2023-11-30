import React from "react";
import * as PropTypes from "prop-types";
import { TooltipComponent as Tooltip } from "shared-resources/components/tooltip/tooltip.component";
import { SvgIconComponent as SvgIcon } from "shared-resources/components/svg-icon/svg-icon.component";
export { DropdownWrapperHelper } from "shared-resources/helpers/dropdown-wrapper/dropdown-wrapper.helper";

import { Options } from "./components";

import "./select.style.scss";

export class SelectComponent extends React.PureComponent {
  constructor(props) {
    super(props);

    this.state = {
      isDirty: false,
      isTouched: false
    };

    this.setIsDirty = this.setIsDirty.bind(this);
    this.toggleIsOpen = this.toggleIsOpen.bind(this);
    this.setIsTouched = this.setIsTouched.bind(this);
    this.onOptionClick = this.onOptionClick.bind(this);
    this.setTriggerRef = this.setTriggerRef.bind(this);

    this.triggerElement = null;
  }

  onOptionClick(value) {
    this.setIsDirty();
    this.toggleIsOpen();
    this.props.onOptionChangeEvent(value);
  }

  setIsDirty() {
    this.setState({
      isDirty: true
    });
  }

  setIsTouched() {
    this.setState({
      isTouched: true
    });
  }

  setTriggerRef(ref) {
    this.triggerElement = ref;
  }

  getMatClasses() {
    const { className, errors, value } = this.props;
    const { isDirty, isTouched } = this.state;
    const classes = [];

    if (isDirty) {
      classes.push(`${className}--dirty`);
    }

    if (this.state.boxIsOpen) {
      classes.push(`${className}--open`);
    }

    if (isTouched) {
      classes.push(`${className}--touched`);
    }

    if (value && value.toString().length && this.props.options.find(option => option.value === value)) {
      classes.push(`${className}--has-value`);
    }

    if (Object.keys(errors).length) {
      classes.push(`${className}--has-errors`);
    }

    if (this.props.disabled) {
      classes.push(`${className}--disabled`);
    }

    return classes.join(" ");
  }

  getDropdownWidth() {
    if (!this.triggerElement) {
      return null;
    }

    return this.triggerElement.getBoundingClientRect().width;
  }

  toggleIsOpen() {
    this.setState(state => ({
      isTouched: true,
      boxIsOpen: !state.boxIsOpen
    }));
  }

  render() {
    const { className, label, errors, options, value } = this.props;
    const { boxIsOpen } = this.state;
    const currentValue = options.find(option => option.value === value);
    const mappedOptions = options.map(item => ({
      ...item,
      isSelected: item.isSelected || (currentValue && currentValue.value === item.value) || false
    }));

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

    return (
      <div className={`${className} ${this.getMatClasses()}`}>
        <div
          className={`${className}__container`}
          onClick={this.toggleIsOpen}
          ref={this.setTriggerRef}
          role="presentation">
          {label && <label htmlFor={label}>{labelText}</label>}
          <div className={`${className}__current-value flex align-center justify-space-between`} id={label}>
            <div style={{ marginTop: "2px", textAlign: "center" }}>
              {(currentValue && currentValue.label) || this.props.placeholder}
            </div>
            <SvgIcon
              icon="arrowDown"
              style={{
                width: 14,
                height: 14
              }}
            />
          </div>
        </div>
        {boxIsOpen && !this.props.disabled && (
          <DropdownWrapperHelper triggerElement={this.triggerElement} onClose={this.toggleIsOpen}>
            <Options
              style={{
                width: this.getDropdownWidth()
              }}
              compactMode={this.props.compactMode}
              className={`${className}-options`}
              options={mappedOptions}
              onOptionChangeEvent={this.onOptionClick}
            />
          </DropdownWrapperHelper>
        )}
        {Object.keys(errors).map((error, key) => (
          <div key={key} className={`${className}__error`}>
            {errors[error]}
          </div>
        ))}
      </div>
    );
  }
}

SelectComponent.propTypes = {
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.any.isRequired,
      label: PropTypes.any.isRequired
    })
  ).isRequired,
  onOptionChangeEvent: PropTypes.func.isRequired,
  errors: PropTypes.object,
  disabled: PropTypes.bool,
  label: PropTypes.string,
  value: PropTypes.any,
  className: PropTypes.string,
  hasTooltip: PropTypes.bool,
  tooltipMessage: PropTypes.string,
  isRequired: PropTypes.bool,
  placeholder: PropTypes.string,
  compactMode: PropTypes.bool
};

SelectComponent.defaultProps = {
  errors: {},
  label: "",
  placeholder: "",
  value: "",
  disabled: false,
  className: "select",
  hasTooltip: false,
  tooltipMessage: "",
  isRequired: false,
  compactMode: false
};
