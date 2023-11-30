import React from "react";
import { connect } from "react-redux";
import { ServerPaginatedTable } from "shared-resources/containers";
import { TableRowActions } from "shared-resources/components";
import { getLoading } from "utils/loadingUtils";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { tableConfig } from "./table-config";
import Loader from "components/Loader/Loader";

export class ReportsListPage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      more_filters: {},
      partial_filters: {},
      delete_loading: false,
      q_id: undefined,
      tags_loading: false,
      selected_rows: [],
      selected_ids: [],
      quizzes_loading: false,
      report_ready: false
    };
    this.onEditHandler = this.onEditHandler.bind(this);
    this.onRemoveHandler = this.onRemoveHandler.bind(this);
    this.buildActionOptions = this.buildActionOptions.bind(this);
    this.onSelectChange = this.onSelectChange.bind(this);
  }

  componentWillReceiveProps(nextProps, nextContext) {
    if (this.state.delete_loading) {
      if (!getLoading(nextProps.rest_api, "quiz", "delete", this.state.q_id.toString())) {
        this.setState({
          delete_loading: false,
          q_id: undefined
        });
      }
    }
  }

  componentWillUnmount() {
    this.props.restapiClear("quiz", "delete", this.state.q_id.toString());
    this.props.restapiClear("tags", "bulk", "0");
  }

  onEditHandler(qId) {
    // let url = ANSWER_QUIZ_PAGE.concat(`?questionnaire=${qId}`);
    // window.location.href = '#'.concat(url);
    //this.props.history.push("/admin/answer-questionnaire-page?questionnaire=".concat(qId))
  }

  onRemoveHandler(qId) {
    // this.setState({
    //     delete_loading: true,
    //     q_id: qId,
    // }, () => this.props.quizDelete(qId))
  }

  buildActionOptions(props) {
    const actions = [
      {
        type: "eye",
        id: props.id,
        onClickEvent: this.onEditHandler
      },
      {
        type: "delete",
        id: props.id,
        onClickEvent: this.onRemoveHandler
      }
    ];
    return <TableRowActions actions={actions} />;
  }

  onSelectChange(rowKeys, selectedRows) {
    console.log(rowKeys);
    this.setState({
      selected_rows: rowKeys
    });
  }

  onSelectAll(selected, selectedRows, changeRows) {
    // console.log(selected);
    // console.log(selectedRows);
    // console.log(changeRows);
    // this.setState({
    //     selected_rows: rowKeys,
    // }, () => this.props.onSelectRows(selectedRows.map( row => row.id)))
  }

  render() {
    const rowSelection = {
      selectedRowKeys: this.state.selected_rows,
      onChange: this.onSelectChange,
      onSelectAll: this.onSelectAll,
      hideDefaultSelections: false
    };

    const mappedColumns = tableConfig.map(column => {
      if (column.dataIndex === "products") {
        return {
          ...column,
          apiCall: this.props.productsList
        };
      }
      if (column.dataIndex === "tags") {
        return {
          ...column,
          apiCall: this.props.tagsList
        };
      }
      return column;
    });
    mappedColumns.push({
      title: "Actions",
      key: "id",
      width: 150,
      render: props => this.buildActionOptions(props)
    });
    if (this.state.delete_loading) {
      return <Loader />;
    }
    return (
      <ServerPaginatedTable
        pageName={"reportslist"}
        restCall="getQuizzes"
        uri={"reports"}
        rowSelection={rowSelection}
        //searchBar
        displayCount={true}
        recordName="Reports"
        columns={mappedColumns}
        hasSearch={true}
        moreFilters={{}}
        partialFilters={{}}
      />
    );
  }
}

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(ReportsListPage);
