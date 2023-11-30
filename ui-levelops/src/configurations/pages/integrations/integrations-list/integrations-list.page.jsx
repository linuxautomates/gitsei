import { notification } from "antd";
import FileSaver from "file-saver";
import ErrorWrapper from "hoc/errorWrapper";
import { cloneDeep, debounce, get, unset } from "lodash";
import * as PropTypes from "prop-types";
import queryString from "query-string";
import React from "react";
import { connect } from "react-redux";
import {
  resetCreateStoreAction,
  setIntegrationInformationAction,
  setIntegrationsStateAction,
  setIntegrationsStepAction
} from "reduxConfigs/actions/integrationActions";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { integrationIngestionAction } from "reduxConfigs/actions/restapi/ingestion.action";
import { toggleFullscreenModalAction } from "reduxConfigs/actions/ui.actions";
import { mapErrorDispatchtoProps } from "reduxConfigs/maps/errorMap";
import { mapIntegrationDispatchtoProps, mapIntegrationStatetoProps } from "reduxConfigs/maps/integrationMap";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { appendErrorMessagesHelper } from "utils/arrayUtils";
import { getError, getLoading } from "utils/loadingUtils";
import { ServerPaginatedTable } from "../../../../shared-resources/containers";
import { tableColumns } from "../integration-table-config";
import { INTEGRATION_LIST_UUID } from "./constant";
import "./integrations.style.scss";
import IntegrationDeleteSmartButton from "./integration-delete-smart-button/integration-delete-smart-button";
import yaml from "js-yaml";
import { getBaseAPIUrl } from "constants/restUri";
import { useAppStore } from "contexts/AppStoreContext";

export class IntegrationsListPage extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      return: undefined,
      active_key: "your_integrations",
      stepsProgress: {
        type: "inProgress",
        credentials: "toDo",
        information: "toDo",
        status: "toDo",
        teams: "toDo",
        products: "toDo"
      },
      previewItems: [],
      searchTerm: "",
      isEditEnabled: false,
      isRemoveEnabled: false,
      selectedIntegrationId: 0,
      selectedIntegration: undefined,
      create_loading: false,
      status: false,
      integration_id: undefined,
      refetchData: false,
      tags_loading: false,
      list_loading: true,
      tags_select: [],
      reload: 1,
      selected_ids: [],
      bulk_deleting: false,
      integrationLoading: true,
      statusLoadingIndex: 0,
      statusLoading: false,
      integrationIds: []
    };

    this.getStatusIcon = this.getStatusIcon.bind(this);
    this.onSearchByNameHandler = this.onSearchByNameHandler.bind(this);
    this.onFetchFilteredData = this.onFetchFilteredData.bind(this);
    this.debounceSearchHandler = debounce(this.onFetchFilteredData, 300);
    this.onToggleModalHandler = this.onToggleModalHandler.bind(this);
    this.onRemoveIntegrationHandler = this.onRemoveIntegrationHandler.bind(this);
    this.clearSelectedIds = this.clearSelectedIds.bind(this);
    this.onSelectChange = this.onSelectChange.bind(this);
    this.onBulkDelete = this.onBulkDelete.bind(this);
  }

  componentDidMount() {
    if (this.props.location.search) {
      const callbackSearch = queryString.parse(this.props.location.search);
      const callbackStateId = callbackSearch.state;
      const items = callbackStateId ? JSON.parse(sessionStorage.getItem(callbackStateId)) : [];
      if (items && Object.keys(items).length) {
        const keysToKeep = Object.keys(items).filter(key => key !== "state");
        const fieldsToUpdate = {};
        keysToKeep.forEach(key => (fieldsToUpdate[key] = items[key]));
        if (keysToKeep.includes("error")) {
          this.props.resetIntegrationStore();
          this.props.setIntegrationStep(0);
          this.setState(
            state => ({
              stepsProgress: {
                ...state.stepsProgress,
                type: "inProgress",
                credentials: "toDo"
              }
            }),
            () => {
              this.props.toggleFullscreenModal("integrations", true);
              sessionStorage.removeItem(callbackStateId);
            }
          );
        } else {
          this.props.setIntegrationStep(2);
          this.props.setIntegrationState(fieldsToUpdate);
          const preview = [];
          Object.keys(fieldsToUpdate).forEach(key => {
            preview.push({
              label: key,
              value: fieldsToUpdate[key]
            });
          });
          this.setState(
            state => ({
              stepsProgress: {
                ...state.stepsProgress,
                type: "done",
                credentials: "done",
                information: "inProgress"
              },
              previewItems: preview
            }),
            () => {
              this.props.toggleFullscreenModal("integrations", true);
              sessionStorage.removeItem(callbackStateId);
            }
          );
        }
      }
      if (callbackSearch.return) {
        this.setState(
          {
            return: callbackSearch.return.concat(`&product=${callbackSearch.product}`)
          },
          () => this.props.toggleFullscreenModal("integrations", true)
        );
      }
    }
  }

  static getDerivedStateFromProps(props, state) {
    if (state.isRemoveEnabled) {
      const selectedIntegrationId = state.selectedIntegrationId;
      const loading = getLoading(props.rest_api, "integrations", "delete", selectedIntegrationId);
      const error = getError(props.rest_api, "integrations", "delete", selectedIntegrationId);
      let local_state = {};
      if (!loading) {
        if (!error) {
          const data = get(props.rest_api, ["integrations", "delete", selectedIntegrationId, "data"], {});
          const success = get(data, ["success"], true);
          if (!success) {
            notification.error({
              message: data["error"]
            });
          } else {
            props.restapiLoading(true, "integrations", "list", "0");
            local_state = {
              reload: state.reload + 1,
              selected_ids: state.selected_ids.filter(id => id !== selectedIntegrationId)
            };
          }
        }
        return {
          ...local_state,
          isRemoveEnabled: false
        };
      }
    }

    if (state.bulk_deleting) {
      const { loading, error } = get(props.rest_api, ["integrations", "bulkDelete", "0"], {
        loading: true,
        error: true
      });
      if (!loading) {
        if (!error) {
          const data = get(props.rest_api, ["integrations", "bulkDelete", "0", "data", "records"], []);
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

    if (state.integrationLoading) {
      const integrationLoaded = get(props.rest_api, ["integrations", "list", "integration_list", "loading"], true);
      const integrationError = get(props.rest_api, ["integrations", "list", "integration_list", "error"], true);
      if (!integrationLoaded && !integrationError) {
        const records = get(props.rest_api, ["integrations", "list", "integration_list", "data", "records"], []);
        const integrationIds = records.map(item => item.id);
        [...integrationIds].splice(state.statusLoadingIndex, state.statusLoadingIndex + 10).forEach(id => {
          props.integrationIngestionAction(id);
        });
        return {
          integrationLoading: false,
          integrationIds,
          statusLoading: true
        };
      }
    }

    if (!state.integrationLoading) {
      const integrationLoaded = get(props.rest_api, ["integrations", "list", "integration_list", "loading"], true);
      const integrationError = get(props.rest_api, ["integrations", "list", "integration_list", "error"], true);
      if (integrationLoaded && !integrationError) {
        // start the loading again
        return {
          integrationLoading: true,
          statusLoading: true
        };
      }
    }

    if (!state.integrationLoading && state.statusLoading) {
      const ids = state.integrationIds.slice(state.statusLoadingIndex, state.statusLoadingIndex + 10);
      let loading = false;
      ids.forEach(id => {
        const _loading = get(props.rest_api, ["ingestion_integration_status", "get", id, "loading"], true);
        loading = loading || _loading;
      });

      if (!loading) {
        if (state.integrationIds.length > state.statusLoadingIndex) {
          [...state.integrationIds]
            .slice(state.statusLoadingIndex + 10, state.statusLoadingIndex + 20)
            .forEach(id => props.integrationIngestionAction(id));
          return {
            statusLoadingIndex: state.statusLoadingIndex + 10
          };
        } else {
          return {
            statusLoading: false,
            statusLoadingIndex: 0
          };
        }
      }
    }
  }

  componentWillUnmount() {
    this.props.restapiClear("integrations", "list", "0");
    this.props.restapiClear("tags", "bulk", "0");
    this.props.restapiClear("integrations", "bulkDelete", "-1");
  }

  onToggleModalHandler(modal) {
    return () => {
      this.setState(state => ({
        [modal]: !state[modal]
      }));
    };
  }

  onSearchByNameHandler(e) {
    const filters = {
      name: e.target.value
    };
    this.setState({
      searchTerm: e.target.value
    });
    this.debounceSearchHandler(filters);
  }

  onFetchFilteredData(filters) {
    this.setState({
      partial_filters: filters
    });
  }

  onRemoveHandler(id) {
    return () => {
      this.setState(
        {
          isRemoveEnabled: true,
          selectedIntegrationId: id
        },
        () => this.props.removeIntegration(id)
      );
    };
  }

  onRemoveIntegrationHandler() {
    this.props.removeIntegration(this.state.selectedIntegrationId);
  }

  getStatusIcon(status) {
    if (!status || status.toLowerCase() === "error") {
      return { icon: "close", type: "error" };
    }
    return { icon: "check", type: "success" };
  }

  buildActionOptions(props) {
    const action = {
      type: "delete",
      id: props.id,
      description: "Delete",
      onClickEvent: this.onRemoveHandler(props.id)
    };
    return <IntegrationDeleteSmartButton action={action} />;
  }

  generateYML(integration) {
    const config = {
      satellite: {
        tenant: window.isStandaloneApp
          ? localStorage.getItem("levelops_user_org") || ""
          : (accountInfo?.identifier || "").toLowerCase(),
        api_key: "",
        url: getBaseAPIUrl()
      },
      integrations: [
        {
          id: integration.id,
          application: integration.application,
          url: "",
          username: "",
          api_key: ""
        }
      ]
    };
    const ymlString = yaml.dump(config);
    let file = new File([ymlString], `satellite_${integration.id}.yml`, { type: "text/plain;charset=utf-8" });
    FileSaver.saveAs(file);
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
    this.props.integrationsBulkDelete(this.state.selected_ids);
    this.setState({
      bulk_deleting: true
    });
  }

  mapFilters = filters => {
    const _mapped = cloneDeep(filters);
    const applications = get(_mapped, ["filter", "applications"], []);
    if (applications.length === 0) {
      unset(_mapped, ["filter", "applications"]);
      return _mapped;
    }
    if (applications.includes("bitbucket")) {
      return {
        ..._mapped,
        filter: {
          ...(_mapped?.filter || {}),
          applications: [...applications, "bitbucket_server"]
        }
      };
    }
    return _mapped;
  };

  render() {
    const rowSelection = {
      selectedRowKeys: this.state.selected_ids,
      onChange: this.onSelectChange,
      hideDefaultSelections: false
    };

    const mappedColumns = tableColumns.map(column => {
      if (column.dataIndex === "id") {
        return {
          ...column,
          width: 100,
          render: (text, record, index) => this.buildActionOptions(record)
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

    return (
      <>
        <ServerPaginatedTable
          pageName={"integration_table"}
          restCall={"getIntegrations"}
          uri={"integrations"}
          hasFilters
          pageSize={10}
          uuid={INTEGRATION_LIST_UUID}
          hasSearch={true}
          columns={mappedColumns}
          reload={this.state.reload}
          partialFilters={this.state.partial_filters}
          rowSelection={rowSelection}
          clearSelectedIds={this.clearSelectedIds}
          onBulkDelete={this.onBulkDelete}
          hasDelete={true}
          bulkDeleting={this.state.bulk_deleting}
          mapFiltersBeforeCall={this.mapFilters}
        />
      </>
    );
  }
}

IntegrationsListPage.propTypes = {
  className: PropTypes.string,
  isFullScreenModalOn: PropTypes.bool,
  toggleFullscreenModal: PropTypes.func,
  credentials: PropTypes.object,
  information: PropTypes.object,
  step: PropTypes.number,
  setIntegrationState: PropTypes.func,
  setIntegrationStep: PropTypes.func,
  resetIntegrationStore: PropTypes.func,
  removeIntegration: PropTypes.func
};

IntegrationsListPage.defaultProps = {
  className: "integrations-page"
};

export const mapStateToProps = state => ({
  step: state.integrationReducer.toJS().create.step || 0,
  //isFullScreenModalOn: state.ui.getIn(["fullscreenModal", "integrations"]) || false,
  type: state.integrationReducer.toJS().create.type || "",
  state: state.integrationReducer.toJS().create.state || {},
  credentials: state.integrationReducer.toJS().create.credentials || {},
  information: state.integrationReducer.toJS().create.information || {},
  ...mapRestapiStatetoProps(state),
  ...mapIntegrationStatetoProps(state)
});

export const mapDispatchToProps = dispatch => ({
  setIntegrationState: state => dispatch(setIntegrationsStateAction(state)),
  setIntegrationStep: step => dispatch(setIntegrationsStepAction(step)),
  toggleFullscreenModal: (page, isOpen) => dispatch(toggleFullscreenModalAction(page, isOpen)),
  resetIntegrationStore: () => dispatch(resetCreateStoreAction()),
  setInformation: (field, value) => dispatch(setIntegrationInformationAction(field, value)),
  removeIntegration: id => dispatch(actionTypes.integrationsDelete(id)),
  integrationIngestionAction: id => dispatch(integrationIngestionAction(id)),
  ...mapErrorDispatchtoProps(dispatch),
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapIntegrationDispatchtoProps(dispatch)
});

const IntegrationsListPageWrapper = props => {
  const { accountInfo } = useAppStore();
  return <IntegrationsListPage {...props} accountInfo={accountInfo} />;
};

export default ErrorWrapper(connect(mapStateToProps, mapDispatchToProps)(IntegrationsListPageWrapper));
