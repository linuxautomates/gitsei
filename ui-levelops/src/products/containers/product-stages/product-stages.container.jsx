import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { withRouter } from "react-router-dom";
import { AntCard, AntInput, Label, IntegrationIcon, AntButton } from "shared-resources/components";
import { ProductStage } from "products/components";
import { mapStagesFormStatetoProps, mapFormDispatchToPros } from "reduxConfigs/maps/formMap";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { IntegrationMapPlanning, IntegrationMapDev, IntegrationMapDefault } from "products/containers";

import Loader from "components/Loader/Loader";
import { getError, getLoading } from "../../../utils/loadingUtils";
import { validateEmail } from "../../../utils/stringUtils";

export class ProductStagesContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      expanded_stage: undefined,
      create_loading: false,
      stage_id: undefined,
      loading: true,
      integrations_loading: true,
      integrations: []
    };
    this.buildExpandedComponent = this.buildExpandedComponent.bind(this);
    this.handleExpandStage = this.handleExpandStage.bind(this);
    this.buildStages = this.buildStages.bind(this);
    this.buildIntegration = this.buildIntegration.bind(this);
    this.buildIntegrationStage = this.buildIntegrationStage.bind(this);
    this.handleAddIntegration = this.handleAddIntegration.bind(this);
    this.handleDoneIntegration = this.handleDoneIntegration.bind(this);
    this.handleCancelIntegration = this.handleCancelIntegration.bind(this);
    this.handleNewIntegration = this.handleNewIntegration.bind(this);
    this.onChangeHandler = this.onChangeHandler.bind(this);
  }

  componentWillMount() {
    this.setState({
      expanded_stage: this.props.expanded_stage
    });
  }

  static getDerivedStateFromProps(props, state) {
    if (state.loading) {
      if (!getLoading(props.rest_api, "mappings", "list", "0") && !getError(props.rest_api, "mappings", "list", "0")) {
        props.integrationsList();
        return {
          ...state,
          loading: false,
          integrations_loading: true
        };
      }
    }
    if (state.integrations_loading) {
      if (
        !getLoading(props.rest_api, "integrations", "list", "0") &&
        !getError(props.rest_api, "integrations", "list", "0")
      ) {
        return {
          ...state,
          integrations_loading: false,
          integrations: props.rest_api.integrations.list["0"].data.records
        };
      }
    }
    if (state.create_loading) {
      if (
        !getLoading(props.rest_api, "stages", "update", state.stage_id) &&
        !getError(props.rest_api, "stages", "update", state.stage_id)
      ) {
        return {
          ...state,
          create_loading: false,
          stage_id: undefined
        };
      }
    }
  }

  onChangeHandler(stageId) {
    return value => {
      let stages = this.props.stages_form;
      stages.forEach(stage => {
        if (stage.id === stageId) {
          stage.owner = value;
        }
      });
      this.props.formUpdateObj("stages_form", stages);
    };
  }

  handleStageUpdate(stageId) {
    return () => {
      this.setState({ create_loading: true, stage_id: stageId }, () => {
        let stage = this.props.stages_form.filter(stg => stg.id === stageId);
        this.props.stagesUpdate(stageId, stage[0]);
      });
    };
  }

  handleExpandStage(stage) {
    return e => {
      if (stage === this.state.expanded_stage) {
        this.setState({ expanded_stage: undefined });
      } else {
        this.setState({ expanded_stage: stage });
      }
    };
  }

  handleAddIntegration(stage) {
    let product = Object.assign(Object.create(Object.getPrototypeOf(this.props.product_form)), this.props.product_form);
    product.integration_stage = stage;
    this.props.formUpdateObj("product_form", product);
  }

  handleDoneIntegration() {
    let product = Object.assign(Object.create(Object.getPrototypeOf(this.props.product_form)), this.props.product_form);
    product.integration_stage = undefined;
    this.props.formUpdateObj("product_form", product);
    this.setState({ loading: true }, () => {
      this.props.mappingsList({
        filter: {
          stage_ids: this.props.stages_form.map(stage => stage.id)
        }
      });
    });
    // with done, actually create / update the mapping
  }

  handleCancelIntegration() {
    let product = Object.assign(Object.create(Object.getPrototypeOf(this.props.product_form)), this.props.product_form);
    product.integration_stage = undefined;
    this.props.formUpdateObj("product_form", product);
  }

  handleNewIntegration(stageId) {
    return e => {
      e.preventDefault();
      this.props.history.push(
        `admin/configuration/integrations/add-integration-page?return=/admin/products/add-product-page?step=1&stage=${stageId}`
      );
    };
  }

  buildExpandedComponent(stage) {
    return (
      <div className={`flex direction-column`}>
        <div className={`flex direction-row justify-start align-center`}>
          <Label type={"title"} text={"Supported Integrations"} />
          {stage.integration_types.map(integration => (
            <div style={{ marginLeft: "5px" }}>
              <IntegrationIcon type={integration} />
            </div>
          ))}
        </div>
        <div>
          <Label type={"description"} text={stage.description} />
        </div>
        <div className={`flex direction-row justify-start`} style={{ marginTop: "10px" }}>
          <div style={{ flexBasis: "40%" }}>
            <AntInput
              name="owner"
              value={stage.owner}
              label="Stage Lead"
              onChangeEvent={this.onChangeHandler(stage.id)}
              hasError={!validateEmail(stage.owner || "")}
              placeholder={"stagelead@product.com"}
              errorMessage={"Valid email required"}
            />
          </div>
          <div>
            <AntButton
              type={"primary"}
              id={`button-${stage.id}`}
              onClick={this.handleStageUpdate(stage.id)}
              disabled={!validateEmail(stage.owner || "")}>
              Update
            </AntButton>
          </div>
        </div>
      </div>
    );
  }

  buildStages() {
    return (
      <AntCard title="Configure SDLC Stages">
        {this.props.stages_form
          .sort((a, b) => a.order - b.order)
          .map(stage => (
            <ProductStage
              stage={stage}
              mappings={this.props.rest_api.mappings.list["0"].data.records.filter(
                mapping => mapping.stage_id === stage.id
              )}
              onExpand={this.handleExpandStage(stage.id)}
              onMapIntegration={this.handleAddIntegration}
              expandedComponent={this.buildExpandedComponent(stage)}
              collapsed={stage.id !== this.state.expanded_stage}
              status={"success"}
              integrations={this.state.integrations}
            />
          ))}
      </AntCard>
    );
  }

  buildIntegrationStage(stage) {
    let component = "";
    switch (stage.type) {
      case "PLANNING":
        component = IntegrationMapPlanning;
        break;
      case "DEVELOPMENT":
        component = IntegrationMapDev;
        break;
      default:
        component = IntegrationMapDefault;
    }
    return (
      <div className={`flex direction-column`}>
        {React.createElement(component, {
          stage: stage,
          onDone: this.handleDoneIntegration
        })}
      </div>
    );
  }

  buildIntegration(stage) {
    return (
      <AntCard title={`Map Integrations for ${stage.name.replace("_", " ").toUpperCase()}`}>
        {this.buildIntegrationStage(stage)}
      </AntCard>
    );
  }

  render() {
    const { className, style, product_form, loading, stages } = this.props;
    if (loading || this.state.loading || this.state.integrations_loading) {
      return <Loader />;
    }
    if (product_form.integration_stage === undefined) {
      return this.buildStages();
    } else {
      return this.buildIntegration(product_form.integration_stage);
    }
  }
}

ProductStagesContainer.propTypes = {
  className: PropTypes.string,
  style: PropTypes.object,
  product_form: PropTypes.object,
  loading: PropTypes.bool,
  stages: PropTypes.array,
  stages_form: PropTypes.object
};

ProductStagesContainer.defaultProps = {
  className: "",
  style: {},
  loading: false,
  stages: [],
  stages_form: []
};

const mapStatetoProps = state => {
  return {
    ...mapStagesFormStatetoProps(state),
    ...mapRestapiStatetoProps(state)
  };
};

const mapDispatchtoProps = dispatch => {
  return {
    ...mapFormDispatchToPros(dispatch),
    ...mapRestapiDispatchtoProps(dispatch)
  };
};

export default withRouter(connect(mapStatetoProps, mapDispatchtoProps)(ProductStagesContainer));
