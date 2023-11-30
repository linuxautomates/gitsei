import React, { Component } from "react";
import "./custom-checkbox.style.scss";
import * as PropTypes from "prop-types";

class CustomCheckbox extends Component {
  constructor(props) {
    super(props);
    this.state = {
      is_checked: props.isChecked ? props.isChecked : false
    };
    this.handleClick = this.handleClick.bind(this);
  }

  handleClick(e) {
    let target = e.currentTarget;
    this.setState({ is_checked: !this.state.is_checked }, () => {
      if (this.props.customhandler) {
        this.props.customhandler(target);
      }
    });
  }

  render() {
    const { className, isChecked, number, label, inline, ...rest } = this.props;
    const classes = inline !== undefined ? "checkbox checkbox-inline" : "checkbox";
    return (
      <div className={`${className} ${classes}`}>
        <input
          id={number}
          type="checkbox"
          onChange={this.handleClick}
          //checked={this.state.is_checked}
          checked={this.props.isChecked}
          {...rest}
        />
        <label htmlFor={number}>{label}</label>
      </div>
    );
  }
}

CustomCheckbox.propTypes = {
  className: PropTypes.string,
  isChecked: PropTypes.bool,
  number: PropTypes.string,
  label: PropTypes.string,
  inline: PropTypes.bool
};

CustomCheckbox.defaultProps = {
  className: "custom-checkbox"
};

export default CustomCheckbox;
