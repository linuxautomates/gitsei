import { Col, Row } from "antd";
import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import { DASHBOARD_ROUTES, getBaseUrl } from "constants/routePaths";
import {
  azureRenderVelocityDynamicColumns,
  renderVelocityDynamicColumns,
  renderVelocityStageDynamicColumns
} from "custom-hooks/helpers/leadTime.helper";
import { customFieldFiltersSanitize } from "custom-hooks/helpers/zendeskCustomFieldsFiltersTransformer";
import { githubAppendAcrossOptions } from "dashboard/constants/applications/constant";
import { TICKET_CATEGORIZATION_SCHEMES_KEY } from "dashboard/constants/bussiness-alignment-applications/constants";
import widgetConstants, { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { DEFAULT_SCM_SETTINGS_OPTIONS } from "dashboard/constants/defaultFilterOptions";
import { ReportsApplicationType } from "dashboard/constants/helper";
import { GLOBAL_SETTINGS_UUID } from "dashboard/constants/uuid.constants";
import { mapColumnsWithInfo } from "dashboard/helpers/mapColumnsWithInfo.helper";
import { LEVELOPS_REPORTS } from "dashboard/reports/levelops/constant";
import { capitalize, cloneDeep, get, isArray, uniqBy, unset } from "lodash";
import React, { isValidElement, useContext, useEffect, useMemo, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  AZURE_CUSTOM_FIELDS_LIST,
  JIRA_CUSTOM_FIELDS_LIST,
  TESTRAILS_CUSTOM_FIELDS_LIST,
  ZENDESK_CUSTOM_FIELDS_LIST,
  widgetDrilldownColumnsUpdate
} from "reduxConfigs/actions/restapi";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { ticketCategorizationSchemesRestGetSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { getDrillDownWidget } from "reduxConfigs/selectors/widgetSelector";
import { sanitizeObject } from "utils/commonUtils";
import { transformFiltersZendesk } from "utils/dashboardFilterUtils";
import { toTitleCase } from "utils/stringUtils";
import { RestDashboard, RestWidget } from "../../../classes/RestDashboards";
import { dashboardWidgetChildrenSelector } from "reduxConfigs/selectors/dashboardSelector";
import { AntCard } from "../../../shared-resources/components";
import { ServerPaginatedTable } from "../../../shared-resources/containers";
import { transformCompositeWidgetsWithSingleReportToSingleReport } from "../../../utils/widgetUtils";
import {
  AZURE_SPRINT_REPORTS,
  azureLeadTimeIssueReports,
  INCLUDE_INTERVAL_IN_PAYLOAD,
  ISSUE_MANAGEMENT_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  JIRA_SPRINT_REPORTS,
  LEAD_TIME_REPORTS,
  leadTimeReports,
  PAGERDUTY_REPORT,
  SCM_ADDITIONAL_KEYS_APPLICATIONS,
  SPRINT,
  SPRINT_JIRA_ISSUE_KEYS,
  SCM_REPORTS,
  AZURE_LEAD_TIME_ISSUE_REPORT,
  LEAD_TIME_BY_STAGE_REPORTS,
  DORA_REPORTS
} from "../../constants/applications/names";
import { csvDrilldownDataTransformer } from "../../helpers/csv-transformers/csvDrilldownDataTransformer";
import { sprintMetricStatCsvTransformer } from "../../helpers/csv-transformers/sprintMetricStatCSVTransformer";
import CustomTableUsingSaga from "./components/CustomTableUsingSaga";
import "./DashboardDrillDownPreview.style.scss";
import {
  buildDrillDownFilters,
  DrillDownProps,
  DRILLDOWN_UUID,
  getAcross,
  getCategoryColorMapping,
  integrationDeriveSupport,
  getSupportedCustomFields
} from "./helper";
import queryString from "query-string";
import { useLocation, useParams } from "react-router-dom";
import LevelopsTableReportDrilldownComponent from "./components/levelops-table-report-drilldown-component/LevelopsTableReportDrilldownComponent";
import DevRawStatsDrilldownWrapper from "./components/dev-raw-stats-drilldown-component/DevRawStatsDrilldownWrapper";
import { levelopsTableReportOpenReportHelper } from "./components/levelops-table-report-drilldown-component/helper";
import { WidgetFilterContext } from "../context";
import DrillDownFilterContent from "shared-resources/containers/server-paginated-table/components/drilldown-filter-content/drilldown-filter-content";
import { defaultColumns } from "./drilldownColumnsHelper";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import cx from "classnames";
import { ColumnProps } from "antd/lib/table";
import { ExtraColumnProps, ReportDrilldownColTransFuncType } from "dashboard/dashboard-types/common-types";
import { testrailsCustomFiledData } from "reduxConfigs/selectors/testrailsSelector";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { ProjectPathProps } from "classes/routeInterface";
import { valuesToFilters } from "dashboard/constants/constants";
import { useParentProvider } from "contexts/ParentProvider";
import { removeLastSlash } from "utils/regexUtils";

interface DashboardDrillDownPreviewProps {
  drillDownProps?: DrillDownProps;
  onDrilldownClose: () => void;
}

const DashboardDrillDownPreview: React.FC<DashboardDrillDownPreviewProps> = (props: DashboardDrillDownPreviewProps) => {
  const { drillDownProps, onDrilldownClose } = props;
  const { filters: contextFilters } = useContext(WidgetFilterContext);
  const [filters, setFilters] = useState<any>({});
  const [across, setAcross] = useState<string>("");
  const [sort, setSort] = useState([]);
  const [reload, setReload] = useState<number>(0);
  const [filterRatings, setFilterRatings] = useState<any>([]);
  const widgetId = drillDownProps?.widgetId;
  const widgetContextFilters = get(contextFilters, [widgetId as string], {});
  const projectParams = useParams<ProjectPathProps>();

  const dashboard: RestDashboard | null = useSelector(selectedDashboard);
  const _widget = useParamSelector(getDrillDownWidget, { widget_id: drillDownProps!.widgetId });
  const children = useParamSelector(dashboardWidgetChildrenSelector, {
    dashboard_id: drillDownProps!.dashboardId,
    widget_id: _widget.id
  });

  const integrationIds = useMemo(() => {
    return get(dashboard, ["query", "integration_ids"], []);
  }, [dashboard?.query]);

  const integrations = useParamSelector(cachedIntegrationsListSelector, {
    integration_ids: integrationIds
  });

  const integrationKey = useMemo(() => (integrationIds.length ? integrationIds.sort().join("_") : "0"), [integrations]);

  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const firstChild = children[0];

  const widget: RestWidget = useMemo(() => {
    if (_widget.isComposite && _widget.children.length === 1) {
      return transformCompositeWidgetsWithSingleReportToSingleReport(_widget, firstChild);
    }
    return _widget;
  }, [_widget, firstChild]);
  const [selectedColumns, setSelectedColumns] = useState(widget.drilldown_columns);

  const application = get(widgetConstants, [widget.type, "application"], "");
  const mapFiltersBeforeCall = get(widgetConstants, [widget.type, "mapFiltersBeforeCall"], undefined);
  const effortUnitFilter = get(_widget, ["_query", "uri_unit"], "");
  const isCommitCountFilter = effortUnitFilter === "commit_count_fte";
  const currentAllocation = get(drillDownProps, [application, "additional_data", "current_allocation"], false);
  const isActive = get(drillDownProps, [application, "isActive"], false);
  const widgetMetaData = useMemo(() => get(widget, ["metadata"], {}), [_widget, firstChild]);
  const drilldownNotSupportedCondition = isCommitCountFilter || currentAllocation || isActive;
  const drilldownMissingAndOtherRatings = get(
    widgetConstants,
    [widget.type, "drilldownMissingAndOtherRatings"],
    undefined
  );
  const isLeadTimeByStage = [
    LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_STAGE_REPORT,
    LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_BY_STAGE_REPORT,
    LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT
  ].includes(widget.type);
  const {
    utils: { getLocationPathName }
  } = useParentProvider();

  const report = useMemo(() => widget.type, [widget]);

  const workspaceOuProfilestate = useParamSelector(workflowProfileDetailSelector, { queryParamOU });

  const jiraFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: JIRA_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey
  });

  const azureFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: AZURE_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey
  });

  const zendeskFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: ZENDESK_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey
  });

  const globalSettingsState = useParamSelector(getGenericRestAPISelector, {
    uri: "configs",
    method: "list",
    uuid: GLOBAL_SETTINGS_UUID
  });

  const testrailsFieldsSelector = useParamSelector(getGenericRestAPISelector, {
    uri: TESTRAILS_CUSTOM_FIELDS_LIST,
    method: "list",
    uuid: integrationKey
  });

  const testrailsCustomField = useSelector(testrailsCustomFiledData);

  const doraProfileIntegrationType = useMemo(() => {
    const getdoraProfileType = get(
      widgetConstants,
      [widget.type as string, "getDoraProfileIntegrationType"],
      undefined
    );
    if (getdoraProfileType) {
      return getdoraProfileType({ integrations, workspaceOuProfilestate });
    }
  }, [workspaceOuProfilestate, widget.type, integrations]);

  const doraProfileDeploymentRoute = useMemo(() => {
    const getdoraProfileRoute = get(
      widgetConstants,
      [widget.type as string, "getDoraProfileDeploymentRoute"],
      undefined
    );
    if (getdoraProfileRoute) {
      return getdoraProfileRoute({ workspaceOuProfilestate });
    }
  }, [workspaceOuProfilestate, widget.type]);

  const doraProfileIntegrationApplication = useMemo(() => {
    const getdoraProfileApplication = get(
      widgetConstants,
      [widget.type as string, "getDoraProfileIntegrationApplication"],
      undefined
    );
    if (getdoraProfileApplication) {
      return getdoraProfileApplication({ workspaceOuProfilestate, reportType: widget.type });
    }
  }, [workspaceOuProfilestate, widget.type]);

  const doraProfileEvent = useMemo(() => {
    const getdoraProfileRoute = get(widgetConstants, [widget.type as string, "getDoraProfileEvent"], undefined);
    if (getdoraProfileRoute) {
      return getdoraProfileRoute({ workspaceOuProfilestate });
    }
  }, [workspaceOuProfilestate, widget.type]);

  /** getting selected profile for categoryColorMapping */
  const profile: RestTicketCategorizationScheme = useParamSelector(ticketCategorizationSchemesRestGetSelector, {
    scheme_id: get(filters, ["filter", TICKET_CATEGORIZATION_SCHEMES_KEY], "")
  });
  const categoryColorMapping = useMemo(() => {
    let categoriesColorMapping = getCategoryColorMapping(profile?.categories ?? []);
    categoriesColorMapping["Other"] = profile?.uncategorized_color;
    return categoriesColorMapping;
  }, [profile]);

  const scmGlobalSettings = useMemo(() => {
    const SCM_GLOBAL_SETTING = globalSettingsState?.data?.records.find(
      (item: any) => item.name === "SCM_GLOBAL_SETTINGS"
    );
    return SCM_GLOBAL_SETTING
      ? typeof SCM_GLOBAL_SETTING?.value === "string"
        ? JSON.parse(SCM_GLOBAL_SETTING?.value)
        : SCM_GLOBAL_SETTING?.value
      : DEFAULT_SCM_SETTINGS_OPTIONS;
  }, [globalSettingsState]);

  const supportedCustomFields = useMemo(() => {
    let _application = application;
    if (_application === "any") {
      _application = doraProfileIntegrationApplication;
    }
    return getSupportedCustomFields(
      azureFieldsSelector,
      jiraFieldsSelector,
      zendeskFieldsSelector,
      testrailsFieldsSelector,
      _application
    );
  }, [
    azureFieldsSelector,
    jiraFieldsSelector,
    widget,
    zendeskFieldsSelector,
    doraProfileIntegrationApplication,
    testrailsFieldsSelector
  ]);

  useEffect(() => {
    if (drillDownProps && dashboard && widget) {
      if (widget) {
        let widgetSort = get(widgetConstants, [widget.type, "drilldown", "defaultSort"], undefined);
        const defaultSort = getTableConfig("defaultSort");
        let { acrossValue, filters } = buildDrillDownFilters(
          {
            ...drillDownProps,
            supportedCustomFields,
            scmGlobalSettings: { ...(scmGlobalSettings || {}) },
            availableIntegrations: integrations,
            doraProfileIntegrationType
          },
          cloneDeep(widget),
          cloneDeep(dashboard.query || {}),
          widgetMetaData,
          get(dashboard, ["_metadata"], {}),
          queryParamOU,
          widgetContextFilters
        );

        const overrideFilterWithStackFilter = getWidgetConstant(widget.type, ["overrideFilterWithStackFilter"], null);
        if (overrideFilterWithStackFilter) {
          filters = overrideFilterWithStackFilter(filters, drillDownProps);
        }

        const application = get(widgetConstants, [widget.type, "application"], "");
        if (application === ReportsApplicationType.ZENDESK) {
          filters = customFieldFiltersSanitize(filters, false);
        }
        if (application === ReportsApplicationType.JIRA_ZENDESK) {
          const { jiraCustomFields, zendeskCustomFields } = transformFiltersZendesk(filters?.filter);
          const customFields = get(filters, ["filter", "custom_fields"], {});
          if (Object.keys(customFields).length > 0) {
            filters = {
              ...(filters || {}),
              filter: {
                ...(filters?.filter || {}),
                custom_fields: {
                  ...jiraCustomFields,
                  ...zendeskCustomFields
                }
              }
            };
          }
        }

        if (isLeadTimeByStage) {
          delete filters?.filter?.value_stage_names;
        }
        if (typeof widgetSort === "function") {
          widgetSort = widgetSort(filters.filter?.histogram_stage_name);
          filters.sort = widgetSort;
        }
        if (LEAD_TIME_BY_STAGE_REPORTS.includes(report as any)) {
          widgetSort = undefined;
        }
        setSort(widgetSort || defaultSort || []);
        setFilters(filters);
        setAcross(acrossValue);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [drillDownProps, drillDownProps?.x_axis]);

  //force reload whenever widget id changes
  useEffect(() => {
    setReload((prev: number) => ++prev);
    setSelectedColumns(widget.drilldown_columns);
  }, [widgetId]);

  useEffect(() => {
    return () => {
      setFilters(undefined);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [drillDownProps]);

  useEffect(() => {
    if (filters?.filter?.ratings) setFilters({ ...filters, filter: { ...filters.filter, ratings: filterRatings } });
  }, [filterRatings]);

  const getReportURLfromConstants = () => {
    if (widget) {
      return get(widgetConstants, [widget.type, "reportURL"], () => "");
    }
  };

  const getFiltersForReportURL = () => {
    const filterCompute = buildDrillDownFilters(
      drillDownProps!,
      widget,
      dashboard?.query,
      drillDownProps?.widgetMetaData,
      get(dashboard, ["_metadata"], {}),
      queryParamOU,
      widgetContextFilters
    );

    const filters = get(filterCompute, ["filters", "filter"], {});
    return sanitizeObject(filters);
  };

  const getInterval = () => {
    const includeIntervalInPayload = get(widgetConstants, [widget.type, INCLUDE_INTERVAL_IN_PAYLOAD], false);
    if (includeIntervalInPayload) {
      return get(filters, ["interval"], undefined);
    }
    return undefined;
  };

  const getOUFilters = () => {
    const ou_ids = filters?.ou_ids || [];
    if (ou_ids.length) {
      let _item: any = {
        ou_ids
      };

      if (Object.keys(filters?.ou_user_filter_designation || {}).length) {
        _item["ou_user_filter_designation"] = filters?.ou_user_filter_designation;
      }

      if ((filters?.ou_exclusions || []).length) {
        _item["ou_exclusions"] = filters?.ou_exclusions;
      }
      if (filters?.hasOwnProperty("apply_ou_on_velocity_report")) {
        _item.apply_ou_on_velocity_report = filters.apply_ou_on_velocity_report;
      }
      return _item;
    }
  };

  const handleLoadTickets = () => {
    const { application, widgetId, x_axis } = drillDownProps!;
    const acrossValue = getAcross(across, widget);
    const interval = getInterval();
    const OUFilters = getOUFilters();
    const xAxis = typeof x_axis === "object" ? x_axis.hygiene || JSON.stringify(x_axis) : x_axis;
    let url = `${getBaseUrl(projectParams)}${DASHBOARD_ROUTES.DRILL_DOWN}?application=${application}&dashboardId=${
      dashboard?.id
    }&widgetId=${widgetId}&x=${xAxis}&filters=${JSON.stringify(filters)}&across=${acrossValue}`;
    if (interval) {
      url = `${url}&interval=${interval}`;
    }
    if (OUFilters || Object.keys(OUFilters || {}).length) {
      url = `${url}&OUFilter=${JSON.stringify(OUFilters)}`;
    }
    if (application === IntegrationTypes.JIRAZENDESK) {
      let jirazendeskdata = get(drillDownProps, [IntegrationTypes.JIRAZENDESK], {});
      url = `${url}&jirazendesk=${JSON.stringify(jirazendeskdata)}`;
    }
    if (application === IntegrationTypes.JIRA_SALES_FORCE) {
      let jirasalesforcedata = get(drillDownProps, [IntegrationTypes.JIRA_SALES_FORCE], {});
      url = `${url}&jirasalesforce=${JSON.stringify(jirasalesforcedata)}`;
    }
    if (application.includes("levelops") && widget.type !== "levelops_assessment_response_time__table_report") {
      const reportURL = getReportURLfromConstants();
      if (reportURL?.().length) {
        const filters = getFiltersForReportURL();
        url = reportURL();
        if (application.includes("assessment")) {
          url = `${url}&filters=${JSON.stringify(filters)}&searchType=equal`;
        } else {
          url = `${url}?filters=${JSON.stringify(filters)}&searchType=equal`;
        }
      }
    }

    if (widget?.type === LEVELOPS_REPORTS.TABLE_REPORT) {
      url = levelopsTableReportOpenReportHelper({
        widgetMetadata: widgetMetaData,
        dashboardMetadata: get(dashboard, ["_metadata"], {}),
        filters: filters?.filter ?? {},
        ouId: queryParamOU
      });
    }
    window.open(`${removeLastSlash(getLocationPathName?.())}${url}`);
  };

  const getTableConfig = (key: string, defaultValue?: any) => {
    return get(widgetConstants, [report, "drilldown", key], defaultValue);
  };

  const getUri = () => {
    const application = get(drillDownProps, "application", "");
    let uri = getTableConfig("uri");
    if (typeof uri === "function") {
      uri = uri({ workspaceOuProfilestate });
    }
    if (["jirazendesk", "jirasalesforce"].includes(application)) {
      const uriForNodeTypes = getTableConfig("uriForNodeTypes");
      if (uriForNodeTypes) {
        const type = get(drillDownProps, [application, "type"], "");
        uri = get(uriForNodeTypes, type, "");
      }
    }

    if (report === PAGERDUTY_REPORT.RESPONSE_REPORTS) {
      const issueType = get(filters, ["filter", "issue_type"], "incident");
      if (issueType === "alert") {
        return getTableConfig("alertUri");
      }
    }

    return uri;
  };

  const scrollX = useMemo(() => {
    return { x: "fit-content" };
  }, []);

  const extraDrilldownProps: any = useMemo(() => {
    let props = {};

    const getExtraDrilldownProps = getTableConfig("getExtraDrilldownProps");
    if (getExtraDrilldownProps) {
      const xAxis: any = get(drillDownProps, ["x_axis"], undefined);
      return {
        ...getExtraDrilldownProps({ widgetId, xAxis, drillDownProps, doraProfileIntegrationType, doraProfileEvent })
      };
    }

    if ([...leadTimeReports, ...azureLeadTimeIssueReports].includes(report as any)) {
      const xAxis: any = get(drillDownProps, ["x_axis"], undefined);
      let activeKey = typeof xAxis === "string" ? xAxis : xAxis?.stageName;
      const recordsTransformer = get(widgetConstants, [report, "drilldown", "transformRecords"]);
      const dynamicColumnRenderer = get(widgetConstants, [report, "drilldown", "renderDynamicColumns"]);
      const totalColName = get(widgetConstants, [report, "drilldownTotalColCaseChange"]);
      if (activeKey === "Total" && totalColName) activeKey = "total";

      if (activeKey) {
        props = {
          ...props,
          activeColumn: activeKey,
          widgetId: widgetId
        };
      }

      const renderColumn =
        dynamicColumnRenderer ??
        (LEAD_TIME_BY_STAGE_REPORTS.includes(report as any)
          ? renderVelocityStageDynamicColumns
          : azureLeadTimeIssueReports.includes(report as any)
          ? azureRenderVelocityDynamicColumns
          : renderVelocityDynamicColumns);

      props = {
        ...props,
        hasDynamicColumns: true,
        renderDynamicColumns: renderColumn,
        transformRecordsData: recordsTransformer,
        scroll: scrollX,
        widgetId: widgetId,
        shouldDerive: ["velocity_config", "integration_id"]
      };
    }

    if (
      [
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_TREND
      ].includes(report as JIRA_SPRINT_REPORTS)
    ) {
      props = {
        shouldDerive: across === SPRINT ? [SPRINT_JIRA_ISSUE_KEYS] : [],
        report
      };
    }
    return {
      ...props,
      widgetId: widgetId
    };
  }, [across, report, drillDownProps]);

  const getLevelopsWidgetsDrillDownTitle = () => {
    const { x_axis } = drillDownProps!;
    if (typeof x_axis === "object" && Object.keys(x_axis).length) {
      return x_axis.name || "";
    }

    if (report === LEVELOPS_REPORTS.TABLE_REPORT) return x_axis;

    return "";
  };
  const showTitle = () => {
    const getShowTitle = get(widgetConstants, [widget.type, "getShowTitle"], undefined);
    if (getShowTitle) {
      return getShowTitle({ drillDownProps });
    }
    return !!report;
  };
  const getDrillDownTitle = () => {
    const xAxis = get(drillDownProps, ["x_axis"], undefined);
    const application = get(drillDownProps, ["application"], "");
    const getDrilldownTitle = get(widgetConstants, [widget.type as string, "getDrilldownTitle"], undefined);

    /**
     * This is the newer/better way of setting and getting the drilldown titles
     * set it under the report definition as getDrilldownTitle
     */
    if (getDrilldownTitle && typeof getDrilldownTitle === "function") {
      return getDrilldownTitle({ xAxis, drillDownProps });
    }

    if (application.includes("levelops")) {
      return getLevelopsWidgetsDrillDownTitle();
    } else if (
      [...SCM_ADDITIONAL_KEYS_APPLICATIONS, "scm_review_collaboration"].includes(application) &&
      ![
        AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_TIME_TREND_REPORT,
        AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_TIME_BY_TYPE_REPORT,
        JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT,
        LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT
      ].includes(report)
    ) {
      return get(drillDownProps, "drillDownTitle", "");
    } else if (
      [
        ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_REPORT,
        JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_REPORT,
        PAGERDUTY_REPORT.RESPONSE_REPORTS
      ].includes(report as any)
    ) {
      return get(drillDownProps, "drillDownTitle", "");
    } else if (xAxis && typeof xAxis === "object") {
      if (xAxis.hygiene && typeof xAxis.hygiene === "string") {
        return xAxis.hygiene.replace(/_/g, " ");
      }

      if (xAxis.taskType) {
        return xAxis.taskType.replace(/_/g, " ");
      }

      if (xAxis.label) {
        return xAxis.label;
      }

      return xAxis.name;
    } else if (["zendesk_time_across_stages", "salesforce_time_across_stages"].includes(application)) {
      return application.replace(/_/g, " ");
    } else if (["zendesk_top_customers_report", "salesforce_top_customers_report"].includes(report || "")) {
      return report?.replace(/_/g, " ");
    } else if (["azure_devops"].includes(application)) {
      return xAxis.replace(/_/g, " ").split("\\").pop();
    } else if (typeof xAxis === "string") {
      return xAxis.replace(/_/g, " ");
    } else return "";
  };

  const getDerive = () => {
    const application = get(drillDownProps, ["application"], "");
    if (widget && integrationDeriveSupport.includes(widget.type)) {
      return ["integration_id"];
    }

    if (
      [
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_TREND
      ].includes(widget.type) &&
      across === SPRINT
    ) {
      return true;
    }
    return application.includes("levelops")
      ? true
      : application.includes("jira") || application.includes("azure_devops")
      ? ["integration_id", "custom_fields"]
      : false;
  };

  const mappedColumns = () => {
    let columns = defaultColumns({
      drillDownProps,
      filters,
      across,
      integrations,
      widget,
      dashboard,
      categoryColorMapping,
      supportedCustomFields,
      zendeskFieldsSelector,
      doraProfileIntegrationType,
      doraProfileDeploymentRoute,
      doraProfileEvent,
      doraProfileIntegrationApplication,
      testrailsCustomField
    });

    const reportscolumnsinfo = get(widgetConstants, [report || "", "columnWithInformation"], false);
    let columnsWithInfo = getTableConfig("columnsWithInfo");
    if (reportscolumnsinfo) {
      columnsWithInfo = get(widgetConstants, [report || "", "columnsWithInfo"], {});
    }
    return mapColumnsWithInfo(columns, columnsWithInfo);
  };

  const getDrillDownType = () => {
    if (!across && report === SCM_REPORTS.SCM_REVIEW_COLLABORATION_REPORT) {
      const isCreator = get(filters, ["filter", "creators"], undefined);
      if (isCreator) {
        return "Committer";
      }
      return "Reviewer";
    }

    const getType = get(widgetConstants, [report, "getDrillDownType"], undefined);
    if (getType) {
      return getType({ across, drillDownProps });
    }

    if (!across) {
      return;
    }

    if (across && across?.includes("customfield_")) {
      return;
    }

    if (across && across?.includes("Custom.")) {
      return across.split(".").pop();
    }

    if (report === "tickets_report" && across === "parent") {
      return "Ticket";
    }

    if (report === "scm_issues_time_across_stages_report" && across == "column") {
      return "Historical Status";
    }

    if (report === LEVELOPS_REPORTS.ASSESSMENT_RESPONSE_TIME_TABLE_REPORT && across === "questionnaire_template_id") {
      return "Assessment";
    }

    if (report === "review_collaboration_report") {
      const data = drillDownProps?.x_axis;
      unset(data, "name");
      return toTitleCase(Object.keys(data)[0]);
    }

    const accrossMappingValue = githubAppendAcrossOptions.find((option: any) => {
      return option.value === across;
    });
    if (accrossMappingValue) {
      return accrossMappingValue.label;
    }

    return report.includes("trend") ? "Trend" : capitalize(across?.replace(/_/g, " "));
  };

  const getJsxHeaders = () => {
    const columns = mappedColumns();
    let jsxHeaders: any = [];
    columns.forEach((col: any) => {
      if (isValidElement(col?.title) && !col?.hidden) {
        let jsxTitle = col?.titleForCSV;
        jsxHeaders.push({
          title: jsxTitle ? jsxTitle : capitalize(col?.dataIndex?.replace(/_/g, " ")),
          key: col?.dataIndex
        });
      }
    });
    return jsxHeaders;
  };

  const getCSVDataTransformer = () => {
    if (
      [
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_TREND
      ].includes(widget.type as JIRA_SPRINT_REPORTS) &&
      get(filters, ["across"], "") !== SPRINT
    ) {
      return sprintMetricStatCsvTransformer;
    }
    return (
      get(widgetConstants, [widget.type, "CSV_DRILLDOWN_TRANSFORMER"], csvDrilldownDataTransformer) ||
      csvDrilldownDataTransformer
    );
  };

  const drilldownHeaderProps = {
    showTitle: showTitle(),
    title: getDrillDownTitle(),
    type: getDrillDownType(),
    onOpenReport: handleLoadTickets,
    onDrilldownClose
  };

  const shouldDerive = () => {
    const application = get(drillDownProps, ["application"], "");
    if (
      [
        LEVELOPS_REPORTS.ASSESSMENT_COUNT_REPORT,
        LEVELOPS_REPORTS.ASSESSMENT_COUNT_REPORT_TRENDS,
        LEVELOPS_REPORTS.ASSESSMENT_RESPONSE_TIME_REPORT,
        LEVELOPS_REPORTS.ASSESSMENT_RESPONSE_TIME_TABLE_REPORT
      ].includes(report as LEVELOPS_REPORTS)
    ) {
      return ["work_item_id"];
    }
    if (widget && integrationDeriveSupport.includes(widget.type)) {
      return ["integration_id"];
    }
    if (application.includes("jira") || application.includes("azure_devops")) {
      return ["custom_fields", "integration_id"];
    }
    return [];
  };

  const getDrilldownFooter = () => {
    const drilldownfooter = get(widgetConstants, [widget.type, "drilldownFooter"], undefined);
    if (drilldownfooter) {
      const component = drilldownfooter({ filters });
      if (component) {
        return React.createElement(component, { filters, setFilters });
      }
    }
    return drilldownfooter;
  };

  // Remove duplicate keys from sort.
  if (filters.filter && filters.filter.hasOwnProperty("sort") && Array.isArray(filters.filter.sort)) {
    filters.filter["sort"] = uniqBy(filters.filter.sort, "id");
  }

  if (!filters || Object.keys(filters).length === 0 || report.length === 0) {
    return null;
  }

  const getRatings = (filters: any) => {
    return filters?.filter?.ratings?.map((item: any) => {
      return item;
    });
  };
  const handleDrilldownCheckbox = (e: any) => {
    const handleCheckboxFilter = get(
      widgetConstants,
      [widget.type, "drilldownCheckBoxhandler", "handleRatingChange"],
      undefined
    );
    if (handleCheckboxFilter) {
      const _filters = handleCheckboxFilter({ filters, checked: e.target.checked });
      setFilters(_filters);
      return;
    }
    let indicators = drilldownMissingAndOtherRatings ? getRatings(filters) : ["good", "needs_attention", "slow"];
    if (drilldownMissingAndOtherRatings) {
      const index = indicators?.indexOf("missing");
      if (index && index < 0) {
        indicators?.push("missing");
      } else {
        indicators?.pop();
      }
      if (!indicators?.length) {
        indicators = ["good", "needs_attention", "slow"];
      }
      setFilterRatings(indicators);
    } else {
      setFilters({
        ...filters,
        filter: { ...filters.filter, ratings: filters.filter.ratings?.[0] === "missing" ? indicators : ["missing"] }
      });
    }
  };

  const getDrilldownCheckBox = () => {
    const drilldownCheckbox = get(widgetConstants, [widget.type, "drilldownCheckbox"], undefined);
    if (drilldownCheckbox) {
      const component = drilldownCheckbox({ filters });
      if (component) {
        let value = drilldownMissingAndOtherRatings
          ? filters?.filter?.ratings?.indexOf("missing") > -1
            ? true
            : false
          : filters?.filter?.ratings?.[0] === "missing";
        const getCheckBoxValue = get(
          widgetConstants,
          [widget.type, "drilldownCheckBoxhandler", "getCheckBoxValue"],
          undefined
        );
        const checkboxTitle = get(widgetConstants, [widget.type, "drilldownCheckBoxhandler", "title"], undefined);
        if (getCheckBoxValue) {
          value = getCheckBoxValue({ filters });
        }
        return React.createElement(component, {
          onClick: handleDrilldownCheckbox,
          value: value,
          title: checkboxTitle
        });
      }
    }
    return drilldownCheckbox;
  };

  const configureDrilldownDynamicColumns = (columns: (ExtraColumnProps & ColumnProps<any>)[]) => {
    /** transforming columns as per report requirements */
    const drilldownDynamicColumnTransformer: ReportDrilldownColTransFuncType | undefined = getTableConfig(
      "drilldownDynamicColumnTransformer"
    );

    if (drilldownDynamicColumnTransformer) {
      return drilldownDynamicColumnTransformer({
        columns,
        filters: {
          ...drillDownProps,
          metadata: widget?.metadata
        }
      });
    }

    return columns;
  };

  return (
    <div className="w-100" style={{ padding: 16, minHeight: "300px" }} id={`${drillDownProps?.widgetId}-drilldown`}>
      <AntCard
        className={cx("drilldownPreview", { "customised-scroll": extraDrilldownProps?.customisedScroll })}
        title={""}>
        {drillDownProps && (
          <Row gutter={[10, 10]}>
            <Col span={24} className="drillTable pr-14">
              {drilldownNotSupportedCondition ? (
                <>
                  <DrillDownFilterContent drilldownHeaderProps={{ onDrilldownClose }} />
                  <div style={{ margin: "30px", textAlign: "center" }}>Drilldown is not available</div>
                </>
              ) : (
                <>
                  {["zendesk_time_across_stages", "salesforce_time_across_stages"].includes(
                    drillDownProps?.application
                  ) ? (
                    <CustomTableUsingSaga
                      filters={filters}
                      report={report || ""}
                      slicedColumns={true}
                      drilldownHeaderProps={drilldownHeaderProps}
                    />
                  ) : [LEVELOPS_REPORTS.TABLE_REPORT].includes(report) ? (
                    <LevelopsTableReportDrilldownComponent
                      filters={filters?.filter ?? {}}
                      tableId={get(widgetMetaData, ["tableId"], "")}
                      drilldownHeaderProps={drilldownHeaderProps}
                      widgetId={widget?.id}
                      widgetDrilldownColumns={widget?.drilldown_columns}
                    />
                  ) : ["dev_raw_stats"].includes(drillDownProps?.application) ? (
                    <DevRawStatsDrilldownWrapper
                      drilldownHeaderProps={drilldownHeaderProps}
                      drillDownProps={drillDownProps}
                    />
                  ) : (
                    <ServerPaginatedTable
                      showTitle
                      title="Drilldown Preview"
                      uri={getUri()}
                      reload={reload}
                      method={"list"}
                      pageSize={10}
                      derive={getDerive()}
                      shouldDerive={shouldDerive()}
                      columns={mappedColumns()}
                      moreFilters={filters.filter}
                      ouFilters={getOUFilters()}
                      sort={sort}
                      across={getAcross(across, widget)}
                      interval={getInterval()}
                      hasSearch={false}
                      hasFilters={false}
                      uuid={DRILLDOWN_UUID}
                      downloadCSV={{
                        tableDataTransformer: getCSVDataTransformer(),
                        jsxHeaders: getJsxHeaders()
                      }}
                      drilldownHeaderProps={drilldownHeaderProps}
                      {...extraDrilldownProps}
                      drilldown={true}
                      report={report}
                      selectedDrilldownColumns={selectedColumns}
                      drilldownFooter={getDrilldownFooter()}
                      drilldownCheckbox={getDrilldownCheckBox()}
                      isLeadTimeByStage={isLeadTimeByStage}
                      mapFiltersBeforeCall={mapFiltersBeforeCall}
                      doraProfileIntegrationType={doraProfileIntegrationType}
                      setSelectedColumns={setSelectedColumns}
                      doraProfileDeploymentRoute={doraProfileDeploymentRoute}
                      doraProfileEvent={doraProfileEvent}
                      doraProfileIntegrationApplication={doraProfileIntegrationApplication}
                      configureDynamicColumns={configureDrilldownDynamicColumns}
                      testrailsCustomField={testrailsCustomField}
                      widgetMetaData={widgetMetaData}
                    />
                  )}
                </>
              )}
            </Col>
          </Row>
        )}
      </AntCard>
    </div>
  );
};

export default DashboardDrillDownPreview;
