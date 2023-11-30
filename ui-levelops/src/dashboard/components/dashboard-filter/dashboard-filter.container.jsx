import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { SelectRestApiNew } from "shared-resources/helpers";
import { AntSelect } from "shared-resources/components";
import { Col, Row, Form, Divider } from "antd";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { getData, getError, getLoading } from "../../../utils/loadingUtils";
import { getDashboardFiltersSelector } from "reduxConfigs/selectors/dashboardFilters.selector";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { WORKSPACES, WORKSPACE_NAME_MAPPING } from "dashboard/constants/applications/names";

export class DashboardFilterContainer extends React.Component {
  constructor(props) {
    super(props);
    const { filters } = props;
    let productSelect = {};
    if (filters.product_id) {
      productSelect = { key: filters.product_id };
      const loading = getLoading(props.rest_api, "products", "get", filters.product_id);
      const error = getError(props.rest_api, "products", "get", filters.product_id);
      if (!loading && !error) {
        const product = getData(props.rest_api, "products", "get", filters.product_id);
        productSelect = { key: filters.product_id, label: product.name };
      } else {
        props.productsGet(filters.product_id);
      }
      const mappingsLoading = getLoading(props.rest_api, "mappings", "list", filters.product_id);
      const mappingsError = getError(props.rest_api, "mappings", "list", filters.product_id);
      if (!mappingsLoading && !mappingsError) {
      } else {
        props.genericList("mappings", "list", { filter: { product_id: filters.product_id } }, null, filters.product_id);
      }
    }

    this.state = {
      product: productSelect,
      integrations: [],
      mappings_loading: filters.product_id !== undefined,
      product_loading: filters.product_id !== undefined,
      updated_mappings: false,
      integrations_loading: false,
      selected_integrations: filters.integration_ids
        ? filters.integration_ids.map(integration => ({ key: integration }))
        : []
    };
    this.onFiltersChange = this.onFiltersChange.bind(this);
    this.handleIntegrationRemove = this.handleIntegrationRemove.bind(this);
  }

  componentWillUnmount() {
    //this.props.restapiClear("products", "get", "-1");
    //this.props.restapiClear("mappings", "list", "-1");
    //this.props.restapiClear("integrations", "get", "-1");
  }

  componentDidUpdate(prevProps, prevState, snapshot) {
    if (prevState.mappings_loading === true && this.state.mappings_loading === false) {
      prevProps.onChange(this.filters);
    }
  }

  static getDerivedStateFromProps(props, state) {
    let newState = { ...state };
    if (state.product_loading) {
      const loading = getLoading(props.rest_api, "products", "get", state.product.key);
      const error = getError(props.rest_api, "products", "get", state.product.key);
      if (!loading) {
        newState.product_loading = false;
        if (!error) {
          const product = getData(props.rest_api, "products", "get", state.product.key);
          newState.product = { key: product.id, label: product.name };
        }
      }
    }

    if (state.mappings_loading) {
      const loading = getLoading(props.rest_api, "mappings", "list", state.product.key);
      if (!loading) {
        newState.mappings_loading = false;
        // it will run only when it is updated from select, not from getting props from filters
        const records = getData(props.rest_api, "mappings", "list", state.product.key).records || [];
        newState.integrations_loading = records.length > 0;
        // LFE-302
        // integrations -> mappings is a one-to-many relation (apparently).
        // "records" is a list of mappings, so we need to dedupe if
        // we want to get a list of integrations from it
        const integrations_dedupe = {};
        const integrations_list = [];
        records.forEach(record => {
          if (!integrations_dedupe[record.integration_id]) {
            integrations_dedupe[record.integration_id] = true;
            integrations_list.push(record.integration_id);
          }
        });

        newState.integrations = integrations_list.map(integration_id => ({ key: integration_id }));
        integrations_list.forEach(integration_id => {
          const integrationLoading = getLoading(props.rest_api, "integrations", "get", integration_id);
          const integrationError = getError(props.rest_api, "integrations", "get", integration_id);
          if (!integrationLoading && !integrationError) {
          } else {
            props.integrationsGet(integration_id);
          }
        });

        if (state.updated_mappings) {
          newState.selected_integrations = integrations_list.map(integration_id => ({ key: integration_id }));
        }
      }
      return newState;
    }

    if (state.integrations_loading) {
      let integrationLoading = false;
      let integrations = [];
      let selectedIntegrations = [];
      // eslint-disable-next-line array-callback-return
      state.integrations.map(integration => {
        const loading = getLoading(props.rest_api, "integrations", "get", integration.key);
        const error = getError(props.rest_api, "integrations", "get", integration.key);
        if (loading) {
          integrationLoading = true;
        } else {
          if (!error) {
            const data = getData(props.rest_api, "integrations", "get", integration.key);
            integrations.push({ key: data.id, label: data.name });
            if (
              state.updated_mappings ||
              state.selected_integrations.find(integ => integ.key === data.id) !== undefined
            ) {
              selectedIntegrations.push({
                key: data.id,
                label: data.name,
                application: data.application
              });
            }
          }
        }
      });
      newState.integrations_loading = integrationLoading;
      if (!integrationLoading) {
        newState.integrations = integrations;
        newState.selected_integrations = selectedIntegrations;
      }
    }

    return newState;
  }

  onFiltersChange(value) {
    this.setState(
      {
        product: value || {},
        mappings_loading: true,
        updated_mappings: true,
        integrations_loading: true
      },
      () => {
        this.props.onChange(this.filters);
        this.props.genericList("mappings", "list", { filter: { product_id: value.key } }, null, value.key);
      }
    );
  }

  handleIntegrationRemove(integrationId) {
    let newIntegrations = this.state.integrations.filter(integration => integration.key !== integrationId);
    this.setState({ integrations: newIntegrations });
  }

  get filters() {
    return {
      product_id: this.state.product.key,
      integration_ids: this.state.selected_integrations.map(integration => integration.key)
    };
  }

  get disableUpdate() {
    let disable = false;
    disable = disable || this.state.product.key === undefined;
    return disable;
  }

  get readOnlyView() {
    return (
      <Row>
        <Col span={24}>Project: {this.state.product.label}</Col>
        <Divider />
        <Col span={24}>
          Integrations:
          {this.state.selected_integrations.map(integration => {
            return (
              <Row>
                {integration.label} ( {integration.application} )
              </Row>
            );
          })}
        </Col>
      </Row>
    );

    // return (
    //   <Row type={"flex"} justify={"start"} gutter={[10, 10]} align={"bottom"}>
    //     <Col span={6}>
    //       <h5>Product</h5>
    //       <AntText>{this.state.product.label}</AntText>
    //     </Col>
    //     <Col span={6}>
    //       <h5>Integrations</h5>
    //       <Row type={"flex"} justify={"start"} gutter={[10, 10]} align={"middle"}>
    //         {this.state.selected_integrations.map(integration => (
    //           <Col>
    //             <div style={{ alignItems: "center", verticalAlign: "center", display: "flex" }}>
    //               <IntegrationIcon type={integration.application} /> &nbsp;
    //               <AntText key={integration.key}>{integration.label}</AntText>
    //             </div>
    //           </Col>
    //         ))}
    //       </Row>
    //     </Col>
    //   </Row>
    // );
  }

  onReportFilter = (value, option) => {
    return option?.label?.toLowerCase().includes(value?.toLowerCase());
  };

  get editView() {
    const { onChange } = this.props;
    const loading = this.state.integrations_loading || this.state.mappings_loading;
    return (
      <>
        <Form.Item label={WORKSPACE_NAME_MAPPING[WORKSPACES]} required colon={false}>
          <SelectRestApiNew
            allowClear={false}
            style={{ width: "100%" }}
            uri={"products"}
            method={"list"}
            mode={"single"}
            value={this.state.product}
            labelInValue={true}
            loading={this.state.product_loading}
            onChange={this.onFiltersChange}
          />
        </Form.Item>
        <Form.Item label={"Integrations"} colon={false}>
          <AntSelect
            mode={"multiple"}
            value={loading ? [] : this.state.selected_integrations}
            onChange={value => this.setState({ selected_integrations: value || [] }, () => onChange(this.filters))}
            labelInValue={true}
            style={{ width: "100%" }}
            loading={loading}
            disabled={this.state.product.key === undefined}
            showArrow={true}
            onOptionFilter={this.onReportFilter}
            showSearch // Turns on filtering
            options={this.state.integrations
              .filter(integration => {
                return integration.key && integration.label;
              })
              .map(integration => {
                const new_integration = {
                  value: integration.key || "",
                  label: integration.label || ""
                };

                return new_integration;
              })}
          />
        </Form.Item>
      </>
    );
  }

  render() {
    const { edit } = this.props;
    // instead of select for integrations, just present tags which are closable and will delete integrations
    if (edit) {
      return this.editView;
    } else {
      return this.readOnlyView;
    }
  }
}

DashboardFilterContainer.propTypes = {
  filters: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  edit: PropTypes.bool.isRequired
};

DashboardFilterContainer.defaultProps = {
  filters: {
    product_id: undefined,
    integration_ids: []
  },
  edit: true
};

const mapStateToProps = state => ({
  rest_api: getDashboardFiltersSelector(state)
});

const mapDispatchToProps = dispatch => ({
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapGenericToProps(dispatch)
});

export default connect(mapStateToProps, mapDispatchToProps)(DashboardFilterContainer);
