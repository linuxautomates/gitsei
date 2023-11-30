import React from "react";
import { getPageSettingsSelector } from "reduxConfigs/selectors/pagesettings.selector";
import { mapPageSettingsDispatchToProps } from "reduxConfigs/maps/pagesettings.map";
import { connect } from "react-redux";
import ErrorWrapper from "hoc/errorWrapper";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { tableColumns } from "./table-config";
import { TableRowActions } from "shared-resources/components";
import { ServerPaginatedTable } from "shared-resources/containers";
import { CONFIG_TABLE_ROUTES, getBaseUrl } from "../constants/routePaths";
import Loader from "../components/Loader/Loader";
import { RestConfigTable } from "../classes/RestConfigTable";
import { EditCloneModal } from "../shared-resources/components";
import { checkTemplateNameExists } from "../configurations/helpers/checkTemplateNameExits";
import { NAME_EXISTS_ERROR } from "../constants/formWarnings";
import { debounce, get } from "lodash";
import { v1 as uuid } from "uuid";
import {
  configTableCreateState,
  configTablesDeleteState,
  configTablesGetState,
  configTablesListState,
  configTablesBulkDeleteState
} from "reduxConfigs/selectors/restapiSelector";
import { appendErrorMessagesHelper } from "utils/arrayUtils";
import { notification } from "antd";
import { WebRoutes } from "../routes/WebRoutes";
import { mapSessionStatetoProps } from "reduxConfigs/maps/sessionMap";
import { BuildActionOptions } from './config-table-list.utils'

export class ConfigTableListPage extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      search_term: "",
      more_filters: {},
      partial_filters: {},
      delete_loading: false,
      d_id: undefined,
      set_header: false,
      clone_id: undefined,
      clone_table_loading: false,
      cloning_table: false,
      openEditCloneModel: false,
      name_exists: undefined,
      checking_name: false,
      clone_table_name: "",
      checkNameListId: undefined,
      selected_ids: [],
      reload: 1,
      bulk_deleting: false
    };

    this.onRemoveHandler = this.onRemoveHandler.bind(this);
    this.onCloneHandler = this.onCloneHandler.bind(this);
    this.onOkEditCloneModal = this.onOkEditCloneModal.bind(this);
    this.onCancelEditCloneModal = this.onCancelEditCloneModal.bind(this);
    this.checkTemplateName = this.checkTemplateName.bind(this);
    this.debounceCheckName = debounce(() => this.checkTemplateName(), 300);
    this.onSearchEvent = this.onSearchEvent.bind(this);
    this.clearSelectedIds = this.clearSelectedIds.bind(this);
    this.onSelectChange = this.onSelectChange.bind(this);
    this.onBulkDelete = this.onBulkDelete.bind(this);
  }

  static getDerivedStateFromProps(props, state) {
    if (state.delete_loading) {
      const id = state.d_id.toString();
      const loading = get(props.configTablesDeleteState, [id, "loading"], true);
      const error = get(props.configTablesDeleteState, [id, "error"], true);

      if (!loading) {
        let local_state = {};
        if (!error) {
          const data = get(props.configTablesDeleteState, [id, "data"], {});
          const success = get(data, ["success"], true);
          if (!success) {
            notification.error({
              message: data["error"]
            });
          } else {
            local_state = {
              selected_ids: state.selected_ids.filter(_id => _id !== id)
            };
          }
        }
        return {
          ...local_state,
          delete_loading: false,
          d_id: undefined
        };
      }
    }

    if (state.clone_table_loading) {
      const loading = get(props.configTablesGetState, [state.clone_id, "loading"], true);
      const error = get(props.configTablesGetState, [state.clone_id, "error"], true);

      if (!loading && !error) {
        const data = get(props.configTablesGetState, [state.clone_id, "data"]);
        if (data) {
          const cloneTableData = {
            name: state.clone_table_name,
            rows: data.rows,
            schema: data.schema
          };
          props.configTablesCreate(new RestConfigTable(cloneTableData));

          return {
            ...state,
            clone_table_loading: false,
            cloning_table: true
          };
        }
      }
    }

    if (state.cloning_table) {
      const loading = get(props.configTablesCreateState, ["loading"], true);
      const error = get(props.configTablesCreateState, ["error"], true);

      if (!loading && !error) {
        const tableId = get(props.configTablesCreateState, ["data", "id"], null);
        if (tableId) {
          props.history.push(`${getBaseUrl()}${CONFIG_TABLE_ROUTES.EDIT}?id=${tableId}`);
        }
      }
    }

    if (state.bulk_deleting) {
      const { loading, error } = props.configTablesBulkDeleteState;
      if (!loading) {
        if (!error) {
          const data = get(props.configTablesBulkDeleteState, ["data", "records"], []);
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
              selected_ids: [],
              reload: state.reload + 1
            };
          }
        }

        return {
          bulk_deleting: false
        };
      }
    }

    if (props.page && !state.set_header) {
      props.setPageSettings(props.location.pathname, {
        title: "Tables",
        action_buttons: {
          import: {
            type: "secondary",
            label: "Import CSV",
            icon: "import",
            hasClicked: false
          },
          add: {
            type: "primary",
            label: "Add Table",
            icon: "",
            hasClicked: false
          }
        }
      });

      return {
        ...state,
        set_header: true
      };
    }

    if (props.page && state.set_header && Object.keys(props.page).length > 0) {
      const page = props.page[props.location.pathname];
      if (page && page.hasOwnProperty("action_buttons")) {
        if (page.action_buttons.import && page.action_buttons.import.hasClicked === true) {
          props.history.push(`${getBaseUrl()}${CONFIG_TABLE_ROUTES.CREATE}?import=true`);
        }

        if (page.action_buttons.add && page.action_buttons.add.hasClicked === true) {
          props.history.push(`${getBaseUrl()}${CONFIG_TABLE_ROUTES.CREATE}`);
        }
        return state;
      }
    }
  }

  componentWillUnmount() {
    this.props.restapiClear("config_tables", "list", "0");
    this.props.restapiClear("config_tables", "get");
    this.props.restapiClear("config_tables", "create", "0");
    this.props.clearPageSettings(this.props.location.pathname);
    this.props.restapiClear("config_tables", "bulkDelete", "-1");
  }

  onRemoveHandler = id => {
    this.setState(
      {
        delete_loading: true,
        d_id: id
      },
      () => this.props.configTablesDelete(id)
    );
  };

  onCloneHandler = id => {
    this.setState({
      openEditCloneModel: true,
      clone_id: `${id}?expand=schema,rows`
    });
  };

  onOkEditCloneModal = () => {
    this.setState(
      {
        clone_table_loading: true,
        openEditCloneModel: false
      },
      () => this.props.configTablesGet(this.state.clone_id)
    );
  };

  onCancelEditCloneModal = () => {
    this.setState({
      openEditCloneModel: false,
      clone_id: undefined
    });
  };

  checkTemplateName = () => {
    const filters = {
      filter: {
        partial: {
          name: this.state.clone_table_name
        }
      }
    };
    const checkNameListId = uuid();
    this.props.configTablesList(filters, checkNameListId);
    this.setState({ checking_name: true, checkNameListId });
  };

  onSearchEvent = name => {
    this.setState({ clone_table_name: name }, () => this.debounceCheckName());
  };

  componentDidMount() {
    if (this.props.isSelfOnboardingUser) {
      this.props.history.push({ pathname: WebRoutes.dashboard.details(this.props.match.params, "") });
    }
  }

  componentDidUpdate(prevProps, prevState, snapshot) {
    if (this.props.isSelfOnboardingUser) {
      this.props.history.push({ pathname: WebRoutes.dashboard.details(this.props.match.params, "") });
    }
    if (this.state.checking_name) {
      const loading = get(this.props.configTablesListState, [this.state.checkNameListId, "loading"], true);
      const error = get(this.props.configTablesListState, [this.state.checkNameListId, "error"], true);
      if (!loading && !error) {
        const data = get(this.props.configTablesListState, [this.state.checkNameListId, "data", "records"], []);
        const prevName = this.state.clone_table_name;
        this.props.restapiClear("config_tables", "list", this.state.checkNameListId);
        this.setState({
          checking_name: false,
          name_exists: checkTemplateNameExists(prevName, data) ? NAME_EXISTS_ERROR : undefined
        });
      }
    }
  }

  clearSelectedIds() {
    this.setState({
      selected_ids: []
    });
  }

  onSelectChange(rowKeys) {
    this.setState({
      selected_ids: rowKeys
    });
  }

  onBulkDelete() {
    this.props.configTablesBulkDelete(this.state.selected_ids);
    this.setState({
      bulk_deleting: true
    });
  }

  render() {
    const rowSelection = {
      selectedRowKeys: this.state.selected_ids,
      onChange: this.onSelectChange,
      hideDefaultSelections: false
    };

    const mappedColumns = tableColumns.map(column => {
      if (column.key === "id") {
        return {
          ...column,
          render: (item, record, index) => <BuildActionOptions id={record.id} onCloneHandler={this.onCloneHandler} onRemoveHandler={this.onRemoveHandler} />
        };
      }
      return column;
    });

    if (this.state.delete_loading || this.state.clone_table_loading || this.state.cloning_table) {
      return <Loader />;
    }

    return (
      <>
        <ServerPaginatedTable
          pageName={"table-configs"}
          method="list"
          uri="config_tables"
          moreFilters={this.state.more_filters}
          partialFilters={this.state.partial_filters}
          columns={mappedColumns}
          hasFilters={false}
          derive={false}
          rowSelection={rowSelection}
          clearSelectedIds={this.clearSelectedIds}
          onBulkDelete={this.onBulkDelete}
          reload={this.state.reload}
          hasDelete={true}
          bulkDeleting={this.state.bulk_deleting}
        />
        <EditCloneModal
          visible={this.state.openEditCloneModel}
          title="Clone Table"
          onOk={this.onOkEditCloneModal}
          onCancel={this.onCancelEditCloneModal}
          nameExists={!!this.state.name_exists}
          searchEvent={this.onSearchEvent}
          confirmButtonText="Clone"
        />
      </>
    );
  }
}

const mapStatetoProps = state => {
  return {
    configTablesListState: configTablesListState(state),
    configTablesGetState: configTablesGetState(state),
    configTablesDeleteState: configTablesDeleteState(state),
    configTablesCreateState: configTableCreateState(state),
    configTablesBulkDeleteState: configTablesBulkDeleteState(state),
    page: getPageSettingsSelector(state)
  };
};

const mapDispatchtoProps = dispatch => {
  return {
    ...mapRestapiDispatchtoProps(dispatch),
    ...mapPageSettingsDispatchToProps(dispatch)
  };
};

export default connect(mapSessionStatetoProps)(
  ErrorWrapper(connect(mapStatetoProps, mapDispatchtoProps)(ConfigTableListPage))
);
