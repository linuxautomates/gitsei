import React from "react";
import { connect } from "react-redux";
import Loader from "components/Loader/Loader";
import { AntRow, AntCol, AntButton, AntTitle } from "shared-resources/components";
import { getError, getLoading } from "utils/loadingUtils";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import queryString from "query-string";
import { ReportSummary, ReportTable } from "configurations/components/reports";
import { ReportPrint } from "configurations/containers/reports";
import { Modal, Descriptions, Avatar } from "antd";
import ReactToPrint from "react-to-print";
import { get } from "lodash";
import CSVReportTable from "../../plugin-results/plugin-result-report-table/PluginResultReportTable";
import DetailsCardComponent from "configurations/pages/plugin-results/plugin-results-details/DetailsCardComponent";
import { RestTags } from "classes/RestTags";
import { getData } from "utils/loadingUtils";
import { AntForm, AntFormItem, AntModal } from "shared-resources/components";
import { SelectRestapi } from "shared-resources/helpers";
import { mapFormStateToProps, mapFormDispatchToPros } from "reduxConfigs/maps/formMap";
import { getIdsMap } from "reduxConfigs/actions/restapi/genericIdsMap.actions";
import { v1 as uuid } from "uuid";

export class ReportDetailsPage extends React.Component {
  constructor(props) {
    super(props);
    const values = queryString.parse(this.props.location.search);
    const reportId = values.report;
    this.state = {
      result_id: reportId,
      loading: reportId !== undefined,
      product_name: "UNKNOWN",
      view_details: undefined,
      product_loading: false,
      selected_plugin_result_tags: [],
      show_edit_tags_modal: false,
      creating_tags: false,
      create_tags: [],
      updating_plugin_result: false,
      tags_loading: false
    };
    this.reportRef = React.createRef();
    this.viewDetails = this.viewDetails.bind(this);
    this.detailsModal = this.detailsModal.bind(this);
    this.getSummary = this.getSummary.bind(this);
  }

  componentDidMount() {
    if (this.state.result_id !== undefined) {
      this.setState(
        {
          loading: true,
          product_loading: false
        },
        () => this.props.pluginResultsGet(this.state.result_id)
      );
    }
  }

  static getDerivedStateFromProps(props, state) {
    const { creating_tags, create_tags, selected_plugin_result_tags, result_id, updating_plugin_result } = state;
    const { rest_api } = props;
    if (state.loading) {
      const loading = getLoading(props.rest_api, "plugin_results", "get", state.result_id);
      const error = getError(props.rest_api, "plugin_results", "get", state.result_id);
      if (!loading && !error) {
        const data = props.rest_api.plugin_results.get[state.result_id].data;
        data.product_ids.forEach(id => props.productsGet(id));
        const ids = {
          tag_ids: data.tags
        };
        const formName = `tags_filters_map_${uuid()}`;
        props.getIdsMap(formName, ids);
        return {
          ...state,
          loading: false,
          product_loading: true,
          tags_loading: true,
          product_ids: data.product_ids,
          formName,
          tag_ids: ids
        };
      }
    }

    if (state.tags_loading) {
      const { tag_ids } = state;
      const formState = props.form_state[state.formName] || {};
      if (Object.keys(tag_ids).length === Object.keys(formState).length) {
        let tags = [];
        if (formState.tag_ids.length > 0) {
          tags = formState.tag_ids.map(tag => ({ key: tag.id, label: tag.name }));
        }
        return {
          ...state,
          selected_plugin_result_tags: tags,
          tags_loading: false
        };
      }
    }

    if (state.product_loading) {
      let productLoading = false;
      let productNames = [];
      const data = props.rest_api.plugin_results.get[state.result_id].data;
      data.product_ids.forEach(id => {
        const loading = getLoading(props.rest_api, "products", "get", id);
        const error = getError(props.rest_api, "products", "get", id);
        if (!loading && !error) {
          productNames.push(props.rest_api.products.get[id].data.name);
        } else {
          productLoading = true;
        }
      });

      return {
        ...state,
        product_name: productLoading ? "UNKNOWN" : productNames.join(", "),
        product_loading: productLoading
      };
    }

    if (creating_tags) {
      let createTagsLoading = false;
      let newlyCreatedTags = [];

      create_tags.forEach(tag => {
        if (getLoading(rest_api, "tags", "create", tag.key)) {
          createTagsLoading = true;
        } else if (!getError(rest_api, "tags", "create", tag.key)) {
          newlyCreatedTags.push({
            key: props.rest_api.tags.create[tag.key].data.id,
            label: tag.label[1]
          });
        }
      });

      if (!createTagsLoading) {
        const { existingTags } = RestTags.getNewAndExistingTags(selected_plugin_result_tags);
        const tags = [...existingTags, ...newlyCreatedTags];
        props.pluginResultsUpdate(result_id, {
          tags: tags.map(tag => tag.label)
        });
        return {
          ...state,
          creating_tags: false,
          updating_plugin_result: true
        };
      }
    } else if (updating_plugin_result) {
      if (!getLoading(rest_api, "plugin_results", "update", result_id)) {
        props.restapiClear("plugin_results", "update", "-1");
        return {
          ...state,
          updating_plugin_result: false
        };
      }
    }

    return null;
  }

  componentWillUnmount() {
    this.props.restapiClear("plugin_results", "get", "-1");
    this.props.restapiClear("products", "get", "-1");
    if ((this.state.formName || "").length) {
      this.props.formClear(this.state.formName);
    }
  }

  viewDetails(id) {
    console.log(`setting id to ${id}`);
    this.setState({ view_details: id });
  }

  detailsModal() {
    console.log("building details modal");
    const report = this.props.rest_api.plugin_results.get[this.state.result_id].data;
    const details = report.results.data[this.state.view_details];
    return (
      <Modal
        title={this.state.view_details}
        visible={this.state.view_details !== undefined}
        onOk={e => this.setState({ view_details: undefined })}
        onCancel={e => this.setState({ view_details: undefined })}
        width={"700px"}
        footer={null}>
        <AntRow>
          {report.tool === "report_praetorian" && (
            <AntCol span={24}>
              <AntRow gutter={[10, 10]}>
                {Object.keys(details.meta.score).map(score => {
                  return (
                    <>
                      <AntCol span={6}>
                        <span style={{ marginRight: "10px" }}>{score.replace(/_/g, " ")}</span>
                      </AntCol>
                      <AntCol span={4}>
                        <Avatar>{details.meta.score[score].value}</Avatar>
                      </AntCol>
                    </>
                  );
                })}
              </AntRow>
            </AntCol>
          )}
          {/* eslint-disable-next-line  array-callback-return */}
          {Object.keys(details).map(detail => {
            if (detail !== "meta") {
              return (
                <AntCol span={24}>
                  <Descriptions title={detail.replace(/_/g, " ").toUpperCase()}>
                    <Descriptions.Item>{details[detail]}</Descriptions.Item>
                  </Descriptions>
                </AntCol>
              );
            }
          })}
        </AntRow>
      </Modal>
    );
  }

  getSummary() {
    const report = this.props.rest_api.plugin_results.get[this.state.result_id].data;
    if (report.tool === "report_praetorian") {
      let summary = {};
      Object.keys(report.results.summary || {}).forEach(item => {
        delete report.results.summary[item].total_issues;
        let issues = {};
        Object.keys(report.results.summary[item] || {}).forEach(issue => {
          issues[issue.replace("total_", "")] = report.results.summary[item][issue];
        });
        summary[item] = { issues_by_severity: issues };
      });
      return {
        summary: summary || {},
        details: report?.results?.metadata || {}
      };
    } else {
      return {
        summary: report?.results?.aggregations || {},
        details: report?.results?.metadata || {}
      };
    }
  }

  result = () => getData(this.props.rest_api, "plugin_results", "get", this.state.result_id);

  onEditTagClick = () => {
    this.toggleEditTagsModal();
  };

  toggleEditTagsModal = () => {
    this.setState(state => ({ show_edit_tags_modal: !state.show_edit_tags_modal }));
  };

  handleTagsChange = tags => {
    this.setState({ selected_plugin_result_tags: tags });
  };

  handleTagsSave = () => {
    this.toggleEditTagsModal();
    const { selected_plugin_result_tags, result_id } = this.state;
    const createTags = RestTags.getNewAndExistingTags(selected_plugin_result_tags).newTags;
    if (createTags.length > 0) {
      this.setState(
        {
          creating_tags: true,
          create_tags: createTags
        },
        () => {
          createTags.forEach(tag => {
            let newTag = new RestTags();
            newTag.name = tag.key.replace("create:", "");
            this.props.tagsCreate(newTag, tag.key);
          });
        }
      );
      return;
    }
    this.setState({ updating_plugin_result: true }, () => {
      this.props.pluginResultsUpdate(result_id, {
        tags: selected_plugin_result_tags.map(tag => tag.label)
      });
    });
  };

  get editTagsModal() {
    const { selected_plugin_result_tags, show_edit_tags_modal } = this.state;
    if (!show_edit_tags_modal) {
      return null;
    }
    return (
      <AntModal
        visible
        title="Edit tags"
        onOk={this.handleTagsSave}
        onCancel={this.handleModalClose}
        okText="Save"
        closable>
        <AntForm layout="vertical">
          <AntFormItem label="Tags" colon={false}>
            <SelectRestapi
              placeholder="Choose Tags..."
              //rest_api={this.props.rest_api}
              uri="tags"
              fetchData={this.props.tagsList}
              searchField="name"
              value={selected_plugin_result_tags}
              allowClear={false}
              mode="multiple"
              createOption
              onChange={this.handleTagsChange}
            />
          </AntFormItem>
        </AntForm>
      </AntModal>
    );
  }

  handleModalClose = () => {
    this.toggleEditTagsModal();
    const formState = this.props.form_state[this.state.formName] || {};
    let tags = [];
    if (formState.tag_ids.length > 0) {
      tags = formState.tag_ids.map(tag => ({ key: tag.id, label: tag.name }));
    }
    this.setState({
      selected_plugin_result_tags: tags
    });
  };

  render() {
    if (this.state.loading || this.state.result_id === undefined) {
      return <Loader />;
    }
    const report = this.props.rest_api.plugin_results.get[this.state.result_id].data;
    report.products = this.state.product_name;
    const summary = this.getSummary();
    console.log("from summary", summary);
    return (
      <>
        {this.state.view_details && this.detailsModal()}
        <div align={"right"} style={{ marginBottom: "10px" }}>
          <ReactToPrint
            trigger={() => <AntButton icon={"printer"}>Print Report</AntButton>}
            content={() => this.reportRef.current}
            copyStyles={true}
          />
        </div>
        <ReportPrint
          reports={[report]}
          ref={this.reportRef}
          tags={this.state.selected_plugin_result_tags}
          products={this.state.product_name}
        />
        <div>
          <AntRow gutter={[10, 10]} justify={"space-between"}>
            <AntCol span={24}>
              <AntTitle level={4}>{(report.plugin_name || "").replace(/_/g, " ").toUpperCase()}</AntTitle>
            </AntCol>
          </AntRow>
          <AntRow gutter={[10, 10]}>
            <AntCol span={8}>
              <AntRow>
                <DetailsCardComponent
                  productName={this.state.product_name}
                  resultData={report || {}}
                  handleOnEditClick={this.onEditTagClick}
                  tagArray={this.state.selected_plugin_result_tags}
                  print={false}
                />
              </AntRow>
              {Object.keys(summary.summary).length > 0 && (
                <AntRow style={{ marginTop: "1rem" }}>
                  <ReportSummary data={summary} type={report.tool} />
                </AntRow>
              )}
            </AntCol>
            <AntCol span={16}>
              {report.tool === "csv" ? (
                <div style={{ maxWidth: "90rem" }}>
                  <CSVReportTable resultData={report.results || {}} print={false} />
                </div>
              ) : (
                <ReportTable
                  data={Object.keys(get(report, ["results", "data"], {})).map(record => ({
                    ...report.results.data[record],
                    id: record
                  }))}
                  type={report.tool}
                  details={this.viewDetails}
                />
              )}
            </AntCol>
          </AntRow>
        </div>
        {this.editTagsModal}
      </>
    );
  }
}

const mapStatetoProps = state => {
  return {
    ...mapRestapiStatetoProps(state),
    ...mapFormStateToProps(state)
  };
};

const mapDispatchtoProps = dispatch => {
  return {
    ...mapRestapiDispatchtoProps(dispatch),
    ...mapFormDispatchToPros(dispatch),
    getIdsMap: (formName, filters) => dispatch(getIdsMap(formName, filters))
  };
};

export default connect(mapStatetoProps, mapDispatchtoProps)(ReportDetailsPage);
