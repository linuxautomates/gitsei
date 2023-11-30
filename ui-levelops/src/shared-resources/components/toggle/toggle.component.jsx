import React from "react";
import * as PropTypes from "prop-types";

import "./toggle.style.scss";

export class ToggleComponent extends React.PureComponent {
  render() {
    const { className, id, checked, onChange } = this.props;
    return (
      <div className={`${className} m-0`}>
        <input
          type="checkbox"
          name="switch"
          className="switch-checkbox"
          id={id}
          checked={checked}
          onChange={onChange}
        />
        <label className="switch-labels" htmlFor={id}>
          <span className="switch-text"></span>
          <span className="switch-dot"></span>
        </label>
      </div>
    );
  }
}

ToggleComponent.propTypes = {
  className: PropTypes.string,
  id: PropTypes.string,
  onChange: PropTypes.func,
  checked: PropTypes.bool,
  color: PropTypes.oneOf(["primary", "warn"])
};

ToggleComponent.defaultProps = {
  className: "switch",
  onChange: () => null,
  checked: false,
  color: "primary"
};
