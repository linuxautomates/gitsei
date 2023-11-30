import React, { Component } from "react";
import { connect } from "react-redux";
import { compose } from "redux";
import axios from "axios";
import { notification } from "antd";
import { mapErrorStatetoProps, mapErrorDispatchtoProps } from "reduxConfigs/maps/errorMap";

const ErrorWrapper = WrappedComponent => {
  class ErrorHOC extends Component {
    constructor(props) {
      super(props);
      this.state = {
        error: false
      };
      this.renderError = this.renderError.bind(this);
      this.handleHTTPError = this.handleHTTPError.bind(this);
    }

    componentWillUnmount() {}

    // componentDidCatch(error, errorInfo) {
    //     console.log(error);
    //     console.log(errorInfo);
    //     this.props.addError({
    //         error: true,
    //         error_header: "",
    //         error_message: errorInfo
    //     });
    //     this.setState({error:true});
    // }

    renderError() {
      let errMsg = JSON.parse(JSON.stringify(this.props.error));
      if (errMsg.error) {
        notification.error({
          message: errMsg.error_header,
          description: errMsg.error_message,
          key: errMsg.id
        });
      }
    }

    handleHTTPError(error, header) {
      // this.props.addError(
      //     {
      //         error: true,
      //         error_header: header,
      //         error_message: error.toString()
      //     }
      // );
    }

    render() {
      // return the following functions
      // a) handling error adding and rendering
      // to be used while initializing a backend service
      return (
        <div>
          {this.renderError()}
          <WrappedComponent
            {...this.props}
            renderError={this.renderError}
            handleHTTPError={this.handleHTTPError}
          />
        </div>
      );
    }
  }
  return ErrorHOC;
};

export default compose(connect(mapErrorStatetoProps, mapErrorDispatchtoProps), ErrorWrapper);
