import React from "react";
import * as PropTypes from "prop-types";

import { getWrapperBestPosition } from "../../lib";
import { DOMWrapperComponent as DOMWrapper } from "shared-resources/helpers/dom-wrapper/dom-wrapper.component";

import "./dropdown-wrapper.style.scss";

export class DropdownWrapperHelper extends React.PureComponent {
  constructor(props) {
    super(props);

    this.state = {
      style: {}
    };

    this.ref = null;
    this.onSetRefHandler = this.onSetRefHandler.bind(this);
  }

  componentDidMount() {
    document.body.style.overflow = "hidden";
    if (document.body.offsetHeight > window.innerHeight) {
      document.body.style.paddingRight = "15px";
    }
    this.setStyle();
  }

  componentWillUnmount() {
    document.body.style.overflow = "";
    document.body.style.paddingRight = "";
  }

  onSetRefHandler(ref) {
    this.ref = ref;
  }

  setStyle() {
    if (Object.keys(this.props.style).length) {
      this.setState({
        style: this.props.style
      });
    } else {
      this.setState({
        style: getWrapperBestPosition(this.props.triggerElement, this.ref, "bottom").style
      });
    }
  }

  render() {
    const { className } = this.props;
    return (
      <DOMWrapper onClose={this.props.onClose}>
        <div className={className} style={this.state.style} ref={this.onSetRefHandler}>
          {this.props.children}
        </div>
      </DOMWrapper>
    );
  }
}

DropdownWrapperHelper.propTypes = {
  triggerElement: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
  className: PropTypes.string,
  style: PropTypes.object
};

DropdownWrapperHelper.defaultProps = {
  className: "dropdown-wrapper",
  style: {}
};
