import React from "react";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { JiraSalesforceNodeType, JiraZendeskNodeType } from "custom-hooks/helpers/sankey.helper";
import { scmTableReportType } from "dashboard/constants/enums/scm-reports.enum";
import { ReportDrilldownColTransFuncType } from "dashboard/dashboard-types/common-types";
import { getLevelopsResponseTimeReport } from "shared-resources/charts/table-chart/table-helper";
import { baseColumnConfig } from "utils/base-table-config";
import { forEach, get, map, uniq, uniqBy } from "lodash";
import { useParams } from "react-router-dom";
import {
  DRILL_DOWN_APPLICATIONS_WITH_FIVE_COLUMNS,
  DRILL_DOWN_APPLICATIONS_WITH_FOUR_COLUMNS,
  DRILL_DOWN_APPLICATIONS_WITH_SIX_COLUMNS,
  DRILL_DOWN_APPLICATIONS_WITH_THREE_COLUMNS,
  MAX_SPRINT_METRICS_UNIT_COLUMNS
} from "./helper-constants";
import { JiraIssueLink } from "shared-resources/components/jira-issue-link/jira-issue-link-component";
import { committerColumn, fileTypeColumn, repoColumn } from "../dashboard-tickets/configs/githubTableConfig";
import { jenkinsBuildColumn } from "../dashboard-tickets/configs/jenkinsTableConfig";
import { zendeskResolutionTimeTableConfig } from "../dashboard-tickets/configs/zendeskTableConfig";
import { getFilterValue } from "configurable-dashboard/helpers/helper";
import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { JiraReports } from "dashboard/constants/enums/jira-reports.enum";
import { MAX_DRILLDOWN_COLUMNS } from "dashboard/constants/filter-key.mapping";
import { depencyAnalysisOptionBEKeys } from "dashboard/graph-filters/components/Constants";
import { SprintNodeType } from "../../../custom-hooks/helpers/constants";
import { timeColumn } from "../dashboard-tickets/configs/common-table-columns";
import {
  azure_resolution_time,
  dependencyAnalysisDrilldownColumns,
  jira_resolution_time
} from "../dashboard-tickets/configs/jiraTableConfig";
import { sprintMetricStatUnitColumns } from "../dashboard-tickets/configs/sprintSingleStatTableConfig";
import {
  AZURE_SPRINT_REPORTS,
  DORA_REPORTS,
  ISSUE_MANAGEMENT_REPORTS,
  JENKINS_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  JIRA_SPRINT_REPORTS,
  PAGERDUTY_REPORT,
  scmCicdReportTypes,
  scmCicdStatReportTypes,
  SCM_REPORTS,
  SPRINT,
  TESTRAILS_REPORTS
} from "dashboard/constants/applications/names";
import { getAcross, jenkinsBuildTypeReports, TicketLifeTimeCloumn } from "./helper";
import { getSCMColumnsForMetrics } from "dashboard/constants/helper";
import widgetConstants from "dashboard/constants/widgetConstants";
import { RBAC } from "constants/localStorageKeys";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { getDashboardsPage } from "constants/routePaths";
import { ProjectPathProps } from "classes/routeInterface";
import { IssueManagementOptions } from "constants/issueManagementOptions";

export const IM_ADO = "IM_ADO";

const getTableConfig = (report: any, key: string, defaultValue?: any) => {
  return get(widgetConstants, [report, "drilldown", key], defaultValue);
};

const getJiraTicketLink = (item: any, record: any, userType: any, dashboard: any) => {
  let _baseUrl = `ticket_details?key=${record.key}&`;
  const hasAccess = window.isStandaloneApp
    ? getRBACPermission(PermeableMetrics.DRILLDOWN_COLUMNS_JIRA_TICKETS_URL)
    : true;
  if (hasAccess) {
    _baseUrl = `${_baseUrl}dashboardId=${dashboard.id}&`;
  }
  let integrationId = get(record, ["integration_id"], undefined);
  if (!integrationId) {
    integrationId = get(dashboard, ["query", "integration_ids"], []).toString();
  }
  _baseUrl = `${_baseUrl}integration_id=${integrationId}`;
  return <JiraIssueLink link={_baseUrl} ticketKey={item} integrationUrl={record?.integration_url} />;
};

const mapSankyJiraColumns = (columns: Array<any>, userType: any, dashboard: any) => {
  return (columns || []).map(column => {
    if (column.key === "key") {
      return {
        ...column,
        render: (item: any, record: any, index: number) => getJiraTicketLink(item, record, userType, dashboard)
      };
    }
    return column;
  });
};

export const defaultColumns = (props: any) => {
  const {
    drillDownProps,
    filters,
    across,
    integrationsList,
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
  } = props;
  const userType = localStorage.getItem(RBAC);
  const projectParams = useParams<ProjectPathProps>();

  const application = get(widgetConstants, [widget.type, "application"], "");
  const report = widget.type;

  const getJiraZendeskTicketLink = (item: any, record: any) =>
    Array.isArray(item) &&
    item.map(i => {
      let _baseUrl = `${getDashboardsPage(projectParams)}/ticket_details?key=${i}`;
      const hasAccess = window.isStandaloneApp
        ? getRBACPermission(PermeableMetrics.DRILLDOWN_COLUMNS_JIRA_TICKETS_URL)
        : true;
      if (hasAccess) {
        _baseUrl = `${_baseUrl}dashboardId=${dashboard.id}&`;
      }

      let integrationId = get(record, ["integration_id"], undefined);
      if (!integrationId) {
        integrationId = get(dashboard, ["query", "integration_ids"], []).toString();
      }
      _baseUrl = `${_baseUrl}&integration_id=${integrationId}`;
      return (
        <a target="_blank" href={_baseUrl} className="mr-5">
          {i}
        </a>
      );
    });

  const mapZendeskColumns = (columns: Array<any>) => {
    return (columns || []).map(column => {
      if (column.key === "jira_keys") {
        return {
          ...column,
          render: (item: any, record: any, index: number) => getJiraZendeskTicketLink(item, record)
        };
      }
      return column;
    });
  };

  const getScmCicdColumns = () => {
    const allColumns = getTableConfig(widget.type, "columns");
    if (
      [JENKINS_REPORTS.JOBS_COMMITS_LEAD_SINGLE_STAT_REPORT, JENKINS_REPORTS.JOBS_COMMIT_LEADS_TRENDS_REPORT].includes(
        report as any
      )
    ) {
      return allColumns.slice(0, 4);
    } else if (
      [
        JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_SINGLE_STAT_REPORT,
        JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_BY_FILE_SINGLE_STAT_REPORT,
        JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_TRENDS_REPORT
      ].includes(report as any)
    ) {
      const _initialColumns = (allColumns || []).slice(0, 3);
      return [..._initialColumns, (allColumns || [])[4], (allColumns || [])[5]];
    } else {
      return allColumns.slice(0, 3);
    }
  };

  const getSprintMetricSingleStatColumns = () => {
    const configColumns = getTableConfig(widget.type, "columns");
    const sprintMeTricCommonColumns = (configColumns || []).slice(0, Math.min(2, (configColumns || []).length));
    const selectedSprintMetric = get(filters, ["filter", "metric"], undefined);
    let resultantColumns: any[] = [...sprintMeTricCommonColumns];
    if (selectedSprintMetric) {
      let sprintMetricAndUnitColumns = [...(sprintMetricStatUnitColumns || []), ...(configColumns || [])].filter(
        (column: any) => {
          const metricKeys: string[] = get(column, ["metricKeys"], undefined);
          return !!metricKeys && metricKeys.includes(selectedSprintMetric);
        }
      );
      sprintMetricAndUnitColumns = (sprintMetricAndUnitColumns || []).slice(
        0,
        Math.min(MAX_SPRINT_METRICS_UNIT_COLUMNS, (sprintMetricAndUnitColumns || []).length)
      );
      resultantColumns = [...resultantColumns, ...(sprintMetricAndUnitColumns || [])];
    }
    return uniqBy(resultantColumns, "key");
  };

  const getPraetorianNccGroupColumns = () => {
    const allColumns = getTableConfig(widget.type, "columns");
    let _initialColumns = (allColumns || []).slice(0, 2);
    const acrossNew = getAcross(across, widget);
    const acrossColumn = (allColumns || []).find((c: any) =>
      c.key === ["project", "tag"].includes(acrossNew) ? `${acrossNew}s` : acrossNew
    );
    const stacks = get(filters, ["filter", "stacks"], []);
    if (acrossColumn) {
      _initialColumns.push(acrossColumn);
    }
    if (stacks.length) {
      const filteredColumns = (allColumns || []).filter((c: any) => {
        if (["projects", "tags"].includes(c.key)) {
          return stacks.includes("project") || stacks.includes("tag");
        }
        return stacks.includes(c.key);
      });
      if (filteredColumns.length) {
        _initialColumns = [..._initialColumns, ...filteredColumns];
      }
    }
    return uniqBy(_initialColumns, "key");
  };

  const getJiraAzureColumns = () => {
    let allColumns = getTableConfig(widget.type, "columns");

    if (
      [
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_TREND
      ].includes(widget.type)
    ) {
      const columnsForNodeTypes = getTableConfig(widget.type, "columnsForNodeTypes");
      if (columnsForNodeTypes) {
        allColumns = get(
          columnsForNodeTypes,
          [across === SPRINT ? SprintNodeType.SPRINT : SprintNodeType.TIME_RANGE],
          []
        );
      }
    }
    const columnSliceCount: number = getTableConfig(widget.type, MAX_DRILLDOWN_COLUMNS, 3);
    let _initialColumns = (allColumns || []).slice(0, columnSliceCount);

    const jiraAcrossMap = {
      component: "component_list",
      label: "labels"
    };
    const newAcross = getAcross(across, widget);
    const acrossValue = get(jiraAcrossMap, [newAcross || ""], newAcross);
    const stacks = get(filters, ["filter", "stacks"], [])
      .filter((s: any) => !!s)
      .map((s: any) => get(jiraAcrossMap, [s], s));

    if (!_initialColumns.map((c: any) => c.key).includes(acrossValue)) {
      const acrossColumn = (allColumns || []).find((c: any) => c.key === acrossValue);
      if (acrossColumn) {
        _initialColumns.push(acrossColumn);
      }
    }
    if (stacks.length) {
      const filteredColumns = (allColumns || []).filter((c: any) => stacks.includes(c.key));
      if (filteredColumns.length) {
        _initialColumns = [..._initialColumns, ...filteredColumns];
      }
    }
    if (
      (acrossValue === "trend" || acrossValue === "issue_created") &&
      ![
        "jira_backlog_trend_report",
        ISSUE_MANAGEMENT_REPORTS.BACKLOG_TREND_REPORT,
        jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
        ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT_TREND,
        ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT,
        ISSUE_MANAGEMENT_REPORTS.TICKET_REPORT_TREND,
        ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT,
        ISSUE_MANAGEMENT_REPORTS.RESPONSE_TIME_REPORT,
        ISSUE_MANAGEMENT_REPORTS.RESPONSE_TIME_TREND_REPORT,
        ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_TREND_REPORT,
        ISSUE_MANAGEMENT_REPORTS.BOUNCE_REPORT,
        ISSUE_MANAGEMENT_REPORTS.BOUNCE_REPORT_TRENDS,
        ISSUE_MANAGEMENT_REPORTS.HOPS_REPORT,
        ISSUE_MANAGEMENT_REPORTS.HOPS_REPORT_TRENDS
      ].includes(report)
    ) {
      _initialColumns.push(timeColumn("Created At", "issue_created_at", { sorter: true }));
    }

    if (
      acrossValue === "trend" &&
      [
        ISSUE_MANAGEMENT_REPORTS.TICKET_REPORT_TREND,
        ISSUE_MANAGEMENT_REPORTS.RESPONSE_TIME_REPORT,
        ISSUE_MANAGEMENT_REPORTS.RESPONSE_TIME_TREND_REPORT,
        ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_TREND_REPORT,
        ISSUE_MANAGEMENT_REPORTS.BOUNCE_REPORT_TRENDS
      ].includes(report as any)
    ) {
      _initialColumns.push(timeColumn("Workitem Created At", "workitem_created_at"));
    }

    if ([ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT_TREND].includes(report as any)) {
      _initialColumns.push(timeColumn("Created At", "workitem_created_at", { sorter: true }));
    }
    if (["jira_backlog_trend_report", ISSUE_MANAGEMENT_REPORTS.BACKLOG_TREND_REPORT].includes(report)) {
      const recordKey: string = report === "jira_backlog_trend_report" ? "issue_created_at" : "workitem_created_at";
      _initialColumns.push(TicketLifeTimeCloumn(get(drillDownProps, ["x_axis"], ""), recordKey));
    }
    if (acrossValue === "issue_updated") {
      _initialColumns.push(timeColumn("Updated At", "issue_updated_at", { sorter: true }));
    }
    if (acrossValue && (acrossValue.includes("customfield_") || acrossValue.includes("Custom."))) {
      _initialColumns.push({
        key: "custom_fields_mappings",
        dataIndex: "custom_fields_mappings",
        width: "10%",
        title: undefined,
        hasAcrossBasedTitle: true,
        render: (value: any, record: any) => {
          const customVal = get(record, ["custom_fields", acrossValue], undefined);
          if (customVal) {
            if (typeof customVal === "string") return customVal;
            if (Array.isArray(customVal)) return customVal.toString();
          }
        }
      });
    }

    if (
      [
        JIRA_MANAGEMENT_TICKET_REPORT.TICKETS_REPORT,
        JIRA_MANAGEMENT_TICKET_REPORT.RESOLUTION_TIME_REPORT,
        JIRA_MANAGEMENT_TICKET_REPORT.BACKLOG_TREND_REPORT,
        ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT,
        ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
        ISSUE_MANAGEMENT_REPORTS.BACKLOG_TREND_REPORT
      ].includes(report as any) &&
      stacks.length
    ) {
      stacks
        .filter((key: string) => key.includes("customfield_") || key.includes("Custom."))
        .forEach((key: string) => {
          let title = key;
          const customField = supportedCustomFields.find((item: any) => item.field_key === key);

          if (customField) {
            title = customField.name || "";
          }

          _initialColumns.push({
            key: key,
            dataIndex: key,
            width: "10%",
            title: title,
            render: (value: any, record: any) => {
              const customVal: any = get(record, ["custom_fields", key], undefined);
              if (customVal) {
                if (typeof customVal === "string") return customVal;
                return customVal.toString();
              }
            }
          });
        });
    }

    if (report === "bounce_report" || report === "bounce_report_trends") {
      const bounceColumn = (allColumns || []).find((column: any) => column.key === "bounces");
      if (bounceColumn) {
        _initialColumns.push(bounceColumn);
      }
    }
    if (
      [
        ISSUE_MANAGEMENT_REPORTS.HOPS_REPORT,
        ISSUE_MANAGEMENT_REPORTS.HOPS_REPORT_TRENDS,
        JIRA_MANAGEMENT_TICKET_REPORT.HOPS_REPORT,
        JIRA_MANAGEMENT_TICKET_REPORT.HOPS_REPORT_TRENDS
      ].includes(report as any)
    ) {
      const hopsColumn = (allColumns || []).find((column: any) => column.key === "hops");
      if (hopsColumn) {
        _initialColumns.push(hopsColumn);
      }
    }

    const reportWithResolutionTime = [
      "jira_time_across_stages",
      "resolution_time_report",
      "resolution_time_report_trends"
    ];
    if (reportWithResolutionTime.includes(report)) {
      _initialColumns.push(jira_resolution_time);
    }

    if (
      [
        ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_TREND_REPORT,
        ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
        ISSUE_MANAGEMENT_REPORTS.TIME_ACROSS_STAGES
      ].includes(report as any)
    ) {
      _initialColumns.push(azure_resolution_time);
    }

    // NEED TO HIDE THIS COLUMN FROM DRILLDONW FOR THIS WIDGTE BECUASE NO SUPPORT FOR KEY FROM BE ONCE IT WILL AVAILABLE WE WILL OPEN THIS
    // if (report === JiraReports.JIRA_TICKETS_REPORT) {
    //   const dependencyAnalysis = getFilterValue(get(filters, ["filter"], {}), "links");
    //   forEach(dependencyAnalysis, (depency: depencyAnalysisOptionBEKeys) => {
    //     _initialColumns.push(dependencyAnalysisDrilldownColumns[depency]);
    //   });
    // }

    // For drilldown columns please try to use drilldownVisibleColumn config in widget.
    const drilldownColumns = get(widgetConstants, [report, "drilldown", "drilldownVisibleColumn"], []);
    if (drilldownColumns.length > 0) {
      const drilldownVisibleColumn = drilldownColumns.map((drillColumn: any) => {
        return allColumns.find((col: any) => {
          return drillColumn === col.key;
        });
      });
      _initialColumns = _initialColumns.concat(drilldownVisibleColumn);
    }
    if (filters?.filter?.fetch_epic_summary) {
      const epicSummary = allColumns.find((cl: any) => cl.key === "epic_summary");
      if (epicSummary) {
        _initialColumns.push(epicSummary);
      }
    } else {
      _initialColumns = _initialColumns.filter((cl: any) => cl.key !== "epic_summary");
    }
    return uniqBy(_initialColumns, "key");
  };

  let _columns;
  let slice = 2;
  if (DRILL_DOWN_APPLICATIONS_WITH_THREE_COLUMNS.includes(application)) {
    slice = 3;
  }
  if (DRILL_DOWN_APPLICATIONS_WITH_FOUR_COLUMNS.includes(application)) {
    slice = 4;
  }
  if (DRILL_DOWN_APPLICATIONS_WITH_FIVE_COLUMNS.includes(application)) {
    slice = 5;
  }

  if (DRILL_DOWN_APPLICATIONS_WITH_SIX_COLUMNS.includes(application)) {
    slice = 6;
  }

  if (["jirazendesk", "jirasalesforce"].includes(application)) {
    const columnsForNodeTypes = getTableConfig(widget.type, "columnsForNodeTypes");
    if (columnsForNodeTypes) {
      const type = get(drillDownProps, [application, "type"], "");
      _columns = get(columnsForNodeTypes, type, "");
      if (type === JiraZendeskNodeType.JIRA || type === JiraSalesforceNodeType.JIRA) {
        _columns = mapSankyJiraColumns(_columns, userType, dashboard);
      }
      if (type === JiraZendeskNodeType.ZENDESK_LIST) {
        _columns = mapZendeskColumns(_columns);
      }
      if (type === JiraZendeskNodeType.ZENDESK) {
        slice = 5;
        _columns = zendeskResolutionTimeTableConfig.filter((col: any) => col.key !== "requester_email");
      }
    }
  }

  if (report === PAGERDUTY_REPORT.RESPONSE_REPORTS) {
    slice = 3;
    const issueType = get(filters, ["filter", "issue_type"], "incident");
    if (issueType === "alert") {
      _columns = getTableConfig(widget.type, "alertColumns");
    }
  }

  if (!_columns) {
    // Set Default Columns if not explicitly assigned
    _columns = getTableConfig(widget.type, "columns");
    if ([IntegrationTypes.JIRA, IntegrationTypes.JIRA_ASSIGNEE_TIME_REPORT].includes(application)) {
      _columns = mapSankyJiraColumns(_columns, userType, dashboard);
    }
  }

  /** transforming columns as per report requirements */
  const drilldownColumnTransformFunction: ReportDrilldownColTransFuncType | undefined = getTableConfig(
    widget.type,
    "drilldownColumnTransformer"
  );

  if (drilldownColumnTransformFunction) {
    return drilldownColumnTransformFunction({
      columns: _columns,
      categoryColorMapping,
      filters: {
        ...drillDownProps,
        query: widget?.query,
        metadata: widget?.metadata,
        integrationType: doraProfileIntegrationType || "",
        integrationApplication: doraProfileIntegrationApplication || ""
      },
      doraProfileDeploymentRoute,
      doraProfileEvent
    });
  }

  if (["praetorian", "nccgroup"].includes(application)) {
    return getPraetorianNccGroupColumns();
  }

  if ([...scmCicdReportTypes, ...scmCicdStatReportTypes].includes(report as any)) {
    return getScmCicdColumns();
  }

  if (
    [JIRA_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT, AZURE_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT].includes(
      widget?.type
    )
  ) {
    return getSprintMetricSingleStatColumns();
  }

  if (["jira", "azure_devops"].includes(application)) {
    return getJiraAzureColumns();
  }

  if (["scm_repos_report", "scm_committers_report", scmTableReportType.SCM_FILE_TYPES_REPORT].includes(report)) {
    const metrics = get(drillDownProps?.widgetMetaData, "metrics", ["num_commits", "num_prs", "num_changes"]);
    let columns = getSCMColumnsForMetrics(metrics);
    if (columns.some((col: any) => ["num_jira_issues", "num_workitems"].includes(col.dataIndex))) {
      const newColumns = [
        ...columns,
        baseColumnConfig("Number of Workitems", "num_workitems", { sorter: true, align: "center" })
      ];
      const applications = uniq(map(integrationsList, (rec: any) => rec?.application));
      if (applications.includes(IssueManagementOptions.JIRA) && applications.includes(IssueManagementOptions.AZURE)) {
        columns = newColumns;
      } else if (applications.includes(IssueManagementOptions.JIRA)) {
        newColumns.push(baseColumnConfig("Number of Issues", "num_jira_issues", { sorter: true, align: "center" }));
        columns = newColumns.filter((item: any) => item.dataIndex !== "num_workitems");
      } else if (applications.includes(IssueManagementOptions.AZURE)) {
        newColumns.push(baseColumnConfig("Number of Workitems", "num_workitems", { sorter: true, align: "center" }));
        columns = newColumns.filter((item: any) => item.dataIndex !== "num_jira_issues");
      } else columns = newColumns.filter((item: any) => !["num_jira_issues", "num_workitems"].includes(item.dataIndex));
    }

    return uniqBy(
      [
        report === "scm_repos_report"
          ? repoColumn
          : report === "scm_committers_report"
          ? committerColumn
          : fileTypeColumn,
        ...columns
      ],
      "dataIndex"
    );
  }

  if (widget && jenkinsBuildTypeReports.includes(widget.type)) {
    return [jenkinsBuildColumn, ...(_columns || [])].slice(0, 3);
  }

  if (widget && drillDownProps && widget.type === "levelops_assessment_response_time__table_report") {
    _columns = getLevelopsResponseTimeReport(drillDownProps.x_axis, _columns);
  }

  if (["zendesk"].includes(application as any)) {
    return zendeskResolutionTimeTableConfig.filter((col: any) => col.key !== "requester_email");
  }

  let getCustomColumn = get(widgetConstants, [widget?.type, "drilldown", "getCustomColumn"], []);
  if ([TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT].includes(widget?.type) && getCustomColumn) {
    let customFiled = getCustomColumn(testrailsCustomField);
    _columns = [..._columns, ...customFiled];
  }

  return _columns || []; //.slice(0, slice);
};

export const allColumns = (props: any) => {
  const {
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
  } = props;
  const application = get(widgetConstants, [widget.type, "application"], "");
  const userType = localStorage.getItem(RBAC);

  let _columns;
  if (["jirazendesk", "jirasalesforce"].includes(application as any)) {
    const columnsForNodeTypes = getTableConfig(widget.type, "columnsForNodeTypes");
    if (columnsForNodeTypes) {
      const app = JSON.parse(get(queries, [application as any], "{}") as any);
      _columns = get(columnsForNodeTypes, app.type, "");
      if (app.type === JiraZendeskNodeType.JIRA || app.type === JiraSalesforceNodeType.JIRA) {
        _columns = mapSankyJiraColumns(_columns, userType, dashboard);
      }
      if (app.type === JiraZendeskNodeType.ZENDESK) {
        _columns = zendeskResolutionTimeTableConfig;
      }
    }
  }

  if (widget.type === PAGERDUTY_REPORT.RESPONSE_REPORTS) {
    const issueType = get(filters, ["filter", "issue_type"], "incident");
    if (issueType === "alert") {
      _columns = getTableConfig(widget.type, "alertColumns");
    }
  }

  if (
    [
      JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
      JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND,
      AZURE_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
      AZURE_SPRINT_REPORTS.SPRINT_METRICS_TREND
    ].includes(widget.type)
  ) {
    const columnsForNodeTypes = getTableConfig(widget.type, "columnsForNodeTypes");
    const across = get(filters, ["across"], "");
    const isSprint = across === SPRINT;
    return get(columnsForNodeTypes, [isSprint ? SprintNodeType.SPRINT : SprintNodeType.TIME_RANGE], []);
  }

  if (!_columns) {
    // Set Default Columns if not explicitly assigned
    _columns = getTableConfig(widget.type, "columns");

    if (["jira", "jira_assignee_time_report"].includes((get(queries, "application") as string) || "")) {
      _columns = mapSankyJiraColumns(_columns, userType, dashboard);
    }
    const { x: x_axis } = queries;
    if (widget.type === "jira_backlog_trend_report" || widget.type === "azure_backlog_trend_report") {
      const recordKey: string =
        widget?.type === "jira_backlog_trend_report" ? "issue_created_at" : "workitem_created_at";
      _columns = [..._columns, TicketLifeTimeCloumn(x_axis?.toString(), recordKey)];
    }

    if (widget && jenkinsBuildTypeReports.includes(widget.type)) {
      _columns = [jenkinsBuildColumn, ...(_columns || [])];
    }

    if (["zendesk"].includes(application as any)) {
      _columns = zendeskResolutionTimeTableConfig;
    }
  }

  if (queries.application === "levelops_assessment_response_time") {
    _columns = getLevelopsResponseTimeReport(queries.x, _columns);
  }
  // NEED TO HIDE THIS COLUMN FROM DRILLDONW FOR THIS WIDGTE BECUASE NO SUPPORT FOR KEY FROM BE ONCE IT WILL AVAILABLE WE WILL OPEN THIS
  // if (reportType === JiraReports.JIRA_TICKETS_REPORT) {
  //   const dependencyAnalysis = getFilterValue(get(filters, ["filter"], {}), "links");
  //   forEach(dependencyAnalysis, (depency: depencyAnalysisOptionBEKeys) => {
  //     _columns.push(dependencyAnalysisDrilldownColumns[depency]);
  //   });
  // }

  if (
    [SCM_REPORTS.REPOS_REPORT, SCM_REPORTS.COMMITTERS_REPORT, scmTableReportType.SCM_FILE_TYPES_REPORT].includes(
      reportType || ""
    )
  ) {
    if (integrationsData) {
      const newColumns = [
        ..._columns,
        baseColumnConfig("Number of Workitems", "num_workitems", { sorter: true, align: "center" })
      ];
      const applications = uniq(map(integrationsData, (rec: any) => rec?.application));
      if (applications.includes(IssueManagementOptions.JIRA) && applications.includes(IssueManagementOptions.AZURE)) {
        _columns = newColumns;
      } else if (applications.includes(IssueManagementOptions.JIRA)) {
        _columns = newColumns.filter((item: any) => item.dataIndex !== "num_workitems");
      } else if (applications.includes(IssueManagementOptions.AZURE)) {
        _columns = newColumns.filter((item: any) => item.dataIndex !== "num_jira_issues");
      } else
        _columns = newColumns.filter((item: any) => !["num_jira_issues", "num_workitems"].includes(item.dataIndex));
    }
    _columns = uniqBy(
      [
        reportType === SCM_REPORTS.REPOS_REPORT
          ? repoColumn
          : reportType === SCM_REPORTS.COMMITTERS_REPORT
          ? committerColumn
          : fileTypeColumn,
        ...(_columns ?? {})
      ],
      "dataIndex"
    );
  }

  let selectedOuIntegration = integrations.filter((data: { id: any }) => filterIntegrationId.includes(data.id));
  /** transforming columns as per report requirements */
  const drilldownColumnTransformFunction: ReportDrilldownColTransFuncType | undefined = getTableConfig(
    widget.type,
    "drilldownColumnTransformer"
  );

  if (drilldownColumnTransformFunction) {
    const filters = {
      widgetMetaData: widget.metadata,
      query: widget.query,
      x_axis: queries.x,
      integrationType: doraProfileIntegrationType || "",
      integrationApplication: doraProfileIntegrationApplication || "",
      selectedOuIntegration: selectedOuIntegration || []
    };
    return drilldownColumnTransformFunction({
      columns: _columns,
      categoryColorMapping,
      tableRecords: filtersData,
      filters,
      doraProfileDeploymentRoute,
      doraProfileEvent
    });
  }

  let getCustomColumn = get(widgetConstants, [widget?.type, "drilldown", "getCustomColumn"], []);
  if ([TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT].includes(widget?.type) && getCustomColumn) {
    let customFiled = getCustomColumn(testrailsCustomField);
    _columns = [..._columns, ...customFiled];
  }

  return _columns;
};
