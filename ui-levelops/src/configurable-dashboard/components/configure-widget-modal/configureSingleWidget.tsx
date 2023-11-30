import { Form } from "antd";
import { RestDashboard, RestWidget } from "classes/RestDashboards";
import DynamicGraphPaginatedFilters from "configurable-dashboard/dynamic-graph-filter/container/dynamic-graph-paginated-filters.container";
import { WIDGET_FILTER_TAB_ORDER } from "constants/widgets";
import {
  removeChildFilterKey,
  removeFilterKey
} from "dashboard/components/dashboard-application-filters/AddFiltersComponent/helpers";
import * as AppNames from "dashboard/constants/applications/names";
import {
  azureLeadTimeIssueReports,
  issueManagementReports,
  ISSUE_MANAGEMENT_REPORTS,
  JENKINS_REPORTS,
  jiraManagementTicketReport,
  leadTimeIssueReports,
  LEAD_TIME_REPORTS,
  PAGERDUTY_REPORT,
  supportReports,
  TESTRAILS_REPORTS
} from "dashboard/constants/applications/names";
import {
  BA_EFFORT_ATTRIBUTION_BE_KEY,
  BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY,
  BA_IN_PROGRESS_STATUS_BE_KEY,
  EffortAttributionOptions,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { azureBAReportTypes } from "dashboard/constants/enums/azure-ba-reports.enum";
import { EffortUnitType, jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { JiraReports } from "dashboard/constants/enums/jira-reports.enum";
import { DEFAULT_METADATA, RANGE_FILTER_CHOICE, REQUIRED_ONE_FILTER } from "dashboard/constants/filter-key.mapping";
import { ALLOWED_WIDGET_DATA_SORTING, WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import { FileReports } from "dashboard/constants/helper";
import { widgetDataSortingOptionsDefaultValue } from "dashboard/constants/WidgetDataSortingFilter.constant";
import WidgetFiltersContainer from "dashboard/graph-filters/components/FiltersContainer/WidgetFilters.container";
import {
  getIssueManagementReportType,
  rangeMap,
  supportSystemToReportTypeMap
} from "dashboard/graph-filters/components/helper";
import { widgetFilterOptionsNode } from "dashboard/helpers/helper";
import { cloneDeep, get, isEqual, set, uniq, unset } from "lodash";
import { LevelOpsFilter, StatTimeRangeFilterData } from "model/filters/levelopsFilters";
import { default as React, useEffect, useMemo, useState } from "react";
import { useSelector } from "react-redux";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { customTimeFilterKeysSelector } from "reduxConfigs/selectors/jira.selector";
import { getSelectedOU } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import { AntCol, AntRow } from "shared-resources/components";
import { combineAllFilters } from "shared-resources/containers/widget-api-wrapper/helper";
import { sanitizeObject } from "utils/commonUtils";
import { AZURE_TIME_FILTERS_KEYS, GROUP_BY_TIME_FILTERS, SCM_PRS_TIME_FILTERS_KEYS } from "constants/filters";
import { SPRINT_FILTER_META_KEY, valuesToFilters } from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { updateMultiTimeSeriesReport, updateWidgetFiltersForReport } from "utils/widgetUtils";
import { getGroupByRootFolderKey, singularPartialKey } from "../../helpers/helper";
import "./configureWidgetModel.scss";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { ADD_EXTRA_FILTER } from "dashboard/reports/jira/issues-report/constants";
import { allowedFilterInTestCaseCountMetric } from "dashboard/report-filters/testrails/common-filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { buildExcludeQuery, buildWidgetQuery, buildPartialQuery } from "configurable-dashboard/helpers/queryHelper";
import { TESTRAILS_COMMON_FILTER_KEY_MAPPING } from "dashboard/reports/testRails/commonTestRailsReports.constants";

const ConfigureSingleWidget = (props: any) => {
  const [widgetData, setWidgetData] = useState<any>(cloneDeep(props.widgetData) || {});
  const [weightError, setWeightError] = useState<string>("");
  const [currentFilterKeyValueChange, setCurrentFilterKeyValueChange] = useState<string>("");
  const [partialFilterError, setPartialFilterError] = useState<any>({});
  const [metadata, setMetadata] = useState<any>((props.widgetData || {}).metadata || {});
  const integrationIds = props?.globalFilters?.integration_ids || [];
  const integrations = useParamSelector(cachedIntegrationsListSelector, { integration_ids: integrationIds });
  const customeDateTimeKeysFields: Array<string> = useSelector(customTimeFilterKeysSelector);
  const dashboard: RestDashboard | null = useSelector(selectedDashboard);
  const selectedOUState = useSelector(getSelectedOU);

  const workspaceProfile = useParamSelector(workflowProfileDetailSelector, { queryParamOU: selectedOUState?.id });

  const query = (props.widgetData || {}).query || {};
  const selectedReport = props.widgetData.type;
  const dashboardMetaData = props?.dashboardMetaData;
  const githubApplictions = ["helix", "gitlab", ...AppNames.BITBUCKET_APPLICATIONS];

  const getWidgetConstant = (data: any) => {
    return get(widgetConstants, [selectedReport, data], undefined);
  };
  const widgetFilters = getWidgetConstant("filters");
  const filters = { ...(combineAllFilters(query, widgetFilters, {}) || {}), ...(props?.globalFilters || {}) };

  const reportFilterConfig = useMemo(() => {
    let config = getWidgetConstant(AppNames.REPORT_FILTERS_CONFIG);
    if (props.widgetData.isMultiTimeSeriesReport) {
      return getWidgetConstant(AppNames.MULTI_SERIES_REPORT_FILTERS_CONFIG) ?? [];
    }
    if (config) {
      let dashboardTimeRangeKey = get(props.widgetData, ["metadata", "dashBoard_time_keys"], {});
      const dynamicRequired = getWidgetConstant(REQUIRED_ONE_FILTER);
      if (typeof dynamicRequired === "function") {
        return dynamicRequired(config, query, selectedReport, dashboardTimeRangeKey);
      }
      if (typeof config === "function")
        return config({
          filters: query,
          integrationState: integrations,
          workspaceProfile: workspaceProfile
        });
      return config;
    }
    return [];
  }, [query, props.widgetData, integrations, filters, widgetFilters]);

  const isDashboardEffortProfileEnabled = useMemo(() => {
    return get(dashboardMetaData, ["effort_investment_profile"], false);
  }, [dashboardMetaData]);

  useEffect(() => {
    const effortFilter = reportFilterConfig.find((report: any) => report?.id === "effort_investment_profile");
    const globalDashboardValue = get(dashboardMetaData, ["effort_investment_profile_filter"], "");
    const ticketCategorizationSchemesValue = get(query, [effortFilter?.beKey], "");
    if (isDashboardEffortProfileEnabled && effortFilter && ticketCategorizationSchemesValue !== globalDashboardValue) {
      handleFilterSelectChange(globalDashboardValue, effortFilter?.beKey);
    }
  }, [isDashboardEffortProfileEnabled]);

  useEffect(() => {
    if (!isEqual(widgetData, props.widgetData)) {
      setWidgetData(props.widgetData);
      setMetadata((props.widgetData || {}).metadata || {});
    }
  }, [props.widgetData]);

  // This method updates report meta data to disable support/management field based on integration applications
  const updatedReportMetaData = (supportedApplications: string[]) => {
    if (supportedApplications.length && supportReports.includes(selectedReport as any)) {
      let data = widgetData;
      let application =
        supportedApplications.length === 0
          ? ""
          : supportedApplications.length > 1
          ? getWidgetConstant("application")
          : supportedApplications?.[0];
      let widgetMetadata = {
        ...(metadata || {}),
        default_value: application?.includes("zendesk") ? "zendesk" : "salesforce",
        disable_support_system: !(
          supportedApplications.includes("salesforce") && supportedApplications.includes("zendesk")
        )
      };
      data.metadata = widgetMetadata;
      setMetadata(widgetMetadata);
      setWidgetData(data);
      props.updateWidget(data);
    }
    if (
      supportedApplications.length &&
      [
        ...leadTimeIssueReports,
        ...azureLeadTimeIssueReports,
        ...issueManagementReports,
        ...jiraManagementTicketReport
      ].includes(selectedReport as any)
    ) {
      let data = widgetData;
      let widgetMetadata = {
        ...(metadata || {}),
        disable_issue_management_system:
          !(supportedApplications.includes("jira") && supportedApplications.includes("azure_devops")) ||
          supportedApplications.length === 0,
        default_value:
          supportedApplications.length === 0
            ? ""
            : supportedApplications.length > 1
            ? getWidgetConstant("application")
            : supportedApplications?.[0]
      };
      data.metadata = widgetMetadata;
      setMetadata(widgetMetadata);
      setWidgetData(data);
      props.updateWidget(data);
    }
  };

  useEffect(() => {
    let applications: any[] = [];
    (integrations || []).forEach((integration: any) => {
      if (integration?.id !== undefined) {
        applications.push(integration.application);

        if (githubApplictions.includes(integration.application) && !applications.includes("github")) {
          applications.push("github");
        }
      }
    });
    updatedReportMetaData(applications);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleMaxRecords = (value: number) => {
    let data = widgetData;
    data.max_records = value;
    setWidgetData(data);
    props.updateWidget(data);
  };

  const handleMetadataChange = (value: any, type: any, reportType?: String) => {
    if (type === "max_records") {
      handleMaxRecords(value);
      return;
    }
    let shouldUpdateType = false;
    let data = cloneDeep(widgetData);
    let nMetaData = cloneDeep(metadata);
    nMetaData = {
      ...(nMetaData || {}),
      [type]: value
    };

    if (type === "support_system") {
      const report = get(supportSystemToReportTypeMap, [selectedReport, value], "");
      data = updateWidgetFiltersForReport(data, report, props.globalApplicationFilters, dashboard);
      data.type = report;
      shouldUpdateType = true;
      nMetaData[WIDGET_FILTER_TAB_ORDER] = {};
    }

    if (type === "issue_management_system") {
      const report = getIssueManagementReportType(selectedReport, value);
      data = updateWidgetFiltersForReport(data, report, props.globalApplicationFilters, dashboard);
      nMetaData.dashBoard_time_keys = {};
      nMetaData[WIDGET_FILTER_TAB_ORDER] = {};
      const default_range_filter_choice = get(widgetConstants, [report, DEFAULT_METADATA, RANGE_FILTER_CHOICE], {});
      nMetaData.range_filter_choice = default_range_filter_choice;
      if (props.widgetData.isMultiTimeSeriesReport) {
        const multiTimeSeries = props.widgetData?.metadata?.multi_series_time || "quarter";
        let restWidget = new RestWidget(data);
        restWidget = updateMultiTimeSeriesReport(restWidget, multiTimeSeries);
        data = cloneDeep(restWidget);
      }

      const ouFilters = get(nMetaData, "ou_user_filter_designation", {});

      if (Object.keys(ouFilters).length) {
        if (selectedReport.includes("azure_")) {
          delete ouFilters?.azure_devops;
        } else {
          delete ouFilters?.jira;
        }
        nMetaData.ou_user_filter_designation = { ...ouFilters };
      }

      data.type = report;
      shouldUpdateType = true;
      nMetaData.default_value = value;
    }

    if (type === "dashBoard_time_keys") {
      const dashboard_time_disable_keys = Object.keys(nMetaData.dashBoard_time_keys).filter(
        (key: any) => !nMetaData.dashBoard_time_keys[key].use_dashboard_time
      );
      dashboard_time_disable_keys.forEach((key: string) => {
        unset(nMetaData, ["dashBoard_time_keys", key]);
      });
    }
    data.metadata = nMetaData;
    setMetadata(nMetaData);
    setWidgetData(data);
    props.updateWidget(data, shouldUpdateType);
  };

  const handleModifiedFilterValueChange = (payload: any) => {
    const { type, value, parentKey } = payload;
    let data = widgetData;
    let _type = get(valuesToFilters, [type], type);
    const parentQuery = get(data, ["query", parentKey], {});
    let query = {
      ...data.query,
      [parentKey]: {
        ...parentQuery,
        [_type]: value
      }
    };
    data.query = query;
    setWidgetData(data);
    props.updateWidget(data);
  };

  const handleFilterSelectChange = (
    value: any,
    type: any,
    exclude?: boolean,
    addToMetaData?: any,
    returnUpdatedQuery?: boolean,
    customEpics?: any
  ) => {
    if (value) setCurrentFilterKeyValueChange(type);

    let data = widgetData;
    const extraFilterFunCall = getWidgetConstant(ADD_EXTRA_FILTER);
    if (extraFilterFunCall) {
      data.query = extraFilterFunCall(data.query, type, value, customEpics);
    }
    // resetting data on changing to/from Commit Count BA Uri Unit
    if (type === TICKET_CATEGORIZATION_UNIT_FILTER_KEY) {
      const prevUnit = get(
        cloneDeep(props.widgetData?.query || {}),
        [TICKET_CATEGORIZATION_UNIT_FILTER_KEY],
        undefined
      );
      const application = getWidgetConstant("application");
      if (
        (prevUnit !== EffortUnitType.COMMIT_COUNT && value === EffortUnitType.COMMIT_COUNT) ||
        (prevUnit === EffortUnitType.COMMIT_COUNT && value !== EffortUnitType.COMMIT_COUNT) ||
        (prevUnit !== EffortUnitType.AZURE_COMMIT_COUNT && value === EffortUnitType.AZURE_COMMIT_COUNT) ||
        (prevUnit === EffortUnitType.AZURE_COMMIT_COUNT && value !== EffortUnitType.AZURE_COMMIT_COUNT)
      ) {
        let nMetadata = cloneDeep(metadata);
        nMetadata[WIDGET_FILTER_TAB_ORDER] = {};
        data.metadata = nMetadata;
        setMetadata(nMetadata);
        const selectedScheme = get(data?.query ?? {}, [TICKET_CATEGORIZATION_SCHEMES_KEY]);
        data = updateWidgetFiltersForReport(data, selectedReport, props.globalApplicationFilters, dashboard);
        const timeRangeKey = application === IntegrationTypes.AZURE ? "workitem_resolved_at" : "issue_resolved_at";
        const timeRange = get(data?.query ?? {}, [timeRangeKey], undefined);
        if (!!selectedScheme) {
          data.query = {
            ...(data.query ?? {}),
            [TICKET_CATEGORIZATION_SCHEMES_KEY]: selectedScheme
          };
        }
        if (timeRange && [EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(value)) {
          data.query = {
            ...(data.query ?? {}),
            committed_at: timeRange
          };
          unset(data.query, ["issue_resolved_at"]);
          unset(data.query, ["workitem_resolved_at"]);
        }
      }
      if (value === EffortUnitType.COMMIT_COUNT) {
        unset(data.query, [BA_EFFORT_ATTRIBUTION_BE_KEY]);
      }
      if (value !== EffortUnitType.TICKET_TIME_SPENT) {
        unset(data.query, [BA_IN_PROGRESS_STATUS_BE_KEY]);
      }
    }

    let query = {
      ...(data.query || {})
    };
    if (type !== "metadata") {
      let _type = get(getWidgetConstant("valuesToFilters"), [type], undefined);
      query = buildWidgetQuery(data.query || {}, value, _type || type, exclude, undefined, !_type);
    } else {
      let nMetaData = cloneDeep(metadata);
      nMetaData = {
        ...(nMetaData || {}),
        [getGroupByRootFolderKey(selectedReport)]: value
      };
      data.metadata = nMetaData;
      setMetadata(nMetaData);
    }

    if (addToMetaData) {
      handleBulkAddToMetadata(addToMetaData || {});
    }

    if (
      [
        FileReports.JIRA_SALESFORCE_FILES_REPORT,
        FileReports.JIRA_ZENDESK_FILES_REPORT,
        FileReports.SCM_FILES_REPORT,
        FileReports.SCM_JIRA_FILES_REPORT
      ].includes(selectedReport)
    ) {
      if (!get(data.metadata, [getGroupByRootFolderKey(selectedReport)], undefined)) {
        unset(query, ["module"]);
        unset(query, ["scm_module"]);
      }

      if (["scm_module", "module"].includes(type)) {
        const filterKey = FileReports.SCM_JIRA_FILES_REPORT === selectedReport ? "scm_module" : "module";
        const repoFilterKey = FileReports.SCM_JIRA_FILES_REPORT === selectedReport ? "scm_file_repo_ids" : "repo_ids";

        const module = value?.module;
        const repoId = value?.repoId;

        if ([FileReports.SCM_FILES_REPORT, FileReports.SCM_JIRA_FILES_REPORT].includes(selectedReport)) {
          query = {
            ...query,
            [filterKey]: module,
            [repoFilterKey]: repoId ? [repoId] : []
          };
          if (query?.hasOwnProperty("exclude") && query?.exclude?.hasOwnProperty(repoFilterKey)) {
            unset(query, ["exclude", repoFilterKey]);
          }
          if (
            query?.hasOwnProperty("partial_match") &&
            query?.partial_match?.hasOwnProperty(singularPartialKey[repoFilterKey])
          ) {
            unset(query, ["partial_match", singularPartialKey[repoFilterKey]]);
          }
        } else {
          query = {
            ...query,
            [filterKey]: module
          };
        }
      }
    }

    if (
      ["resolution_time_report", AppNames.ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT].includes(selectedReport) &&
      type === "graph_type" &&
      value === ChartType.CIRCLE
    ) {
      const metric = get(query, ["metric"], []);
      if (metric.length > 1) {
        query = {
          ...query,
          metric: metric[0]
        };
      }
    }

    if (selectedReport === JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_SINGLE_STAT_REPORT) {
      let by = "lines";
      if (query.agg_type === "files_changed") {
        by = "files";
      }
      query = {
        ...query,
        by: by
      };
    }

    if (
      [JiraReports.JIRA_TICKETS_REPORT, ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT].includes(selectedReport) &&
      query.visualization &&
      query.visualization === IssueVisualizationTypes.DONUT_CHART
    ) {
      unset(query, ["stacks"]);
      unset(query, ["custom_stacks"]);
    }

    if (
      [JiraReports.JIRA_TICKETS_REPORT, ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT].includes(selectedReport) &&
      query.visualization &&
      query.visualization === IssueVisualizationTypes.PERCENTAGE_STACKED_BAR_CHART &&
      (!query?.stacks || query?.stacks?.[0] === undefined)
    ) {
      set(query, ["visualization"], "bar_chart");
    }

    if (selectedReport === JENKINS_REPORTS.SCM_CODING_DAYS_REPORT && type === "metrics") {
      const interval = value.split("_").pop();
      query = {
        ...query,
        interval
      };
    }

    if (
      [
        AppNames.ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT,
        AppNames.JIRA_MANAGEMENT_TICKET_REPORT.TICKET_ISSUE_SINGLE_STAT
      ].includes(selectedReport as any) &&
      type === "agg_type"
    ) {
      const modifiedAcross = value.substring(0, value.lastIndexOf("_"));
      const intervalFromValue = value.substring(value.lastIndexOf("_") + 1, value.length);
      delete query?.agg_type;
      delete query?.time_period;
      query = {
        ...query,
        across: modifiedAcross,
        interval: intervalFromValue || "day"
      };
    }

    if (type === BA_EFFORT_ATTRIBUTION_BE_KEY && value === EffortAttributionOptions.CURRENT_ASSIGNEE) {
      unset(query, [BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY]);
    }

    if ([JENKINS_REPORTS.SCM_PRS_REPORT].includes(selectedReport as any) && type === "linked_issues_key") {
      if (value === "true") {
        query["has_issue_keys"] = "true";
      } else {
        unset(query, ["linked_issues_key"]);
        query["has_issue_keys"] = null;
      }
    }

    if (
      selectedReport === TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT &&
      type === "metric" &&
      value === "test_case_count"
    ) {
      set(query, ["across"], "project");
      unset(query, ["stacks"]);
      Object.keys(query).reduce((acc, obj) => {
        const convertKeyToBeKey = allowedFilterInTestCaseCountMetric.map(data => TESTRAILS_COMMON_FILTER_KEY_MAPPING[data] || data);
        if (!["across", "metric", "stacks", "custom_fields", ...convertKeyToBeKey].includes(obj)) {
          unset(query, [obj]);
        }
        return acc;
      }, {});
    }
    if (returnUpdatedQuery) {
      return query;
    }
    data.query = query;
    setWidgetData(data);
    props.updateWidget(data);
    // setQuery(query);

    let partialKey: string | undefined = undefined;
    const currentFilter = reportFilterConfig.find((item: LevelOpsFilter) => item.beKey === type);

    if (currentFilter) {
      partialKey = currentFilter.partialKey;
    }

    setPartialFilterError((prev: any) => ({ ...prev, [partialKey ?? type]: undefined }));
  };

  const handleTimeFilterChange = (value: any, type?: any, rangeType?: string, isCustom = false) => {
    const _type = get(rangeMap, type, type);
    setWidgetData((_data: any) => {
      if (isCustom) {
        _data.query = {
          ..._data.query,
          custom_fields: {
            ...(_data.query.custom_fields || {}),
            [type]: value.absolute
          }
        };
      } else {
        _data.query = {
          ..._data.query,
          [type]: value.absolute
        };
      }

      let metadata = {
        ...(_data.metadata || {}),
        range_filter_choice: {
          ...(_data.metadata?.range_filter_choice || {}),
          [_type]: { type: value.type, relative: value.relative }
        }
      };
      _data.metadata = metadata;
      setMetadata(metadata);
      props.updateWidget(_data);
      return _data;
    });
  };

  const handleExcludeFilter = (key: string, value: boolean, selectMode?: string, otherFlagData: any = {}) => {
    let data = widgetData;
    let _type = get(getWidgetConstant("valuesToFilters"), [key], undefined);

    let partialKey: string | undefined = undefined;
    const currentFilter = reportFilterConfig.find((item: LevelOpsFilter) => item.beKey === key);

    if (currentFilter) {
      partialKey = currentFilter.partialKey;
    }

    const query = buildExcludeQuery(
      data.query || {},
      _type || key,
      value,
      "custom_fields",
      !_type,
      partialKey,
      selectMode,
      otherFlagData
    );
    data.query = query;
    setWidgetData(data);
    props.updateWidget(data);
    setPartialFilterError((prev: any) => ({ ...prev, [partialKey ?? key]: undefined }));
  };

  const handlePartialFilters = (key: string, value: any, otherFlagData: any = {}) => {
    let data = widgetData;
    const { filters, error } = buildPartialQuery(data.query || {}, key, value, selectedReport, "", otherFlagData);
    if (!!error) {
      setPartialFilterError((prev: any) => ({ ...prev, [key]: error }));
    } else {
      data.query = { ...filters };
      setWidgetData(data);
      props.updateWidget(data);
      setPartialFilterError((prev: any) => ({ ...prev, [key]: undefined }));
    }
  };

  const handleAggregationAcrossSelection = (
    value: any,
    appendDeleteQuery?: { appendQuery?: { [x: string]: string[] }; deleteQuery?: string[] }
  ) => {
    let data = widgetData;

    let query = {
      ...(data.query || {}),
      across: value
    };

    if (
      [
        jiraBAReportTypes.JIRA_BURNDOWN_REPORT,
        jiraBAReportTypes.JIRA_PROGRESS_REPORT,
        azureBAReportTypes.AZURE_ISSUES_PROGRESS_REPORT,
        azureBAReportTypes.AZURE_PROGRAM_PROGRESS_REPORT
      ].includes(selectedReport) &&
      value
    ) {
      if (value === "epic") {
        unset(query, [TICKET_CATEGORIZATION_SCHEMES_KEY]);
      }
    }

    if (selectedReport === "cicd_scm_jobs_count_report" && value === "qualified_job_name") {
      query = {
        ...query,
        stacks: []
      };
    }

    if ([JiraReports.JIRA_TICKETS_REPORT].includes(selectedReport)) {
      if (!GROUP_BY_TIME_FILTERS.includes(value)) {
        unset(query, ["interval"]);
      }

      const acrossFilterKey = get(valuesToFilters, [value], value);

      if (!get(query, acrossFilterKey, undefined)) {
        unset(query, ["filter_across_values"]);
      }
    }

    if (
      [
        AppNames.ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT,
        AppNames.ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT
      ].includes(selectedReport)
    ) {
      if (!AZURE_TIME_FILTERS_KEYS.includes(value)) {
        unset(query, ["interval"]);
      }
    }

    if (
      selectedReport === "zendesk_tickets_report" &&
      ["ticket_created_day", "ticket_created_week", "ticket_created_month"].includes(value)
    ) {
      delete query.stacks;
    }

    if (
      ["resolution_time_report", "scm_issues_time_resolution_report"].includes(selectedReport) &&
      ["issue_created", "issue_updated"].includes(value)
    ) {
      delete query.interval;
    }

    if (
      [JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_REPORT, JENKINS_REPORTS.SCM_PRS_REPORT].includes(selectedReport as any) &&
      !SCM_PRS_TIME_FILTERS_KEYS.includes(value)
    ) {
      delete query.interval;
    }

    if ("scm_issues_time_across_stages_report" === selectedReport) {
      if (!["created_at", "resolved_at"].includes(value)) {
        delete query.interval;
      }

      if (value === "column") {
        delete query.stacks;
      }
    }

    if (
      ["praetorian_issues_report", "ncc_group_vulnerability_report", "snyk_vulnerability_report"].includes(
        selectedReport
      ) &&
      value
    ) {
      if (value === "trend") {
        delete query.stacks;
      } else {
        query = {
          ...query,
          stacks: query.stacks?.filter((f: string) => f !== value)
        };
      }
    }

    if (
      [
        AppNames.ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_REPORT,
        AppNames.JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_REPORT
      ].includes(selectedReport as any) &&
      ["stage", "issue_created", "issue_updated", "workitem_created_at", "workitem_resolved_at"].includes(value)
    ) {
      delete query.stacks;
    }

    if (value.includes("issue_resolved")) {
      let interval = "day";
      switch (value) {
        case "issue_resolved_week":
          interval = "week";
          break;
        case "issue_resolved_month":
          interval = "month";
          break;
        case "issue_resolved_quarter":
          interval = "quarter";
          break;
      }
    }

    if (
      [JENKINS_REPORTS.CICD_JOBS_COUNT_REPORT, JENKINS_REPORTS.CICD_SCM_JOBS_DURATION_REPORT].includes(
        selectedReport as any
      )
    ) {
      if (value.includes("job_end")) {
        const interval = value.split("_").pop();
        query = {
          ...query,
          across: "job_end",
          interval
        };
      }
    }

    if (
      [JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_REPORT, JENKINS_REPORTS.SCM_PRS_REPORT].includes(selectedReport as any)
    ) {
      const modifiedAcross = value.substring(0, value.lastIndexOf("_"));
      if (SCM_PRS_TIME_FILTERS_KEYS.includes(modifiedAcross)) {
        const intervalFromValue = value.substring(value.lastIndexOf("_") + 1, value.length);
        query = {
          ...query,
          across: modifiedAcross,
          interval: intervalFromValue || "week"
        };
      }
    }

    if (["github_commits_report", "scm_rework_report"].includes(selectedReport as any)) {
      const modifiedAcross = value.substring(0, value.lastIndexOf("_"));
      if (["trend"].includes(modifiedAcross)) {
        const intervalFromValue = value.substring(value.lastIndexOf("_") + 1, value.length);
        query = {
          ...query,
          across: modifiedAcross,
          interval: intervalFromValue || "week"
        };
      } else {
        if (query?.interval) {
          delete query?.interval;
        }
      }
    }

    if (
      [
        AppNames.ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT,
        "resolution_time_report",
        "tickets_report",
        "zendesk_tickets_report",
        "scm_issues_time_resolution_report",
        "scm_issues_time_across_stages_report",
        AppNames.ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT
      ].includes(selectedReport)
    ) {
      const modifiedAcross = value.substring(0, value.lastIndexOf("_"));
      if (
        [
          ...AZURE_TIME_FILTERS_KEYS,
          ...GROUP_BY_TIME_FILTERS,
          "issue_last_closed",
          "ticket_created",
          "resolved_at",
          "created_at",
          "issue_closed"
        ].includes(modifiedAcross)
      ) {
        const intervalFromValue = value.substring(value.lastIndexOf("_") + 1, value.length);
        query = {
          ...query,
          across: modifiedAcross,
          interval: intervalFromValue || "day"
        };
      }
    }

    if (selectedReport === LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_TYPE_REPORT) {
      delete query.across;
      query = {
        ...query,
        stacks: [value]
      };
    }

    if (selectedReport === PAGERDUTY_REPORT.RESPONSE_REPORTS) {
      query = {
        ...query,
        stacks: query.stacks?.filter((f: string) => f !== value)
      };
    }

    if (getWidgetConstant(ALLOWED_WIDGET_DATA_SORTING)) {
      const prevDefaultSortingNode = widgetFilterOptionsNode(
        get(data.query, ["across"], ""),
        customeDateTimeKeysFields
      );
      const curDefaultSortingNode = widgetFilterOptionsNode(query.across, customeDateTimeKeysFields);
      if (prevDefaultSortingNode !== curDefaultSortingNode) {
        query[WIDGET_DATA_SORT_FILTER_KEY] = widgetDataSortingOptionsDefaultValue[curDefaultSortingNode];
      }
    }

    if (appendDeleteQuery && appendDeleteQuery.hasOwnProperty("appendQuery")) {
      query = {
        ...query,
        ...appendDeleteQuery?.appendQuery
      };
    }

    if (appendDeleteQuery && appendDeleteQuery.hasOwnProperty("deleteQuery")) {
      const deleteKeys = get(appendDeleteQuery, ["deleteQuery"], []);
      deleteKeys.forEach((key: string) => {
        unset(query, [key]);
      });
    }
    data.query = query;
    setWidgetData(data);
    props.updateWidget(data);
  };

  const handleWeight = (value: any, hygiene: any) => {
    let data = widgetData;
    const key = typeof hygiene === "object" ? hygiene.type : hygiene;
    let weights = {
      ...(data.weights || {}),
      [key]: value
    };

    if (typeof hygiene === "object") {
      data.custom_hygienes = uniq([...(data.custom_hygienes || []), hygiene.id]);
    }

    const totalWeights = Object.keys(weights).reduce((acc, obj) => {
      acc = acc + weights[obj];
      return acc;
    }, 0);
    data.weights = weights;
    setWidgetData(data);
    props.updateWidget(data);
    if (totalWeights > 100) {
      setWeightError("Weight cannot add up to more than 100");
    } else {
      setWeightError("");
    }
  };

  const handleTimeRangeTypeChange = (key: string, value: any) => {
    setWidgetData((_data: any) => {
      let metadata = {
        ...(_data.metadata || {}),
        range_filter_choice: {
          ...(_data.metadata?.range_filter_choice || {}),
          [key]: value
        }
      };
      _data.metadata = metadata;
      setMetadata(metadata);
      props.updateWidget(_data);
      return _data;
    });
  };

  const handleSingleStatTimeFilterTypeChanged = (value: string, removeKey: string) => {
    const currentKey = value.includes("_at") ? value : `${value}_at`;
    const currentMetaKey = get(rangeMap, currentKey, currentKey);
    const removeMetaKey = get(rangeMap, removeKey, removeKey);

    setWidgetData((_data: any) => {
      _data.query = {
        ...(_data.query || {}),
        across: value,
        [currentKey]: { $gt: "30", $lt: "30" }
      };

      delete _data?.query?.[removeKey];

      let metadata = {
        ...(_data.metadata || {}),
        range_filter_choice: {
          ...(_data.metadata?.range_filter_choice || {}),
          [currentMetaKey]: { type: "relative", relative: { next: { unit: "today" }, last: { unit: "days", num: 30 } } }
        }
      };

      const dashBoard_time_keys = get(_data, ["_metadata", "dashBoard_time_keys"], undefined);

      if (dashBoard_time_keys) {
        delete metadata?.dashBoard_time_keys?.[removeKey];
      }

      delete metadata?.range_filter_choice?.[removeMetaKey];

      _data.metadata = metadata;
      setMetadata(metadata);
      props.updateWidget(_data);
      return _data;
    });
  };

  const handleLastSprintChange = (value: boolean, filterKey: string) => {
    let data = widgetData;
    let query = {
      ...(data.query || {})
    };

    query["last_sprint"] = value;
    if (value) {
      unset(query, ["jira_sprint_states"]);
      unset(query, ["partial_match", filterKey]);
      unset(query, ["custom_fields", filterKey]);
      query["sprint_count"] = 1;
    } else {
      unset(query, ["sprint_count"]);
    }

    if (!data?.metadata?.last_sprint_key || data?.metadata?.last_sprint_key !== filterKey) {
      let metadata = {
        ...(data.metadata || {}),
        [SPRINT_FILTER_META_KEY]: filterKey
      };
      data.metadata = metadata;
      setMetadata(metadata);
    }

    data.query = query;
    setWidgetData(data);
    props.updateWidget(data);
  };

  const handleFilterRemove = (key: string, returnUpdatedQuery?: boolean, isCustomSprint?: boolean) => {
    let data = widgetData;
    let query = {
      ...(data.query || {})
    };
    let _key = key;
    const filterConfig = reportFilterConfig;
    const currentConfig = filterConfig?.find((config: LevelOpsFilter) => config.beKey === _key);
    if (_key === "stat_time_based") {
      if (filterConfig && currentConfig) {
        const filterKey = ((currentConfig as LevelOpsFilter)?.filterMetaData as StatTimeRangeFilterData)?.filterKey;
        if (filterKey) {
          if (typeof filterKey === "string") _key = filterKey;
          else _key = filterKey?.({ allFilters: filters });
        }
      }
    }

    let getExcludeWithPartialMatchKey = getWidgetConstant("getExcludeWithPartialMatchKey");
    let getExcludeWithPartialMatchKeyFlag =
      typeof getExcludeWithPartialMatchKey === "function" ? getExcludeWithPartialMatchKey(currentConfig.beKey) : false;

    query = removeFilterKey(
      query,
      _key,
      isCustomSprint,
      "",
      getExcludeWithPartialMatchKeyFlag,
      currentConfig?.partialKey
    );
    if (returnUpdatedQuery) return query;
    data.query = query;
    setWidgetData(data);
    props.updateWidget(data);
  };

  const handleChildFilterRemove = (filterpayload: any, returnUpdatedQuery?: boolean) => {
    const key = filterpayload.value;
    let data = widgetData;
    let query = {
      ...(data.query || {})
    };
    query = removeChildFilterKey(query, filterpayload.parentKey, key);
    if (returnUpdatedQuery) return query;
    data.query = query;
    setWidgetData(data);
    props.updateWidget(data);
  };

  const handleBulkAddToMetadata = (metaDataToBeMerged: { [x: string]: any }, filterPayload?: any) => {
    let data = cloneDeep(widgetData);
    let nMetaData = cloneDeep(metadata);
    nMetaData = {
      ...(nMetaData || {}),
      ...(metaDataToBeMerged || {})
    };
    let query = { ...(data.query || {}) };
    if (filterPayload) {
      if (filterPayload.type === "remove") {
        if (filterPayload?.childKeys && filterPayload?.childKeys.length > 0) {
          query = filterPayload?.childKeys.map((data: any) => handleFilterRemove(data, false));
          query = handleFilterRemove(filterPayload.value, true, filterPayload.isCustomSprint);
        } else {
          query = handleFilterRemove(filterPayload.value, true, filterPayload.isCustomSprint);
        }
        unset(nMetaData, [RANGE_FILTER_CHOICE, get(rangeMap, filterPayload.value, filterPayload.value)]);
        unset(nMetaData, ["dashBoard_time_keys", filterPayload.value]);
      } else if (filterPayload.type === "childFilterRemove") {
        query = handleChildFilterRemove(filterPayload, true);
      } else {
        query = handleFilterSelectChange(filterPayload.value, filterPayload.type, undefined, undefined, true);
      }
    }
    data.query = sanitizeObject(query);
    data.metadata = nMetaData;
    props.updateWidget(data);
    setWidgetData(data);
    setMetadata(nMetaData);
  };

  const widgetWeights = { ...widgetData.weights };

  const graphType: string = widgetData.widget_type || "";

  return (
    <div>
      <AntRow type={"flex"} justify={"space-between"}>
        <AntCol span={24}>
          <div className="p-10">
            <Form layout="vertical">
              {selectedReport && (
                <>
                  {getWidgetConstant("application") ? (
                    getWidgetConstant("application") !== "levelops" ? (
                      <>
                        <WidgetFiltersContainer
                          key={reportFilterConfig}
                          widgetId={widgetData?.id}
                          report={selectedReport}
                          graphType={graphType}
                          maxRecords={widgetData.max_records}
                          onAggregationAcrossSelection={handleAggregationAcrossSelection}
                          onMaxRecordsSelection={handleMaxRecords}
                          onMetadataChange={handleMetadataChange}
                          onWeightChange={handleWeight}
                          weightError={weightError}
                          widgetWeights={widgetWeights}
                          integrationIds={integrationIds}
                          filters={filters}
                          onExcludeFilterChange={handleExcludeFilter}
                          onFilterValueChange={handleFilterSelectChange}
                          handlePartialValueChange={handlePartialFilters}
                          handleTimeRangeTypeChange={handleTimeRangeTypeChange}
                          onFilterRemoved={handleFilterRemove}
                          onChildFilterRemove={handleChildFilterRemove}
                          handleTimeRangeFilterValueChange={handleTimeFilterChange}
                          metaData={metadata}
                          dashboardMetaData={dashboardMetaData}
                          handleLastSprintChange={handleLastSprintChange}
                          partialFiltersErrors={partialFilterError}
                          onSingleStatTypeFilterChange={handleSingleStatTimeFilterTypeChanged}
                          onModifiedFilterValueChange={handleModifiedFilterValueChange}
                          handleBulkAddToMetadata={handleBulkAddToMetadata}
                          isMultiTimeSeriesReport={props.widgetData.isMultiTimeSeriesReport}
                          isParentTabData={props?.isParentTabData}
                          advancedTabState={props?.advancedTabState}
                          currentFilterKeyValueChange={currentFilterKeyValueChange}
                        />
                      </>
                    ) : (
                      <DynamicGraphPaginatedFilters
                        selectedReport={selectedReport}
                        filters={filters}
                        metaData={metadata}
                        onMetadataChange={handleMetadataChange}
                        onFilterValueChange={handleFilterSelectChange}
                        maxRecords={widgetData.max_records}
                        onMaxRecordsSelection={handleMaxRecords}
                      />
                    )
                  ) : null}
                </>
              )}
            </Form>
          </div>
        </AntCol>
      </AntRow>
    </div>
  );
};

export default ConfigureSingleWidget;
