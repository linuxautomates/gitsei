import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { StepProgress, IntegrationIcon } from "shared-resources/components";
import ErrorWrapper from "hoc/errorWrapper";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { mapProductFormStatetoProps, mapFormDispatchToPros } from "reduxConfigs/maps/formMap";
import "./product-details.style.scss";
import queryString from "query-string";
import { ProductInformation, ProductStages } from "products/containers";
import { getError, getLoading } from "utils/loadingUtils";
import { RestProduct, RestStage } from "classes/RestProduct";

export class ProductDetailPage extends React.Component {
  constructor(props) {
    super(props);

    const values = queryString.parse(this.props.location.search);
    let productId = values.product;
    let productStep = values.step || 0;
    let productStage = values.stage || undefined;

    this.state = {
      loading: productId !== undefined,
      product_id: productId,
      product_step: parseInt(productStep),
      product_stage: productStage,
      information_loading: false,
      stages_loading: false,
      mappings_loading: false,
      create_loading: false,
      created: false,
      step_progress: {
        information: "inProgress",
        stages: "toDo",
        mappings: "toDo"
      },
      preview_items: []
    };
    this.handleSubmitStep = this.handleSubmitStep.bind(this);
    this.onFinishClickHandler = this.onFinishClickHandler.bind(this);
  }

  componentDidMount() {
    if (this.state.loading) {
      this.props.productsGet(this.state.product_id);
    }

    if (this.state.product_stage !== undefined) {
      // let product = Object.assign(
      //     Object.create( Object.getPrototypeOf(this.props.product_form)),
      //     this.props.product_form);
      //product.integration_stage = this.state.product_stage;
      //this.props.formUpdateObj("product_form", product);
      this.setState({ stages_loading: true });
    }
  }

  static getDerivedStateFromProps(props, state) {
    if (state.loading) {
      if (
        !getLoading(props.rest_api, "products", "get", state.product_id) &&
        !getError(props.rest_api, "products", "get", state.product_id)
      ) {
        let productForm = new RestProduct(props.rest_api.products.get[state.product_id].data);
        props.formUpdateObj("product_form", productForm);
        let previewItems = [];
        if (state.product_step !== 0) {
          // load up the preview items here
          previewItems.push(
            { label: "name", value: productForm.name, hasIcon: true, icon: "product" },
            { label: "description", value: productForm.description, hasIcon: false },
            { label: "owner", value: productForm.owner, hasIcon: false }
          );
        }
        let filter = {
          filter: {
            product_id: state.product_id
          },
          sort: [{ id: "order", desc: false }]
        };
        //const waitStagesList = debounce(props.stagesList, 500);
        //waitStagesList(filter);
        props.stagesList(filter);
        return {
          ...state,
          loading: false,
          stages_loading: true,
          preview_items: previewItems
        };
      }
    }

    if (state.information_loading) {
      // adapt this for create or update
      let method = state.product_id === undefined ? "create" : "update";
      let id = state.product_id || "0";
      if (!getLoading(props.rest_api, "products", method, id) && !getError(props.rest_api, "products", method, id)) {
        // list the stages here with filter = product_id, sorted by order
        let filter = {
          filter: {
            product_id: state.product_id || props.rest_api.products[method][id].data.id
          },
          sort: [{ id: "order", desc: false }]
        };
        //const waitStagesList = debounce(props.stagesList, 500);
        //waitStagesList(filter);
        props.stagesList(filter);
        return {
          ...state,
          information_loading: false,
          product_id: state.product_id || props.rest_api.products[method][id].data.id,
          stages_loading: true
        };
      }
    }

    if (state.stages_loading) {
      if (!getLoading(props.rest_api, "stages", "list", "0") && !getError(props.rest_api, "stages", "list", "0")) {
        if (props.rest_api.stages.list["0"].data.records.length === 0) {
          // maybe the mappings have not been created yet so try a couple of times
          // let filter = {
          //     filter: {
          //         product_id: state.product_id,
          //     },
          //     sort: [{id: "order", desc: false}]
          // };
          // const waitStagesList = debounce(props.stagesList, 500);
          // waitStagesList(filter);
          // return state;
          return {
            ...state,
            stages_loading: false
          };
        } else {
          const stages = props.rest_api.stages.list["0"].data.records.map(stage => new RestStage(stage));
          props.formUpdateObj("stages_form", stages);
          const stageIds = stages.map(stage => stage.id);
          props.mappingsList({ filter: { stage_ids: stageIds } });
          return {
            ...state,
            stages_loading: false,
            mappings_loading: true
          };
        }
      }
    }

    if (state.mappings_loading) {
      if (!getLoading(props.rest_api, "mappings", "list", "0") && !getError(props.rest_api, "mappings", "list", "0")) {
        let previewItems = state.preview_items.filter(item => item.label !== "integrations");
        previewItems.push({
          label: "integrations",
          value: (
            <div className={`flex direction-row justify-start`}>
              {props.rest_api.mappings.list["0"].data.records.map(mapping => (
                <IntegrationIcon className="mr-5" type={mapping.integration_type} />
              ))}
            </div>
          )
        });
        return {
          ...state,
          mappings_loading: false,
          preview_items: previewItems
        };
      }
    }
  }

  componentWillUnmount() {
    this.props.formClear("product_form");
    this.props.formClear("mapping_form");
    this.props.formClear("jira_mapping_form");
    this.props.formClear("github_mapping_form");
    this.props.formClear("stages_form");
    this.props.restapiClear("mappings", "list", "-1");
    this.props.restapiClear("stages", "list", "-1");
    this.props.restapiClear("integrations", "get", "-1");
    this.props.restapiClear("integrations", "list", "-1");
    this.props.restapiClear("products", "get", "-1");
  }

  onFinishClickHandler() {
    //this.setState({create_loading: true}, () => this.props.productsCreate(this.props.product_form));
    this.props.formClear("product_form");
    this.props.formClear("mapping_form");
    this.props.formClear("jira_mapping_form");
    this.props.formClear("github_mapping_form");
    this.props.formClear("stages_form");
    this.props.restapiClear("products", "get", "-1");
    this.props.history.push("/admin/products");
  }

  handleSubmitStep(step, nextStep) {
    return () => {
      let previewItems = this.state.preview_items;
      let stepProgress = {
        ...this.state.step_progress,
        [step]: "done",
        [nextStep]: "inProgress"
      };
      let informationLoading = false;
      switch (step) {
        case "information":
          previewItems = [
            { label: "name", value: this.props.product_form.name, hasIcon: true, icon: "product" },
            { label: "description", value: this.props.product_form.description, hasIcon: false },
            { label: "owner", value: this.props.product_form.owner, hasIcon: false },
            ...this.state.preview_items
          ];
          // previewItems.push(
          //     { label: 'name', value: this.props.product_form.name, hasIcon: true, icon: "product" },
          //     { label: 'description', value: this.props.product_form.description, hasIcon: false},
          //     { label: 'owner', value: this.props.product_form.owner, hasIcon: false}
          // );
          informationLoading = true;
          if (!this.state.product_id) {
            this.props.productsCreate(this.props.product_form);
          } else {
            this.props.productsUpdate(this.state.product_id, this.props.product_form);
          }

          break;
        case "stages":
          break;
        case "mappings":
          break;
        default:
          break;
      }
      this.setState({
        step_progress: stepProgress,
        preview_items: previewItems,
        information_loading: informationLoading
      });
    };
  }

  render() {
    const steps = [
      {
        id: "information",
        status: this.state.step_progress.information,
        onSubmitStep: this.handleSubmitStep("information", "stages"),
        label: "Information",
        isNextDisabled: !this.props.product_form.valid, // change this to some sort of validation handler
        component: <ProductInformation />
      },
      {
        id: "stages",
        status: this.state.step_progress.integrations,
        onSubmitStep: this.handleSubmitStep,
        label: "Stages",
        isNextDisabled: false,
        component: (
          <ProductStages
            product_form={this.props.product_form}
            loading={this.state.stages_loading}
            stages={
              this.state.stages_loading
                ? []
                : this.props.rest_api.stages.list["0"] === undefined
                ? []
                : this.props.rest_api.stages.list["0"].data === undefined
                ? []
                : this.props.rest_api.stages.list["0"].data.records
            }
          />
        )
      }
    ];
    return (
      <div>
        <StepProgress
          startAtStep={this.state.product_step}
          hasPreview
          previewItems={this.state.preview_items}
          extraClass="step-progress__navigation-on-header"
          onFinishEvent={this.onFinishClickHandler}
          steps={steps}
        />
      </div>
    );
  }
}

ProductDetailPage.propTypes = {
  className: PropTypes.string,
  product_step: PropTypes.number
};

ProductDetailPage.defaultProps = {
  className: "product-detail-page",
  product_step: 0
};

const mapStatetoProps = state => {
  return {
    ...mapProductFormStatetoProps(state),
    ...mapRestapiStatetoProps(state)
  };
};

const mapDispatchtoProps = dispatch => {
  return {
    ...mapFormDispatchToPros(dispatch),
    ...mapRestapiDispatchtoProps(dispatch)
  };
};

export default ErrorWrapper(connect(mapStatetoProps, mapDispatchtoProps)(ProductDetailPage));
