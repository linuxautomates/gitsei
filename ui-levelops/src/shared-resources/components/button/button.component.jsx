import React from "react";
import * as PropTypes from "prop-types";

import "./button.style.scss";

export class ButtonComponent extends React.PureComponent {
  render() {
    const { className, color, skin } = this.props;

    return (
      <button
        disabled={this.props.disabled}
        onClick={this.props.onClick}
        id={this.props.id}
        className={`${className} ${className}--${color} ${className}--${skin}`}
        type={this.props.type}>
        <span>{this.props.children}</span>
      </button>
    );
  }
}

ButtonComponent.propTypes = {
  className: PropTypes.string,
  children: PropTypes.oneOfType([PropTypes.element, PropTypes.string]).isRequired,
  type: PropTypes.oneOf(["button", "submit"]),
  disabled: PropTypes.bool,
  id: PropTypes.string,
  onClick: PropTypes.func,
  color: PropTypes.oneOf(["basic", "primary", "accent", "warn"]),
  skin: PropTypes.oneOf(["raised", "text"])
};

ButtonComponent.defaultProps = {
  className: "button",
  type: "button",
  onClick: () => null,
  disabled: false,
  color: "basic",
  id: "",
  skin: "raised"
};
