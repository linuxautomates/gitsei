import React from "react";
import * as PropTypes from "prop-types";

import "./option.style.scss";

export class OptionComponent extends React.PureComponent {
  constructor(props) {
    super(props);

    this.onOptionClickHandler = this.onOptionClickHandler.bind(this);
    this.getClassName = this.getClassName.bind(this);
  }

  onOptionClickHandler() {
    this.props.onClickEvent(this.props.option.id);
  }

  getClassName() {
    const { className } = this.props;
    const classes = [className];
    if (this.props.option.isSelected) {
      classes.push(`${className}--selected`);
    }
    if (this.props.compactMode) {
      classes.push(`${className}--compact`);
    }

    return classes.join(" ");
  }

  render() {
    const { option } = this.props;
    return (
      <div className={this.getClassName()} onClick={this.onOptionClickHandler} role="presentation">
        {option.label}
      </div>
    );
  }
}

OptionComponent.propTypes = {
  className: PropTypes.string,
  onClickEvent: PropTypes.func.isRequired,
  compactMode: PropTypes.bool,
  option: PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    label: PropTypes.string.isRequired
  }).isRequired
};

OptionComponent.defaultProps = {
  className: "option",
  compactMode: false
};
