import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { Card, Label } from "shared-resources/components";
import { SelectRestapi, Title } from "shared-resources/helpers";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import ErrorWrapper from "hoc/errorWrapper";
import { ServerPaginatedTable } from "shared-resources/containers";
import { getError, getLoading } from "utils/loadingUtils";
import { tableCell } from "utils/tableUtils";
import Loader from "components/Loader/Loader";
import ReactJson from "react-json-view";
import { RestSignatureLog } from "classes/RestSignatureLog";
import { Col, Divider, Form, Row, Select, Typography } from "antd";
import queryString from "query-string";
import { getIntegrationUrlMap } from "../constants/integrations";

const { Text } = Typography;
const { Option } = Select;

export class SignatureLogsListPage extends React.Component {
  constructor(props) {
    super(props);
    const values = queryString.parse(this.props.location.search);
    if (values.product) {
      this.props.productsGet(values.product);
    }

    this.state = {
      product_loading: values.product !== undefined,
      release_loading: values.release !== undefined,
      stage_loading: values.stage !== undefined,
      more_filters: {},
      partial_filters: {},
      highlighted_row: undefined,
      expanded: {},
      expanded_row_id: undefined,
      expanded_row_loading: false,
      expanded_signature_log: new RestSignatureLog(),
      products: undefined,
      stages: undefined,
      releases: undefined,
      integrations: undefined,
      signatures: undefined,
      query_params: values
    };
    this.subComponent = this.subComponent.bind(this);
    this.onRowClick = this.onRowClick.bind(this);
    this.onExpandedChange = this.onExpandedChange.bind(this);
    this.updateFilters = this.updateFilters.bind(this);
  }

  static getDerivedStateFromProps(props, state) {
    if (state.product_loading) {
      const productId = state.query_params.product;
      if (
        !getLoading(props.rest_api, "products", "get", productId) &&
        !getError(props.rest_api, "products", "get", productId)
      ) {
        return {
          ...state,
          product_loading: false,
          products: [
            {
              key: state.query_params.product,
              label: props.rest_api.products.get[productId].data.name
            }
          ]
        };
      }
    }

    if (state.release_loading) {
      const releaseId = state.query_params.release;
      if (!getLoading(props.rest_api, "releases", "list", "0") && !getError(props.rest_api, "releases", "list", "0")) {
        // newState.release_loading = false;
        // newState.releases = props.rest_api.releases.list["0"].data.records.filter(
        //     record => record.id === releaseId).map(
        //     release => ({label: release.name, key: release.id})
        // );
        return {
          ...state,
          release_loading: false,
          releases: props.rest_api.releases.list["0"].data.records
            .filter(record => record.id === releaseId)
            .map(release => ({ label: release.name, key: release.id }))
        };
      }
    }
    if (state.stage_loading) {
      const stageId = state.query_params.stage;
      if (!getLoading(props.rest_api, "stages", "list", "0") && !getError(props.rest_api, "stages", "list", "0")) {
        // newState.stage_loading = false;
        // newState.stages = props.rest_api.stages.list["0"].data.records.filter(
        //     stage => stage.id === stageId).map(
        //     rec => ({label: rec.name, key: rec.id})
        // );
        return {
          ...state,
          stage_loading: false,
          stages: props.rest_api.stages.list["0"].data.records
            .filter(stage => stage.id === stageId)
            .map(rec => ({ label: rec.name, key: rec.id }))
        };
      }
    }
    //return newState;
    if (state.expanded_row_loading) {
      if (
        !getLoading(props.rest_api, "signature_logs", "get", state.expanded_row_id) &&
        !getError(props.rest_api, "signature_logs", "get", state.expanded_row_id)
      ) {
        return {
          ...state,
          expanded_row_loading: false,
          expanded_signature_log: new RestSignatureLog(props.rest_api.signature_logs.get[state.expanded_row_id].data)
        };
      }
    }
  }

  subComponent(row) {
    if (this.state.expanded_row_id !== row.original.id) {
      this.setState({ expanded_row_id: row.original.id, expanded_row_loading: true }, () => {
        this.props.signatureLogsGet(row.original.id);
      });
    }
    return <div style={{ padding: "20px" }}>{this.state.expanded_row_loading ? <Loader /> : this.resultsJson()}</div>;
  }

  resultsJson() {
    const signatureLog = this.state.expanded_signature_log;
    return (
      <div className={`flex direction-column`}>
        <div className={`flex direction-row justify-start`}>
          {signatureLog.labels.map(label => (
            <div style={{ margin: "10px" }}>
              <Label type={"link"} text={label} />
            </div>
          ))}
        </div>
        <div>
          <ReactJson src={signatureLog.results} name={"results"} />
        </div>
      </div>
    );
  }

  onRowClick(e, t, rowInfo) {
    this.setState({ highlighted_row: rowInfo.original.id });
  }

  onExpandedChange(newExpanded) {
    // only one expanded row at a time
    const prevExpanded = this.state.expanded;
    Object.keys(prevExpanded).forEach(key => delete newExpanded[key]);
    this.setState({
      expanded: newExpanded,
      expanded_row_loading: true
    });
  }

  updateFilters() {
    let moreFilters = {
      product_ids: this.state.products ? this.state.products.map(product => product.key) : [],
      stage_ids: this.state.stages ? this.state.stages.map(stage => stage.key) : [],
      release_ids: this.state.releases ? this.state.releases.map(release => release.key) : [],
      integration_ids: this.state.integrations ? this.state.integrations.map(integ => integ.key) : [],
      signature_ids: this.state.signatures ? this.state.signatures.map(signature => signature.key) : []
    };
    this.setState({ more_filters: moreFilters });
  }

  render() {
    if (this.state.product_loading) {
      return <Loader />;
    }
    return (
      <div>
        <Card>
          <div className={`flex direction-column`}>
            <Title title={"Violation Logs"} button={false} />
            <Divider />
            <Form>
              <Row type={"flex"} justify={"space-between"} gutter={[10, 10]}>
                <Col span={4}>
                  <Form.Item label={<Text type={"secondary"}>Products</Text>} colon={false} htmlFor={"products"}>
                    <SelectRestapi
                      showSearch={true}
                      value={this.state.products}
                      filterOption={true}
                      mode={"multiple"}
                      allowClear={true}
                      showArrow={true}
                      onChange={value => {
                        this.setState({ products: value }, () => this.updateFilters());
                      }}
                      notFoundContent={null}
                      rest_api={this.props.rest_api}
                      uri={"products"}
                      fetchData={this.props.productsList}
                    />
                  </Form.Item>
                </Col>
                <Col span={4}>
                  <Form.Item label={<Text type={"secondary"}>Stages</Text>} colon={false}>
                    <SelectRestapi
                      showSearch={true}
                      disabled={!this.state.products}
                      value={this.state.stages}
                      filterOption={true}
                      moreFilters={
                        this.state.products ? { product_id: this.state.products.map(product => product.key) } : {}
                      }
                      mode={"multiple"}
                      allowClear={true}
                      showArrow={true}
                      onChange={value => {
                        this.setState({ stages: value }, () => this.updateFilters());
                      }}
                      notFoundContent={null}
                      rest_api={this.props.rest_api}
                      uri={"stages"}
                      fetchData={this.props.stagesList}
                    />
                  </Form.Item>
                </Col>
                <Col span={4}>
                  <Form.Item label={<Text type={"secondary"}>Releases</Text>} colon={false}>
                    <SelectRestapi
                      showSearch={true}
                      disabled={!this.state.products}
                      value={this.state.releases}
                      moreFilters={
                        this.state.products ? { product_id: this.state.products.map(product => product.key) } : {}
                      }
                      filterOption={true}
                      loading={this.state.release_loading}
                      mode={"multiple"}
                      allowClear={true}
                      showArrow={true}
                      onChange={value => {
                        this.setState({ releases: value }, () => this.updateFilters());
                      }}
                      notFoundContent={null}
                      rest_api={this.props.rest_api}
                      uri={"releases"}
                      fetchData={this.props.releasesList}
                    />
                  </Form.Item>
                </Col>
                <Col span={4}>
                  <Form.Item label={<Text type={"secondary"}>Integration Type</Text>} colon={false}>
                    <Select
                      showSearch={true}
                      value={this.state.integrations}
                      filterOption={true}
                      mode={"multiple"}
                      allowClear={true}
                      showArrow={true}
                      labelInValue={true}
                      onChange={value => {
                        this.setState({ integrations: value }, () => this.updateFilters());
                      }}
                      notFoundContent={null}>
                      {Object.keys(getIntegrationUrlMap()).map(integration => (
                        <Option key={integration}>{tableCell("integration_type", integration)}</Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
                <Col span={4}>
                  <Form.Item label={<Text type={"secondary"}>Signatures</Text>} colon={false}>
                    <SelectRestapi
                      showSearch={true}
                      value={this.state.signatures}
                      filterOption={true}
                      mode={"multiple"}
                      allowClear={true}
                      showArrow={true}
                      onChange={value => {
                        this.setState({ signatures: value }, () => this.updateFilters());
                      }}
                      notFoundContent={null}
                      rest_api={this.props.rest_api}
                      uri={"signatures"}
                      fetchData={this.props.signaturesList}
                    />
                  </Form.Item>
                </Col>
              </Row>
            </Form>
            <ServerPaginatedTable
              restCall="getSignatureLogs"
              moreFilters={this.state.more_filters}
              partialFilters={this.state.partial_filters}
              // getTrProps={(state, rowInfo, column) => {
              //     return {
              //         onMouseOver: (e,t) => {
              //             this.onRowClick(e,t,rowInfo);
              //         }
              //     };
              // }}
              SubComponent={this.subComponent}
              onExpandedChange={newExpanded => this.onExpandedChange(newExpanded)}
              expanded={this.state.expanded}
              expandedRows={false}
              //freezeWhenExpanded={true}
              columns={[
                {
                  Header: "Timestamp",
                  id: "timestamp",
                  accessor: "timestamp",
                  filterable: false,
                  sortable: false,
                  style: { textAlign: "center" },
                  headerStyle: {
                    textAlign: "center"
                  },
                  width: 100,
                  Cell: props => tableCell("created_at", props.value)
                },
                // {
                //     Header: "Success",
                //     id: "success",
                //     accessor: "success",
                //     filterable: false,
                //     sortable: false,
                //     //style: {textAlign: "center"},
                //     headerStyle: {
                //         textAlign: "center"
                //     },
                //     width: 100,
                //     Cell: (props) => (
                //         <div
                //             className={`flex direction-column justify-space-between
                //              ${this.props.className}__status
                //             ${this.props.className}__status--${props.value?"success":"error"}`}>
                //             {
                //                 props.value?
                //             <SvgIcon icon={"check"} style={{ width: '1.4rem', height: '1.4rem' }}/>:
                //             <SvgIcon icon={"close"} style={{ width: '1.4rem', height: '1.4rem' }}/>}
                //         </div>
                //     )
                // },
                {
                  Header: "Severity",
                  id: "severity",
                  accessor: "severity",
                  filterable: false,
                  sortable: false,
                  style: { textAlign: "center" },
                  headerStyle: {
                    textAlign: "center"
                  },
                  //width: 100,
                  Cell: props => tableCell("severity", props.value)
                },
                {
                  Header: "Workflow",
                  id: "workflow",
                  accessor: "workflow",
                  filterable: false,
                  sortable: false,
                  style: { textAlign: "center" },
                  headerStyle: {
                    textAlign: "center"
                  }
                  //width: 100,
                  //Cell: (props) => (tableCell("created_at",props.value))
                },
                {
                  Header: "Signature",
                  id: "signature",
                  accessor: "signature",
                  filterable: false,
                  sortable: false,
                  style: { textAlign: "center" },
                  headerStyle: {
                    textAlign: "center"
                  }
                  //width: 100,
                  //Cell: (props) => (tableCell("created_at",props.value))
                },
                {
                  Header: "Integration",
                  id: "integration_type",
                  accessor: "integration_type",
                  filterable: false,
                  sortable: false,
                  style: { textAlign: "center" },
                  headerStyle: {
                    textAlign: "center"
                  },
                  //width: 100,
                  Cell: props => tableCell("integration_type", props.value)
                },
                {
                  Header: "Product",
                  id: "product",
                  accessor: "product",
                  filterable: false,
                  sortable: false,
                  style: { textAlign: "center" },
                  headerStyle: {
                    textAlign: "center"
                  }
                  //width: 100,
                  //Cell: (props) => (tableCell("created_at",props.value))
                },
                {
                  Header: "Release",
                  id: "release",
                  accessor: "release",
                  filterable: false,
                  sortable: false,
                  style: { textAlign: "center" },
                  headerStyle: {
                    textAlign: "center"
                  }
                  //width: 100,
                  //Cell: (props) => (tableCell("created_at",props.value))
                }
              ]}
            />
          </div>
        </Card>
      </div>
    );
  }
}

SignatureLogsListPage.propTypes = {
  className: PropTypes.string
};

SignatureLogsListPage.defaultProps = {
  className: "signature-logs-list-page"
};

export default ErrorWrapper(connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(SignatureLogsListPage));
