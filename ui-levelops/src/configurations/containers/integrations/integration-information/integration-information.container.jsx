import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";

import { Input, Card } from "shared-resources/components";
import { RestIntegrations } from "classes/RestIntegrations";

export class IntegrationInformationContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      integration_data: new RestIntegrations()
    };
    this.onChangeHandler = this.onChangeHandler.bind(this);
  }

  onChangeHandler(field) {
    return value => {
      this.props.setInformation(field, value);
    };
  }

  render() {
    const { className, style } = this.props;
    return (
      <Card style={style}>
        <div className={`${className} flex direction-column`}>
          <Input
            name="name"
            value={this.props.information.name || ""}
            label="Name"
            onChangeEvent={this.onChangeHandler("name")}
          />
          <Input
            name="description"
            value={this.props.information.description || ""}
            label="Description"
            onChangeEvent={this.onChangeHandler("description")}
          />
        </div>
        <div style={{ textAlign: "right" }}>
          <span>{`integration id ${this.props.information.id}`}</span>
        </div>
      </Card>
    );
  }
}

IntegrationInformationContainer.propTypes = {
  className: PropTypes.string,
  setInformation: PropTypes.func.isRequired,
  style: PropTypes.object
};

IntegrationInformationContainer.defaultProps = {
  className: "",
  style: {}
};

export const mapStateToProps = state => ({});

export const mapDispatchToProps = dispatch => ({});

export default connect(mapStateToProps, mapDispatchToProps)(IntegrationInformationContainer);
