import React from "react";
import * as PropTypes from "prop-types";
import { OptionComponent as Option } from "shared-resources/components/option/option.component";

import "./options.style.scss";

export class OptionsComponent extends React.PureComponent {
  render() {
    const { className, options } = this.props;

    return (
      <div className={className} style={this.props.style}>
        {options.map(item => (
          <Option
            key={item.value}
            option={item}
            compactMode={this.props.compactMode}
            onClickEvent={this.props.onOptionChangeEvent}
          />
        ))}
      </div>
    );
  }
}

OptionsComponent.propTypes = {
  style: PropTypes.object.isRequired,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.any.isRequired,
      label: PropTypes.any.isRequired,
      isSelected: PropTypes.bool.isRequired
    })
  ).isRequired,
  onOptionChangeEvent: PropTypes.func.isRequired,
  className: PropTypes.string,
  compactMode: PropTypes.bool
};

OptionsComponent.defaultProps = {
  className: "select-options",
  compactMode: false
};
