import React from "react";
import * as PropTypes from "prop-types";
import { SvgIconComponent as SvgIcon } from "../svg-icon/svg-icon.component";
import "./icon-button.style.scss";

export class IconButtonComponent extends React.PureComponent {
  render() {
    const { className } = this.props;

    return (
      <button className={className} onClick={this.props.onClickEvent} disabled={this.props.disabled}>
        <SvgIcon icon={this.props.icon} style={this.props.style} rotateIcon={this.props.rotateIcon} />
        {this.props.hasText && <span>{this.props.label}</span>}
      </button>
    );
  }
}

IconButtonComponent.propTypes = {
  className: PropTypes.string,
  icon: PropTypes.string.isRequired,
  onClickEvent: PropTypes.func.isRequired,
  hasText: PropTypes.bool,
  label: PropTypes.string,
  style: PropTypes.object,
  rotateIcon: PropTypes.bool,
  disabled: PropTypes.bool
};

IconButtonComponent.defaultProps = {
  className: "icon-button",
  hasText: false,
  label: "",
  disabled: false,
  rotateIcon: false,
  style: {}
};
