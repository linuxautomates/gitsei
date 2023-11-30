/*eslint-disable*/
import React, { Component } from "react";

class FixedPlugin extends Component {
  constructor(props) {
    super(props);
    this.state = {
      classes: "dropdown show-dropdown open",
      bg_checked: true,
      bgImage: this.props.bgImage,
      switched: false,
      navbar_checked: false
    };
    this.handleClick = this.handleClick.bind(this);
    this.onChangeClick = this.onChangeClick.bind(this);
    this.onNavbarClick = this.onNavbarClick.bind(this);
    this.onMiniClick = this.onMiniClick.bind(this);
  }
  handleClick() {
    this.props.handleFixedClick();
  }
  onChangeClick() {
    this.setState({ bg_checked: !this.state.bg_checked });
    this.props.handleHasImage(this.state.bg_checked);
  }
  onNavbarClick() {
    this.setState({ navbar_checked: !this.state.navbar_checked });
    this.props.handleNavbarClick(this.state.navbar_checked);
  }
  onMiniClick() {
    this.props.handleMiniClick();
  }

  render() {
    return <div />;
  }
}

export default FixedPlugin;
