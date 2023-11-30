import React, { Component } from "react";

export default class ErrorNotFound extends Component {
  // eslint-disable-next-line no-useless-constructor
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <div className="main-content">
        <p>You do not have the permissions to view this page</p>
      </div>
    );
  }
}
