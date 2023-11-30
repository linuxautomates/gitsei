import React, { Component } from "react";
import { connect } from "react-redux";
import { ServerPaginatedTable } from "shared-resources/containers";
import { TableRowActions } from "shared-resources/components";
import { tableColumns } from "./table-config";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { triageRulesDeleteState, triageRulesBulkDeleteState } from "reduxConfigs/selectors/restapiSelector";
import { get } from "lodash";
import { Button, notification } from "antd";
import { appendErrorMessagesHelper } from "utils/arrayUtils";
import { getBaseUrl, TRIAGE_ROUTES } from "constants/routePaths";

interface Props {
  history: any;
  triageRulesDelete: (id: string) => void;
  delete_state: any;
  bulk_delete_state: any;
  triageRulesBulkDelete: any;
}

interface State {
  delete_loading: boolean;
  delete_rule_id: string | undefined;
  reload: number;
  selected_ids: any;
  bulk_deleting: boolean;
}

export class TriageRulesListPage extends Component<Props, State> {
  constructor(props: any) {
    super(props);
    this.state = {
      delete_loading: false,
      delete_rule_id: undefined,
      reload: 1,
      selected_ids: [],
      bulk_deleting: false
    };
    this.onRemoveHandler = this.onRemoveHandler.bind(this);
    this.buildActionOptions = this.buildActionOptions.bind(this);
    this.clearSelectedIds = this.clearSelectedIds.bind(this);
    this.onSelectChange = this.onSelectChange.bind(this);
    this.onBulkDelete = this.onBulkDelete.bind(this);
  }

  static getDerivedStateFromProps(props: Props, state: State) {
    if (state.delete_loading) {
      console.log(props.delete_state);
      // @ts-ignore
      const loading = get(props.delete_state, [state.delete_rule_id, "loading"], true);
      // @ts-ignore
      const error = get(props.delete_state, [state.delete_rule_id, "error"], false);
      if (!loading) {
        let local_state = {};
        if (!error) {
          //@ts-ignore
          const data = get(props.delete_state, [state.delete_rule_id, "data"], {});
          const success = get(data, ["success"], true);
          if (!success) {
            notification.error({
              message: data["error"]
            });
          } else {
            local_state = {
              reload: state.reload + 1,
              selected_ids: state.selected_ids.filter((id: string) => id !== state.delete_rule_id)
            };
          }
        }
        return {
          ...local_state,
          delete_loading: false,
          delete_rule_id: undefined
        };
      }
    }

    if (state.bulk_deleting) {
      const { loading, error } = get(props.bulk_delete_state, "0", { loading: true, error: true });
      if (!loading) {
        if (!error) {
          const data = get(props.bulk_delete_state, ["0", "data", "records"], []);
          const errorMessages = appendErrorMessagesHelper(data);
          let errorOccurs = false;
          if (errorMessages.length) {
            errorOccurs = true;
            notification.error({
              message: errorMessages
            });
          }
          if (errorOccurs) {
            return {
              bulk_deleting: false,
              reload: state.reload + 1
            };
          } else {
            return {
              bulk_deleting: false,
              reload: state.reload + 1,
              selected_ids: []
            };
          }
        }
        return {
          bulk_deleting: false
        };
      }
    }
    return null;
  }

  componentWillUnmount() {
    // @ts-ignore
    this.props.restapiClear("triage_rules", "bulkDelete", "-1");
  }

  onRemoveHandler(ruleId: string) {
    this.setState(
      {
        delete_loading: true,
        delete_rule_id: ruleId
      },
      () => this.props.triageRulesDelete(ruleId)
    );
  }

  buildActionOptions(props: any) {
    const actions = [
      {
        type: "delete",
        id: props.id,
        onClickEvent: this.onRemoveHandler
      }
    ];
    // @ts-ignore
    return <TableRowActions actions={actions} />;
  }

  clearSelectedIds() {
    this.setState({
      selected_ids: []
    });
  }

  onSelectChange(rowKeys: any) {
    this.setState({
      selected_ids: rowKeys
    });
  }

  onBulkDelete() {
    this.props.triageRulesBulkDelete(this.state.selected_ids);
    this.setState({ bulk_deleting: true });
  }

  render() {
    const rowSelection = {
      selectedRowKeys: this.state.selected_ids,
      onChange: this.onSelectChange,
      hideDefaultSelections: false
    };
    const mappedColumns = tableColumns().map(column => {
      if (column.key === "id") {
        return {
          ...column,
          render: (item: any, record: any, index: number) => this.buildActionOptions(record)
        };
      }

      return column;
    });

    return (
      <>
        <ServerPaginatedTable
          reload={this.state.reload}
          pageName={"triageRulesList"}
          uri={"triage_rules"}
          method={"list"}
          columns={mappedColumns}
          hasFilters={false}
          rowSelection={rowSelection}
          clearSelectedIds={this.clearSelectedIds}
          onBulkDelete={this.onBulkDelete}
          hasDelete={true}
          bulkDeleting={this.state.bulk_deleting}
          customExtraContent={
            <Button
              type="primary"
              icon="plus"
              className="ml-10"
              onClick={() => {
                this.props.history.push(`${getBaseUrl()}${TRIAGE_ROUTES.CREATE}`);
              }}>
              Add Triage Rule
            </Button>
          }
        />
      </>
    );
  }
}

// @ts-ignore
const mapStateToProps = (state: any) => ({
  delete_state: triageRulesDeleteState(state),
  bulk_delete_state: triageRulesBulkDeleteState(state)
});

// @ts-ignore
export default connect(mapStateToProps, mapRestapiDispatchtoProps)(TriageRulesListPage);
