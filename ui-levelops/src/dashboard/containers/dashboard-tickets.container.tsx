import { notification, Spin } from "antd";
import { RestWidget } from "classes/RestDashboards";
import { PAGERDUTY_REPORT } from "dashboard/constants/applications/names";
import {
  jiraSalesforceSupportedFilters,
  jiraZenDeskSupportedFilters,
  salesForceSupportedFilters,
  supportedFilterType,
  zendeskSupportedFilters
} from "dashboard/constants/supported-filters.constant";
import { GLOBAL_SETTINGS_UUID } from "dashboard/constants/uuid.constants";
import { mapColumnsWithInfo, mapReportColumnWithInfo } from "dashboard/helpers/mapColumnsWithInfo.helper";
import CustomTableUsingSaga from "dashboard/pages/dashboard-drill-down-preview/components/CustomTableUsingSaga";
import { getSupportedCustomFields } from "dashboard/pages/dashboard-drill-down-preview/helper";
import { get, isEqual, set, uniqBy } from "lodash";
import queryString from "query-string";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { connect, useDispatch, useSelector } from "react-redux";
import {
  azureCustomFilterFieldsList,
  AZURE_CUSTOM_FIELDS_LIST,
  configsList,
  DEFAULT_AZURE_CUSTOM_FIELDS_LIST_ID,
  DEFAULT_JIRA_CUSTOM_FIELDS_LIST_ID,
  DEFAULT_ZENDESK_CUSTOM_FIELDS_LIST_ID,
  jiraCustomFilterFieldsList,
  JIRA_CUSTOM_FIELDS_LIST,
  zendeskCustomFilterFieldsList,
  ZENDESK_CUSTOM_FIELDS_LIST,
  DEFAULT_TESTRAILS_CUSTOM_FIELDS_LIST_ID,
  TESTRAILS_CUSTOM_FIELDS_LIST
} from "reduxConfigs/actions/restapi";
import { dashboardWidgetChildrenSelector, getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { transformCompositeWidgetsWithSingleReportToSingleReport } from "utils/widgetUtils";
import { DASHBOARD_ID_KEY, RBAC } from "../../constants/localStorageKeys";
import { JiraSalesforceNodeType, JiraZendeskNodeType } from "../../custom-hooks/helpers/sankey.helper";
import { useApiData } from "../../custom-hooks/useApiData";
import { useSupportedFilters } from "../../custom-hooks/useSupportedFilters";
import { mapPageSettingsDispatchToProps } from "reduxConfigs/maps/pagesettings.map";
import { REST_API_CLEAR_TYPE } from "reduxConfigs/maps/restapi/restapiMap";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { AntText } from "../../shared-resources/components";
import { JiraIssueLink } from "../../shared-resources/components/jira-issue-link/jira-issue-link-component";
import { reportsHavingTicketCategoryDrilldownCol } from "../constants/constants";
import widgetConstants from "../constants/widgetConstants";
import { DashboardTicketsTable } from "../pages";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { useTicketCategorizationFilters } from "custom-hooks";
import { TICKET_CATEGORIZATION_SCHEMES_KEY } from "dashboard/constants/bussiness-alignment-applications/constants";
import { sanitizeObject } from "utils/commonUtils";
import {
  allColumns,
  defaultColumns,
  IM_ADO
} from "dashboard/pages/dashboard-drill-down-preview/drilldownColumnsHelper";
import { selectedDashboardIntegrations } from "reduxConfigs/selectors/integrationSelector";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { getWorkflowProfileByOuAction } from "reduxConfigs/actions/restapi/workflowProfileByOuAction";
import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { testrailsCustomFiledData } from "reduxConfigs/selectors/testrailsSelector";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface StoreProps {
  dashboardsGet: (id: string) => void;
  widgetFilterValuesGet: (uri: string, data: any) => void;
  integrationsList: (filters: any, complete?: any) => void;
  setPageSettings: (path: string, dashboard_settings: any) => void;
  restapiClear: REST_API_CLEAR_TYPE;
  rest_api?: any;
  location: any;
  history: any;
}

const getFiltersFromURI = (encodedURI: string) => {
  let URIDecoded = decodeURIComponent(encodedURI);
  return URIDecoded.match(/filters=(\{.*\})\&/);
};

const DashboardTicketsContainer: React.FC<StoreProps> = (props: StoreProps) => {
  const queries = queryString.parse(props.location.search);
  let jirazendesk: any;
  let jirasalesforce: any;
  const userType = localStorage.getItem(RBAC);

  if (get(queries, "application") === "jirazendesk" && queries && queries.jirazendesk) {
    jirazendesk = JSON.parse(queries.jirazendesk.toString());
  }

  if (get(queries, "application") === "jirasalesforce" && queries && queries.jirasalesforce) {
    jirasalesforce = JSON.parse(queries.jirasalesforce.toString());
  }

  /**
   * queryString.parse(props.location.search) wont work if there is '&' char in the filters;
   * using regex to get the filters
   */
  const _filters = getFiltersFromURI(props.location.search);
  let queryFilters = {};
  if (!!_filters?.length && !!_filters[1]) {
    queryFilters = JSON.parse(_filters[1]);
  }

  const queryAcross = get(queries, "across") || "";
  const queryInterval = get(queries, "interval") || "";
  const queryOUFilters = get(queries, "OUFilter") ? JSON.parse(get(queries, "OUFilter", "") as string) : {};
  const [integrationIds, setIntegrationIds] = useState<Array<string>>([]);
  const [reportType, setReportType] = useState<string>();
  const [filters, setFilters] = useState<any>();
  const [sort, setSort] = useState<any>([]);
  const [reload, setReload] = useState<number>(1);
  const [loadingIntegrationList, setLoadingIntegrationList] = useState<boolean>(true);
  const [jiraFieldsLoading, setJiraFieldsLoading] = useState<boolean>(false);
  const [azureFieldsLoading, setAzureFieldsLoading] = useState<boolean>(false);
  const [zendeskFieldsLoading, setZendeskFieldsLoading] = useState<boolean>(false);
  const integrationsList = useSelector(selectedDashboardIntegrations);
  const dashboard = useParamSelector(getDashboard, { dashboard_id: queries.dashboardId });
  const filterIntegrationId = get(queryFilters, ["filter", "integration_ids"], []);

  const dispatch = useDispatch();

  const [dashboardLoading, dashboardData, dashboardApiError] = useApiData(
    props.rest_api,
    "dashboards",
    "get",
    queries.dashboardId as string
  );
  const [integrationsLoading, integrationsData, integrationsApiError] = useApiData(
    props.rest_api,
    "integrations",
    "list"
  );

  const widgetId = queries.widgetId;
  const dashboardId = queries.dashboardId;

  const integrations = useParamSelector(cachedIntegrationsListSelector, {
    integration_ids: integrationIds
  });

  const queryParamOUArray = get(queryOUFilters, ["ou_ids"], undefined);
  const queryParamOU = queryParamOUArray ? queryParamOUArray[0] : "";

  useEffect(() => {
    dispatch(getWorkflowProfileByOuAction(queryParamOU));
  }, [queryParamOU]);

  const workspaceOuProfilestate = useParamSelector(workflowProfileDetailSelector, { queryParamOU });

  const doraProfileIntegrationType = useMemo(() => {
    const getdoraProfileType = get(widgetConstants, [reportType as string, "getDoraProfileIntegrationType"], undefined);
    if (getdoraProfileType) {
      return getdoraProfileType({ integrations, workspaceOuProfilestate });
    }
  }, [workspaceOuProfilestate, reportType, integrations]);

  const doraProfileIntegrationApplication = useMemo(() => {
    const getdoraProfileApplication = get(
      widgetConstants,
      [reportType as string, "getDoraProfileIntegrationApplication"],
      undefined
    );
    if (getdoraProfileApplication) {
      return getdoraProfileApplication({ workspaceOuProfilestate, reportType: reportType });
    }
  }, [workspaceOuProfilestate, reportType]);

  const doraProfileDeploymentRoute = useMemo(() => {
    const getdoraProfileRoute = get(
      widgetConstants,
      [reportType as string, "getDoraProfileDeploymentRoute"],
      undefined
    );
    if (getdoraProfileRoute) {
      return getdoraProfileRoute({ workspaceOuProfilestate });
    }
  }, [workspaceOuProfilestate, reportType]);

  const doraProfileEvent = useMemo(() => {
    const getdoraProfileRoute = get(widgetConstants, [reportType as string, "getDoraProfileEvent"], undefined);
    if (getdoraProfileRoute) {
      return getdoraProfileRoute({ workspaceOuProfilestate });
    }
  }, [workspaceOuProfilestate, reportType]);

  const _widget = useParamSelector(getWidget, { widget_id: widgetId });
  const children = useParamSelector(dashboardWidgetChildrenSelector, {
    dashboard_id: dashboardId,
    widget_id: _widget?.id
  });

  const jiraFieldsState = useParamSelector(getGenericRestAPISelector, {
    uri: JIRA_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: DEFAULT_JIRA_CUSTOM_FIELDS_LIST_ID
  });

  const azureFieldsState = useParamSelector(getGenericRestAPISelector, {
    uri: AZURE_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: DEFAULT_AZURE_CUSTOM_FIELDS_LIST_ID
  });

  const zendeskFieldsState = useParamSelector(getGenericRestAPISelector, {
    uri: ZENDESK_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: DEFAULT_ZENDESK_CUSTOM_FIELDS_LIST_ID
  });

  const testrailsFieldsState = useParamSelector(getGenericRestAPISelector, {
    uri: TESTRAILS_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: DEFAULT_TESTRAILS_CUSTOM_FIELDS_LIST_ID
  });

  const testrailsCustomField = useSelector(testrailsCustomFiledData);

  const firstChild = children?.[0] || {};

  const widget: RestWidget = useMemo(() => {
    if (_widget?.isComposite && _widget?.children?.length === 1) {
      return transformCompositeWidgetsWithSingleReportToSingleReport(_widget || {}, firstChild);
    }
    return _widget;
  }, [_widget, firstChild]);

  const application =
    queries.application === IntegrationTypes.JIRAZENDESK
      ? jirazendesk && jirazendesk.type
      : queries.application === "jirasalesforce"
      ? jirasalesforce && jirasalesforce.type
      : doraProfileIntegrationType === WorkflowIntegrationType.IM
      ? "jira"
      : doraProfileIntegrationType === IM_ADO
      ? "azure_devops"
      : queries!.application;

  const { categoryColorMapping } = useTicketCategorizationFilters(
    widget?.type,
    [reportType],
    get(filters, ["filter", TICKET_CATEGORIZATION_SCHEMES_KEY], "")
  );

  const getTableConfig = useCallback(
    (key: string, defaultValue = undefined) => {
      return get(widgetConstants, [reportType || "", "drilldown", key], defaultValue);
    },
    [reportType]
  );

  const getSupportedFilters = useMemo(() => {
    let _supportedFilters;

    // * adding reportType, we are setting reportType when the integrations ids are loaded
    if (!!reportType) {
      if (get(queries, "application") === "jirazendesk") {
        const type = jirazendesk.type;
        if (
          [
            JiraZendeskNodeType.JIRA,
            JiraZendeskNodeType.ZENDESK_LIST,
            JiraZendeskNodeType.COMMIT,
            "zendesk_time_across_stages"
          ].includes(type)
        ) {
          _supportedFilters = jiraZenDeskSupportedFilters;
        }
        if (type === JiraZendeskNodeType.ZENDESK) {
          _supportedFilters = zendeskSupportedFilters;
        }
      }
      if (get(queries, "application") === "jirasalesforce") {
        const type = jirasalesforce.type;
        if (
          [
            JiraSalesforceNodeType.JIRA,
            JiraSalesforceNodeType.SALESFORCE_LIST,
            JiraSalesforceNodeType.COMMIT,
            "salesforce_time_across_stages"
          ].includes(type)
        ) {
          _supportedFilters = jiraSalesforceSupportedFilters;
        }
        if (type === JiraSalesforceNodeType.SALESFORCE) {
          _supportedFilters = salesForceSupportedFilters;
        }
      }

      // getting drilldown specific supported filters
      const drilldownSpecificSupportedFilters: supportedFilterType = getTableConfig(
        "drilldownSpecificSupportedFilters"
      );
      if (Object.keys(drilldownSpecificSupportedFilters ?? {}).length) {
        _supportedFilters = drilldownSpecificSupportedFilters;
      }

      const doraDrilldownSpecificSupportedFilters = getTableConfig("doraDrilldownSpecificSupportedFilters");

      if (doraDrilldownSpecificSupportedFilters) {
        _supportedFilters = doraDrilldownSpecificSupportedFilters({ integrationType: doraProfileIntegrationType });
      }

      const cicdDrilldownSpecificSupportedFilters = getTableConfig("cicdDrilldownSpecificSupportedFilters");

      if (cicdDrilldownSpecificSupportedFilters) {
        let integrationData = integrations.filter((data: { id: any }) => filterIntegrationId.includes(data.id));
        _supportedFilters = cicdDrilldownSpecificSupportedFilters({ integrationData });
      }

      if (!_supportedFilters) {
        _supportedFilters = getTableConfig("supported_filters");
      }

      // this adds filters to filter values endpoint api call
      let moreFilters: basicMappingType<any> = {};
      if (reportsHavingTicketCategoryDrilldownCol.includes(reportType as any)) {
        moreFilters = sanitizeObject({
          [TICKET_CATEGORIZATION_SCHEMES_KEY]: get(widget?.query, [TICKET_CATEGORIZATION_SCHEMES_KEY], ""),
          issue_resolved_at: get(widget?.query, ["issue_resolved_at"], undefined)
        });
      }

      if (Object.keys(moreFilters).length) {
        set(_supportedFilters ?? {}, ["moreFilters"], moreFilters);
      }
    }

    return _supportedFilters;
  }, [queries, reportType, widget, getTableConfig, filterIntegrationId, integrations]);

  const { loading: filtersApiLoading, apiData: filtersData } = useSupportedFilters(
    getSupportedFilters,
    integrationIds,
    application as string,
    [integrationIds]
  );

  useEffect(() => {
    if (integrationsData) {
      if (application === IntegrationTypes.JIRA && integrationIds.length) {
        setJiraFieldsLoading(true);
        dispatch(jiraCustomFilterFieldsList({ integration_ids: integrationIds }, DEFAULT_JIRA_CUSTOM_FIELDS_LIST_ID));
      }

      if (application === IntegrationTypes.AZURE && integrationIds.length) {
        setAzureFieldsLoading(true);
        dispatch(azureCustomFilterFieldsList({ integration_ids: integrationIds }, DEFAULT_AZURE_CUSTOM_FIELDS_LIST_ID));
      }
      if (application === IntegrationTypes.ZENDESK) {
        setZendeskFieldsLoading(true);
        dispatch(
          zendeskCustomFilterFieldsList({ integration_ids: integrationIds }, DEFAULT_ZENDESK_CUSTOM_FIELDS_LIST_ID)
        );
      }
    }
  }, [integrationsData, integrationIds]);

  useEffect(() => {
    if (jiraFieldsLoading) {
      const loading = get(jiraFieldsState, "loading", true);
      const error = get(jiraFieldsState, "error", true);
      if (!loading && !error) {
        setJiraFieldsLoading(false);
      }
    }
  }, [jiraFieldsLoading, jiraFieldsState]);

  useEffect(() => {
    if (azureFieldsLoading) {
      const loading = get(azureFieldsState, "loading", true);
      const error = get(azureFieldsState, "error", true);
      if (!loading && !error) {
        setAzureFieldsLoading(false);
      }
    }
  }, [azureFieldsLoading, azureFieldsState]);

  useEffect(() => {
    if (zendeskFieldsLoading) {
      const loading = get(zendeskFieldsState, "loading", true);
      const error = get(zendeskFieldsState, "error", true);
      if (!loading && !error) {
        setZendeskFieldsLoading(false);
      }
    }
  }, [zendeskFieldsLoading, zendeskFieldsState]);

  const supportedCustomFields = useMemo(
    () =>
      getSupportedCustomFields(
        azureFieldsState,
        jiraFieldsState,
        zendeskFieldsState,
        testrailsFieldsState,
        application
      ),
    [azureFieldsState, jiraFieldsState, zendeskFieldsState, application]
  );

  useEffect(() => {
    if (!queries || !queries.dashboardId || !queries.widgetId || !queries.application) {
      notification.error({
        message: "Required Parameters are not available"
      });
    }
  }, []);

  useEffect(() => {
    const dashboardId = queries.dashboardId;
    if (dashboardId) {
      props.dashboardsGet(dashboardId as string);
      dispatch(configsList({}, GLOBAL_SETTINGS_UUID as any));
      const hasAccess = window.isStandaloneApp
        ? getRBACPermission(PermeableMetrics.OPEN_REPORT_SET_DASHBOARD_ID_ACTION)
        : true;
      if (!hasAccess) {
        localStorage.setItem(DASHBOARD_ID_KEY, dashboardId as string);
      }
    }

    return () => {
      if (userType) {
        localStorage.removeItem(DASHBOARD_ID_KEY);
      }
    };
  }, []);

  useEffect(() => {
    if (dashboardData && loadingIntegrationList) {
      const integrationIds = get(dashboardData, ["query", "integration_ids"], []);
      props.integrationsList({ filter: { integration_ids: integrationIds } }, null);
      setLoadingIntegrationList(false);
    }
  }, [dashboardData]);

  useEffect(() => {
    if (!filters && dashboardData && dashboardData.widgets && integrationsData) {
      props.setPageSettings(props.location.pathname, {
        title: dashboardData.name ? dashboardData.name : "Drill-Down"
      });
      let ids: any = [];
      dashboardData.query.integration_ids?.forEach((id: any) => {
        const integration = integrationsData.find((item: any) => item.id === id);
        if (integration) {
          ids.push(id);
        }
      });
      if (widget) {
        const widgetSort = get(widgetConstants, [widget.type, "drilldown", "defaultSort"], undefined);
        const defaultSort = getTableConfig("defaultSort");
        setSort(widgetSort || defaultSort || []);
        if (!isEqual(queryFilters, filters)) {
          queryFilters = {
            ...queryFilters,
            widget_id: widget.id
          };
          setFilters(queryFilters);
        }
        setReportType(widget.type);
        setIntegrationIds(ids);
      }
    }
  }, [dashboardData, integrationsData, widget, queryFilters]);

  useEffect(() => {
    return () => {
      props.restapiClear("dashboards", "get", "-1");
      props.restapiClear("integrations", "list", "-1");
    };
  }, []);

  const getTitle = () => {
    if (get(queries, "application") === "jirazendesk") {
      const type = jirazendesk.type;
      return (type[0] || "").toUpperCase() + type?.substring(1).replace("_list", "") + " Tickets";
    }

    if (get(queries, "application") === "jirasalesforce") {
      const type = jirasalesforce.type;
      return (type[0] || "").toUpperCase() + type?.substring(1).replace("_list", "") + " Tickets";
    }

    return getTableConfig("title");
  };

  const getUri = () => {
    let uri = getTableConfig("uri");
    if (typeof uri === "function") {
      uri = uri({ workspaceOuProfilestate });
    }
    if (application === IntegrationTypes.JIRAZENDESK || application === IntegrationTypes.JIRA_SALES_FORCE) {
      const uriForNodeTypes = getTableConfig("uriForNodeTypes");
      if (uriForNodeTypes) {
        const app = JSON.parse(get(queries, [application as any], "{}") as any);
        uri = get(uriForNodeTypes, app.type, "");
      }
    }

    if (reportType === PAGERDUTY_REPORT.RESPONSE_REPORTS) {
      const issueType = get(filters, ["filter", "issue_type"], "incident");
      if (issueType === "alert") {
        return getTableConfig("alertUri");
      }
    }
    return uri;
  };

  /** getting table records */
  const tableRecords = useParamSelector(getGenericRestAPISelector, {
    uuid: "jira-tickets",
    method: "list",
    uri: getUri()
  });

  const getJiraTicketLink = (item: any, record: any) => {
    let _baseUrl = `ticket_details?key=${record.key}&`;
    if (userType !== "ADMIN") {
      _baseUrl = `${_baseUrl}dashboardId=${queries.dashboardId}&`;
    }
    let integrationId = get(record, ["integration_id"], undefined);
    if (!integrationId) {
      integrationId = get(dashboardData, ["query", "integration_ids"], []).toString();
    }
    _baseUrl = `${_baseUrl}integration_id=${integrationId}`;
    return <JiraIssueLink link={_baseUrl} ticketKey={item} integrationUrl={record?.integration_url} />;
  };

  const getColumns = () => {
    const allColumnsProps = {
      integrationsData,
      filters,
      reportType,
      queries,
      widget,
      dashboard,
      categoryColorMapping,
      filtersData,
      doraProfileIntegrationType,
      doraProfileDeploymentRoute,
      doraProfileEvent,
      doraProfileIntegrationApplication,
      integrations,
      filterIntegrationId,
      testrailsCustomField
    };
    const _columns = allColumns(allColumnsProps);

    return uniqBy(_columns, "dataIndex");
  };

  const handleFiltersChange = (data: any) => {
    const nApplication = application!.includes("github") ? "github" : "jira";

    let ids: any = [];
    if (data.integration_ids && data.integration_ids.length > 0) {
      data.integration_ids.forEach((id: any) => {
        const integration = integrationsData.find((item: any) => item.id === id);
        if (integration && integration.application === nApplication) {
          ids.push(id);
        }
      });
    }
    if (ids.length) {
      const newFilters = {
        ...filters,
        filter: {
          ...data,
          integration_ids: ids
        }
      };
      setIntegrationIds(ids);
      setFilters(newFilters);
    }
  };

  const getCustomFields = () => {
    if (filtersData) {
      const fields = filtersData.find((item: any) => Object.keys(item)[0] === "custom_fields");
      if (fields && fields.hasOwnProperty("custom_fields")) {
        return fields.custom_fields;
      } else return [];
    } else return [];
  };

  const isCustomTableData = () => {
    return application === "zendesk_time_across_stages" || application === "salesforce_time_across_stages";
  };

  //If you are adding new report please follow columnWithInformation approach rather then columnsWithInfo
  const mappedColumns = () => {
    const columns = getColumns().map((column: any) => {
      if (column.filterField === "hygiene_types") {
        return {
          ...column,
          options: column.options?.map((option: any) => {
            if (typeof option === "string") {
              return {
                label: option.replace(/_/g, " "),
                value: option
              };
            }
            return option;
          })
        };
      }
      return column;
    });

    const reportscolumnsinfo = get(widgetConstants, [reportType || "", "columnWithInformation"], false);

    if (reportscolumnsinfo) {
      const columnsWithInfo = get(widgetConstants, [reportType || "", "columnsWithInfo"], {});
      return mapReportColumnWithInfo(columns, columnsWithInfo);
    }
    const columnsWithInfo = getTableConfig("columnsWithInfo");

    return mapColumnsWithInfo(columns, columnsWithInfo);
  };

  const getVisibleColumns = () => {
    const allColumns = mappedColumns();
    const defaultColumnsProps = {
      drillDownProps: {},
      filters,
      across: get(filters, ["across"], ""),
      integrationsList,
      widget,
      dashboard,
      categoryColorMapping,
      supportedCustomFields,
      zendeskFieldsState,
      doraProfileIntegrationType,
      doraProfileDeploymentRoute,
      doraProfileIntegrationApplication,
      testrailsCustomField
    };
    const _columns = defaultColumns(defaultColumnsProps);

    const defaultColumnsDataIndex = _columns.map((column: any) => column.dataIndex);

    const visibleColumns = allColumns.filter((column: any) =>
      defaultColumnsDataIndex.find((dataIndex: string) => dataIndex === column.dataIndex)
    );

    return visibleColumns;
  };

  const handleToggleFilterChange = (filters: any) => {
    setFilters(filters);
  };

  return (
    <>
      {!queries || !queries.dashboardId || !queries.widgetId || !queries.application ? (
        <div style={{ display: "flex", justifyContent: "center" }}>
          <AntText>Required Parameters are not available</AntText>
        </div>
      ) : (
        <>
          {dashboardLoading || filtersApiLoading || jiraFieldsLoading || azureFieldsLoading || zendeskFieldsLoading ? (
            <div style={{ display: "flex", justifyContent: "center" }}>
              <Spin />
            </div>
          ) : (
            <>
              {reportType && filters && (
                <>
                  {isCustomTableData() ? (
                    <CustomTableUsingSaga
                      slicedColumns={false}
                      filters={filters}
                      report={(queries?.application as string) || ""}
                    />
                  ) : (
                    <>
                      {integrationsData && filtersData && (
                        <DashboardTicketsTable
                          reportType={reportType}
                          filters={filters || {}}
                          integrationIds={integrationIds!}
                          integrationsData={integrationsData}
                          filtersData={filtersData}
                          sortData={sort}
                          customFields={getCustomFields()}
                          columns={getVisibleColumns()}
                          supportedFilters={getSupportedFilters}
                          title={getTitle()}
                          uri={getUri()}
                          setFilters={(data: any) => handleFiltersChange(data)}
                          ouFilters={queryOUFilters}
                          interval={queryInterval}
                          across={queryAcross}
                          drillDownColumns={widget?.drilldown_columns}
                          allColumns={mappedColumns()}
                          doraProfileIntegrationType={doraProfileIntegrationType || ""}
                          doraProfileDeploymentRoute={doraProfileDeploymentRoute}
                          reload={reload}
                          setReload={setReload}
                          widgetMetadata={widget?.metadata ?? {}}
                          widgetId={widgetId as string}
                          handleToggleFilterChange={handleToggleFilterChange}
                          doraProfileEvent={doraProfileEvent}
                          doraProfileIntegrationApplication={doraProfileIntegrationApplication}
                          testrailsCustomField={testrailsCustomField}
                        />
                      )}
                    </>
                  )}
                </>
              )}
            </>
          )}
        </>
      )}
    </>
  );
};

const mapDispatchToProps = (dispatch: any) => ({
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapPageSettingsDispatchToProps(dispatch)
});

export default connect(mapRestapiStatetoProps, mapDispatchToProps)(DashboardTicketsContainer);
