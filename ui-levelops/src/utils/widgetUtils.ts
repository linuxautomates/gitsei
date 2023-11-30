import { LEVELOPS_REPORTS } from "./../dashboard/reports/levelops/constant";
import { ISSUE_MANAGEMENT_REPORTS } from "dashboard/constants/applications/names";
import {
  DefaultKeyTypes,
  jiraBAReports,
  jiraBAStatReports
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { JiraReports } from "dashboard/constants/enums/jira-reports.enum";
import { scmTableReportType } from "dashboard/constants/enums/scm-reports.enum";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { DISPLAY_FORMAT_FILTER_KEY } from "dashboard/graph-filters/components/display-format-filter/helper";
import { cloneDeep, forEach, get } from "lodash";
import { RestDashboard, RestWidget } from "../classes/RestDashboards";
import { groupByRootFolderKeyCheck } from "../configurable-dashboard/helpers/helper";
import { defaultWeights, GROUP_BY_ROOT_FOLDER, zendeskHygieneDefaultWeights } from "../dashboard/constants/constants";
import { FileReports } from "../dashboard/constants/helper";
import { updateIssueCreatedAndUpdatedFilters } from "dashboard/graph-filters/components/updateFilter.helper";
import { DEFAULT_METADATA } from "../dashboard/constants/filter-key.mapping";
import widgetConstants, { getWidgetConstant } from "../dashboard/constants/widgetConstants";
import { buildInitialReportQuery } from "configurable-dashboard/helpers/queryHelper";

export function updateWidgetFiltersForReport(widget: RestWidget, reportType: string, globalApplicationFilters: any, dashboard: RestDashboard | null, profileType?:string) {
  let { query, metadata } = buildInitialReportQuery({
    report: reportType,
    globalApplicationFilters: globalApplicationFilters,
    metadata: widget.metadata,
    dashboard,
    profileType
  });

  if (reportType.includes("hygiene")) {
    let weights;
    if (reportType.includes("zendesk") || reportType.includes("salesforce")) {
      weights = { ...zendeskHygieneDefaultWeights };
    } else {
      weights = { ...defaultWeights };
    }
    widget.weights = weights;
  }

  if (groupByRootFolderKeyCheck(metadata)) {
    let newMetadata = {};
    forEach(Object.keys(metadata), (key: string) => {
      if (!key.includes(GROUP_BY_ROOT_FOLDER)) {
        newMetadata = {
          ...newMetadata,
          [key]: get(metadata, [key])
        };
      }
    });
    metadata = newMetadata;
  }

  if (
    [
      FileReports.JIRA_SALESFORCE_FILES_REPORT,
      FileReports.JIRA_ZENDESK_FILES_REPORT,
      FileReports.SCM_FILES_REPORT,
      FileReports.SCM_JIRA_FILES_REPORT
    ].includes(reportType as any)
  ) {
    metadata = { ...metadata, [`groupByRootFolder_${reportType}`]: true };
  }

  if (["scm_repos_report", "scm_committers_report", scmTableReportType.SCM_FILE_TYPES_REPORT].includes(reportType)) {
    metadata = { ...metadata, metrics: ["num_changes", "num_commits", "num_prs"] };
  }

  if (
    jiraBAReports.includes(reportType as jiraBAReportTypes) &&
    ![jiraBAReportTypes.JIRA_BURNDOWN_REPORT, jiraBAReportTypes.EFFORT_INVESTMENT_TEAM_REPORT].includes(
      reportType as jiraBAReportTypes
    )
  ) {
    widget.max_records = 5;
  }
  if (jiraBAStatReports.includes(reportType as any)) {
    const displayFormat = getWidgetConstant(reportType, DefaultKeyTypes.DEFAULT_DISPLAY_FORMAT_KEY, undefined);
    if (displayFormat) {
      metadata = { ...metadata, [DISPLAY_FORMAT_FILTER_KEY]: displayFormat };
    }
  }

  const _defaultMetaData = get(widgetConstants, [DEFAULT_METADATA], {});
  const hasDefaultMeta = Object.keys(_defaultMetaData).length > 0;

  if (hasDefaultMeta) {
    query = updateIssueCreatedAndUpdatedFilters(query, metadata, reportType);
  }

  if ([LEVELOPS_REPORTS.TABLE_REPORT, LEVELOPS_REPORTS.TABLE_STAT_REPORT].includes(reportType as any)) {
    query = {};
  }

  widget.query = query;
  widget.metadata = metadata;
  return widget;
}

export function transformCompositeWidgetsWithSingleReportToSingleReport(_widget: RestWidget, childWidget: RestWidget) {
  if (!_widget || !childWidget) {
    return _widget;
  }
  const widget = new RestWidget(_widget.json);
  widget.type = childWidget.type;
  widget.query = childWidget.query;
  widget.weights = childWidget.weights;
  widget.children = [];
  widget.widget_type = childWidget.widget_type;
  widget.metadata = {
    ...widget.metadata,
    ...childWidget.metadata,
    hidden: widget.hidden,
    order: widget.order,
    // child doesn't have description property so it's overriding widget description
    description: widget.metadata?.description || ""
  };
  return widget;
}

export function updateMultiTimeSeriesReport(widget: RestWidget, multiSeriesTime: string) {
  let restWidget = widget;
  let filters = cloneDeep(restWidget.query);
  filters = { ...(filters || {}), interval: multiSeriesTime };
  switch (restWidget.type) {
    case JiraReports.JIRA_TICKETS_REPORT:
    case JiraReports.RESOLUTION_TIME_REPORT:
      filters = {
        ...(filters || {}),
        across: "issue_created",
        sort: [{ id: "issue_created", desc: true }],
        sort_xaxis: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
      };
      break;
    case ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT:
    case ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT:
      filters = {
        ...(filters || {}),
        across: "workitem_created_at",
        sort: [{ id: "workitem_created_at", desc: true }],
        sort_xaxis: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
      };
      break;
    case JiraReports.BACKLOG_TREND_REPORT:
    case ISSUE_MANAGEMENT_REPORTS.BACKLOG_TREND_REPORT:
      filters = {
        ...(filters || {}),
        across: "trend",
        sort: [{ id: "trend", desc: true }],
        sort_xaxis: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
      };
      break;
  }
  restWidget.query = filters;
  return restWidget;
}
