import React from "react";
import { connect } from "react-redux";
import { ServerPaginatedTable } from "shared-resources/containers";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { TableRowActions } from "shared-resources/components";
import { tableCell } from "utils/tableUtils";
import { getBaseUrl } from 'constants/routePaths'
import { tableColumns } from "./table-config";

export class WorkflowsListPage extends React.PureComponent {
  constructor(props) {
    super(props);

    this.state = {
      search_term: "",
      highlighted_row: null,
      partial_filters: {},
      delete_signature_id: undefined
    };

    this.onRowClickHandler = this.onRowClickHandler.bind(this);
    this.onEditHandler = this.onEditHandler.bind(this);
    this.onRemoveHandler = this.onRemoveHandler.bind(this);
    this.buildActionOptions = this.buildActionOptions.bind(this);
  }

  componentWillUnmount() {
    this.props.restapiClear("workflows", "list", "0");
  }

  onRowClickHandler(e, t, rowInfo) {
    this.setState({
      highlighted_row: rowInfo.original.id
    });
  }

  onEditHandler(workflowId) {
    this.props.history.push(`${getBaseUrl()}/workflows/workflow-editor?workflow=${workflowId}`);
  }

  onRemoveHandler(workflowId) {
    this.setState(
      {
        delete_loading: true,
        delete_signature_id: workflowId
      },
      () => this.props.workflowsDelete(workflowId)
    );
  }

  buildActionOptions(props) {
    const actions = [
      {
        type: "edit",
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

  render() {
    const PAGE_ERROR = "Could not fetch workflows";

    const mappedColumns = tableColumns.map(column => {
      if (["products", "priority", "enabled", "created_at"].includes(column.key)) {
        return {
          ...column,
          render: props => tableCell(column.key, props)
        };
      }
      return column;
    });

    mappedColumns.push({
      title: "Actions",
      key: "actions",
      dataIndex: "id",
      width: 100,
      render: (props, item) => this.buildActionOptions(item)
    });

    return (
      <ServerPaginatedTable
        restCall="getWorkflows"
        uri="workflows"
        backendErrorMessage={PAGE_ERROR}
        columns={mappedColumns}
        getTrProps={(state, rowInfo) => {
          return {
            onMouseOver: (e, t) => {
              this.onRowClickHandler(e, t, rowInfo);
            }
          };
        }}
        partialFilters={this.state.partial_filters}
        hasFilters={false}
        hasSearch
        generalSearchField="policy"
      />
    );
  }
}

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(WorkflowsListPage);
