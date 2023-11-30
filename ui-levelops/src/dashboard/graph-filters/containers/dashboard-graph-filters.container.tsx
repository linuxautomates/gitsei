import { Spin } from "antd";
import { RestWidget } from "classes/RestDashboards";
import { aggregationMappingsForMultiTimeSeriesReport } from "dashboard/constants/applications/multiTimeSeries.application";
import * as AppName from "dashboard/constants/applications/names";
import {
  allSprintMetricsReport,
  azureIterationSupportableReports,
  azureSprintReports,
  AZURE_LEAD_TIME_ISSUE_REPORT,
  ISSUE_MANAGEMENT_REPORTS,
  LEAD_TIME_REPORTS,
  PAGERDUTY_REPORT,
  scmCicdReportTypes
} from "dashboard/constants/applications/names";
import {
  azureBAReports,
  extraReportWithTicketCategorizationFilter,
  jiraBAReports,
  jiraBAStatReports,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { DEFAULT_SCM_SETTINGS_OPTIONS } from "dashboard/constants/defaultFilterOptions";
import { azureBAReportTypes } from "dashboard/constants/enums/azure-ba-reports.enum";
import {
  EffortUnitType,
  jiraAzureBADynamicSupportedFiltersReports,
  jiraBAReportTypes
} from "dashboard/constants/enums/jira-ba-reports.enum";
import { IGNORE_FILTER_KEYS_CONFIG } from "dashboard/constants/filter-key.mapping";
import { ReportsApplicationType } from "dashboard/constants/helper";
import { BACommitSupportedFIlters } from "dashboard/constants/supported-filters.constant";
import { GLOBAL_SETTINGS_UUID } from "dashboard/constants/uuid.constants";
import { ignoreFilterKeysType } from "dashboard/dashboard-types/common-types";
import { DashboardWidgetResolverContext, WidgetTabsContext } from "dashboard/pages/context";
import { get, isArray, uniqBy } from "lodash";
import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import { fieldListDataSelector } from "reduxConfigs/selectors/fields-list.selector";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { AntText } from "shared-resources/components";
import { WIDGET_CONFIGURATION_KEYS } from "../../../constants/widgets";
import { useIsChildWidget, useSupportedFilters } from "../../../custom-hooks";
import { useSprintReportFilters } from "../../../custom-hooks/useSprintReportFilters";
import {
  jiraTicketsTrendReportOptions,
  leadTimeReportXAxisOptions,
  scmMergeTrendTypes,
  sprintReportXAxisOptions
} from "../../constants/constants";
import widgetConstants from "../../constants/widgetConstants";
import { DashboardGraphFilters, PagerDutyFilters } from "../components";
import BAWidgetFiltersWrapper from "../components/BAWidgetFiltersWrapper";
import { azureTicketsReportTimeAcrossOptions, jiraTicketsReportTimeAcrossOptions } from "../components/Constants";
import DevProductivityFilters from "../components/DevProductivityFilters/DevProductivityFilters";
import { updateIssueCreatedAndUpdatedFilters } from "../components/updateFilter.helper";
import OUFiltersComponent from "../components/OUFilters/OUFilters.component";
import { stringSortingComparator } from "../components/sort.helper";
import HygieneFiltersContainer from "./hygiene-filters.container";
import { GITHUB_CODING_DAYS_REPORT_ACROSS } from "./x-axis-constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface DashboardGraphsFiltersContainerProps {
  widgetId: string;
  reportType: string;
  integrationIds: Array<string>;
  onWeightChange: (value: any, type: any) => void;
  onFilterValueChange: (value: any, type: any, exclude?: boolean, addToMetaData?: any) => void;
  onMetadataChange: (value: any, type: any, reportType?: String) => void;
  onTimeFilterValueChange?: (value: any, type?: any, rangeType?: string, isCustom?: boolean) => void;
  onExcludeChange?: (key: string, value: boolean) => void;
  onAggregationAcrossSelection?: (value: any) => void;
  onMaxRecordsSelection?: (value: any) => void;
  onTimeRangeTypeChange?: (key: string, value: any) => void;
  onPartialChange: (key: string, value: any) => void;
  partialFilterError?: any;
  widgetWeights: any;
  weightError: string;
  filters: any;
  maxRecords?: any;
  graphType?: string;
  metaData?: any;
  handleLastSprintChange?: (value: boolean, filterKey: string) => void;
  onModifieldFilterValueChange?: (payload: any) => void;
  dashboardMetaData?: any;
  onSingleStatTypeFilterChange: (value: string, removeKey: string) => void;
  queryParamDashboardOUId?: any;
}

const DashboardGraphsFiltersContainer: React.FC<DashboardGraphsFiltersContainerProps> = (
  props: DashboardGraphsFiltersContainerProps
) => {
  const {
    reportType,
    filters,
    integrationIds,
    onFilterValueChange,
    onMetadataChange,
    onMaxRecordsSelection,
    onAggregationAcrossSelection,
    onPartialChange,
    weightError,
    widgetWeights,
    maxRecords,
    onExcludeChange,
    onTimeRangeTypeChange,
    metaData,
    onTimeFilterValueChange,
    handleLastSprintChange,
    onModifieldFilterValueChange,
    widgetId,
    dashboardMetaData,
    onSingleStatTypeFilterChange,
    queryParamDashboardOUId
  } = props;

  const { dashboardId } = useContext(DashboardWidgetResolverContext);

  const { isVisibleOnTab } = useContext(WidgetTabsContext);

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });

  const globalSettingsState = useParamSelector(getGenericRestAPISelector, {
    uri: "configs",
    method: "list",
    uuid: GLOBAL_SETTINGS_UUID
  });
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

  const isCompositeChild = useIsChildWidget(dashboardId, widgetId);

  const getWidgetConstant = useCallback(
    (data: any) => get(widgetConstants, [reportType, data], undefined),
    [reportType]
  );

  const [aggregationAcrossOptions, setAggregationAcrossOptions] = useState<Array<any>>([]);
  const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
  const supportedFilters = useMemo(() => {
    let supported_filters = getWidgetConstant("supported_filters");
    if (jiraAzureBADynamicSupportedFiltersReports.includes(reportType as any)) {
      if (uriUnit && [EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
        supported_filters = BACommitSupportedFIlters;
      }
    }
    return supported_filters;
  }, [reportType, uriUnit]);

  const application = getWidgetConstant("application");
  const {
    loading: apiLoading,
    error: hasError,
    apiData,
    has_next
  } = useSupportedFilters(
    supportedFilters,
    integrationIds,
    application,
    // quick fix for now
    // some across options are hardcoded and some hardcoded
    // options need to append to this apiData , so if two reports have same filters uri
    // then acorss options are not changing in the UI , so need to force api call
    [reportType]
  );

  const fieldList = useParamSelector(fieldListDataSelector, { application });

  const getLoading = useMemo(
    () => (
      <div className="centered m-15">
        <Spin />
      </div>
    ),
    []
  );

  const sprintReportFilters = useMemo(() => {
    const updatedFilters = updateIssueCreatedAndUpdatedFilters({ filter: filters }, metaData, reportType);
    const completedAt = get(updatedFilters, ["filter", "completed_at"], undefined);
    if (completedAt) {
      return {
        ...filters,
        completed_at: completedAt
      };
    }

    return filters;
  }, [reportType, filters, metaData]);

  const completedAt = useMemo(() => filters.completed_at, [filters]);

  const showOUFilters =
    !!(props?.queryParamDashboardOUId || dashboardMetaData?.ou_ids || []).length &&
    (["jira", "jenkins", "github", "azure_devops", "jenkinsgithub", "githubjira"].includes(application) ||
      [PAGERDUTY_REPORT.RESPONSE_REPORTS].includes(reportType as any));

  const {
    loading: sprintFiltersLoading,
    error: sprintError,
    sprintApiData
  } = useSprintReportFilters(application, sprintReportFilters, [reportType, completedAt]);

  useEffect(() => {
    if (!apiLoading && apiData && getWidgetConstant("xaxis")) {
      let data: any;
      if (
        (["jenkinsgithub", "pagerduty", "praetorian", "nccgroup", "snyk", "azure_devops"].includes(application) &&
          ![...scmCicdReportTypes, ...azureBAReports, ...azureSprintReports].includes(reportType as any)) ||
        [
          "github_prs_report_trends",
          "github_issues_report_trends",
          "sonarqube_metrics_report",
          "github_issues_first_response_report_trends",
          "github_prs_single_stat",
          "github_prs_response_time_single_stat",
          "github_issues_count_single_stat",
          "sonarqube_metrics_trend_report",
          "github_issues_first_response_count_single_stat",
          "cicd_jobs_count_report",
          "cicd_scm_jobs_duration_report"
        ].includes(reportType) ||
        scmMergeTrendTypes.includes(reportType)
      ) {
        data = getWidgetConstant("across").map((item: any) => {
          return {
            label: item?.replace(/_/g, " ")?.toUpperCase(),
            value: item
          };
        });

        if (
          [ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT, ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT_TREND].includes(
            reportType as ISSUE_MANAGEMENT_REPORTS
          )
        ) {
          data.push(
            { label: "Workitem Created", value: "workitem_created_at" },
            { label: "Workitem Updated", value: "workitem_updated_at" }
          );
        }

        if (application === IntegrationTypes.AZURE) {
          if (
            [ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT, ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT].includes(
              reportType as any
            )
          ) {
            data.push(...azureTicketsReportTimeAcrossOptions);
          }
          const fields = apiData.find((item: any) => Object.keys(item)[0] === "custom_fields");
          if (fields && fields.hasOwnProperty("custom_fields")) {
            data.push(...fields.custom_fields.map((field: any) => ({ label: field.name, value: field.key })));
          }
        }
      } else {
        data = getSupportedFiltersData().map((item: any) => ({
          label: (Object.keys(item)[0] || "").replace(/_/g, " ")?.toUpperCase(),
          value: Object.keys(item)[0]
        }));

        if (
          [ReportsApplicationType.JIRA, ReportsApplicationType.JIRA_ZENDESK, ReportsApplicationType.ZENDESK].includes(
            application
          ) ||
          [LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT, AZURE_LEAD_TIME_ISSUE_REPORT.LEAD_TIME_SINGLE_STAT].includes(
            reportType as any
          )
        ) {
          const fields = apiData.find((item: any) => Object.keys(item)[0] === "custom_fields");
          if (fields && fields.hasOwnProperty("custom_fields")) {
            data.push(...fields.custom_fields.map((field: any) => ({ label: field.name, value: field.key })));
          }
        }

        if (application === IntegrationTypes.JIRA) {
          if (reportType === "resolution_time_report") {
            data.push(
              { label: "Issue Last Closed Week", value: "issue_resolved_week" },
              { label: "Issue Last Closed Month", value: "issue_resolved_month" },
              { label: "Issue Last Closed Quarter", value: "issue_resolved_quarter" }
            );
          } else if (reportType === "jira_time_across_stages") {
            data = data.filter((item: any) => item.value !== "status");
            data = [{ label: "None", value: "none" }, ...data];
          } else if (reportType === "tickets_report") {
            data.push(...jiraTicketsReportTimeAcrossOptions);
          } else {
            data.push(
              { label: "Issue Created", value: "issue_created" },
              { label: "Issue Updated", value: "issue_updated" }
            );
          }
        }

        if (application === IntegrationTypes.ZENDESK && reportType === "zendesk_tickets_report") {
          data.push(
            { label: "Ticket Created By Date", value: "ticket_created_day" },
            { label: "Ticket Created By Week", value: "ticket_created_week" },
            { label: "Ticket Created By Month", value: "ticket_created_month" }
          );
        }

        if (scmCicdReportTypes.includes(reportType as any)) {
          data = [
            { label: "Job End Day", value: "job_end_day" },
            { label: "Job End Week", value: "job_end_week" },
            { label: "Job End Month", value: "job_end_month" },
            { label: "Job End Quarter", value: "job_end_quarter" }
          ];
        }
        if (["scm_issues_time_resolution_report"].includes(reportType)) {
          data = [
            { label: "Project", value: "project" },
            { label: "Label", value: "label" },
            { label: "Repo", value: "repo_id" },
            { label: "Assignee", value: "assignee" },
            { label: "Issue Created By Date", value: "issue_created" },
            { label: "Issue Updated By Date", value: "issue_updated" },
            { label: "Issue Last Closed Week", value: "issue_created_week" },
            { label: "Issue Last Closed Month", value: "issue_created_month" },
            { label: "Issue Last Closed Quarter", value: "issue_created_quarter" }
          ];
        }
        if (["scm_issues_time_across_stages_report"].includes(reportType)) {
          data = [
            { label: "Historical Status", value: "column" },
            { label: "Project", value: "project" },
            { label: "Issue Created By Date", value: "issue_created_day" },
            { label: "Issue Created By Week", value: "issue_created_week" },
            { label: "Issue Created By Month", value: "issue_created_month" },
            { label: "Repo", value: "repo_id" },
            { label: "Label", value: "label" },
            { label: "Assignee", value: "assignee" },
            { label: "Issue Closed By Date", value: "issue_closed_day" },
            { label: "Issue Closed By Week", value: "issue_closed_week" },
            { label: "Issue Closed By Month", value: "issue_closed_month" }
          ];
        }
        if (["github_prs_response_time_report"].includes(reportType)) {
          data = [
            { label: "Project", value: "project" },
            { label: "Repo ID", value: "repo_id" },
            { label: "Branch", value: "branch" },
            { label: "Author", value: "author" },
            { label: "Reviewer", value: "reviewer" },
            { label: "PR Closed By Week", value: "pr_closed_week" },
            { label: "PR Closed By Month", value: "pr_closed_month" },
            { label: "PR Closed By Quarter", value: "pr_closed_quarter" }
          ];
        }

        if (["github_coding_days_report"].includes(reportType)) {
          data = GITHUB_CODING_DAYS_REPORT_ACROSS;
        }
      }
      setAggregationAcrossOptions(data);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiLoading, apiData]);

  const getAcrossOptions = () => {
    const appendAcrossOptions = getWidgetConstant("appendAcrossOptions");
    let acrossOptions = [
      jiraBAReportTypes.JIRA_PROGRESS_REPORT,
      jiraBAReportTypes.JIRA_BURNDOWN_REPORT,
      azureBAReportTypes.AZURE_ISSUES_PROGRESS_REPORT,
      azureBAReportTypes.AZURE_PROGRAM_PROGRESS_REPORT
    ].includes(reportType as any)
      ? []
      : aggregationAcrossOptions;
    if (["github_commits_report"].includes(reportType as any)) {
      const aggFilters = [
        { label: "Code Change Size", value: "code_change" },
        { label: "Committed in Week", value: "trend_week" },
        { label: "Committed in Month", value: "trend_month" },
        { label: "Committed in Quarter", value: "trend_quarter" },
        { label: "By Day of Week", value: "trend_day" }
      ];
      acrossOptions = [...acrossOptions, ...aggFilters];
    }

    if (["scm_rework_report"].includes(reportType as any)) {
      const aggFilters = [
        { label: "Commit Week", value: "trend_week" },
        { label: "Commit Month", value: "trend_month" }
      ];
      acrossOptions = [...acrossOptions, ...aggFilters];
    }

    if (
      application === IntegrationTypes.GITHUB &&
      acrossOptions &&
      ["github_commits_report"].includes(reportType as any) &&
      !scmMergeTrendTypes.includes(reportType)
    ) {
      if (!acrossOptions.includes("technology")) {
        acrossOptions.push("technology");
      }
    }

    if (application === IntegrationTypes.JIRA) {
      acrossOptions = acrossOptions.map((option: any) => {
        const mapping = getWidgetConstant("filterOptionMap");
        return { ...option, label: get(mapping, [option.value], option.label) };
      });
    }

    if (
      application === IntegrationTypes.JIRA &&
      reportType === "tickets_report" &&
      acrossOptions &&
      !acrossOptions.find(f => f.value === "epic")
    ) {
      // add epics
      acrossOptions && acrossOptions.push({ label: "EPIC", value: "epic" });
    } else if (
      application === IntegrationTypes.JIRA &&
      reportType !== "tickets_report" &&
      acrossOptions &&
      acrossOptions.find(f => f.value === "epics")
    ) {
      // remove epics
      const index = acrossOptions.findIndex(f => f.value === "epic");
      acrossOptions = index > 0 ? [...acrossOptions.slice(0, index), ...acrossOptions.slice(index + 1)] : [];
    }

    if (application === IntegrationTypes.JIRA && reportType === "tickets_report_trends") {
      acrossOptions = jiraTicketsTrendReportOptions;
    }

    if (application === IntegrationTypes.JIRA && reportType === "hygiene_report") {
      acrossOptions = ["epic", "status", "issue_type", "priority", "project", "assignee", "reporter"].map(r => ({
        label: (r || "").toUpperCase().replace("_", " "),
        value: r
      }));
    }

    if (application === AppName.MICROSOFT_APPLICATION_NAME && reportType === AppName.MICROSOFT_ISSUES_REPORT_NAME) {
      acrossOptions = acrossOptions.filter(option => {
        if (option.value === "model") return false;
        if (filters?.stacks && filters.stacks.includes(option.value)) return false;

        return true;
      });
    }

    if (isArray(appendAcrossOptions) && appendAcrossOptions.length) {
      acrossOptions = acrossOptions.filter(option => !appendAcrossOptions.find(item => item.value === option.value));
      acrossOptions = [...acrossOptions, ...appendAcrossOptions].map(item => ({
        ...item,
        label: (item.label || "").toUpperCase()
      }));
    }
    if (
      [
        "sprint_metrics_trend",
        "sprint_metrics_percentage_trend",
        "azure_sprint_metrics_percentage_trend",
        "azure_sprint_metrics_trend"
      ].includes(reportType)
    ) {
      acrossOptions = sprintReportXAxisOptions;
    }

    if (reportType === LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_TYPE_REPORT) {
      acrossOptions = leadTimeReportXAxisOptions;
    }
    if (widget.isMultiTimeSeriesReport) {
      acrossOptions = aggregationMappingsForMultiTimeSeriesReport(reportType);
    }
    acrossOptions.sort(stringSortingComparator());
    return uniqBy(acrossOptions, "value");
  };

  const getSupportedFiltersData = useCallback(() => {
    return (apiData || [])
      .map((item: any) => {
        if (isArray(supportedFilters)) {
          for (let _filter of supportedFilters) {
            if (_filter.values.includes(Object.keys(item)[0])) {
              return item;
            }
          }
        } else {
          if (supportedFilters && supportedFilters.values?.includes(Object.keys(item)[0])) {
            return item;
          }
        }
      })
      .filter(item => item !== undefined);
  }, [supportedFilters, apiData]);

  const getCustomFieldData = () => {
    // filter by custom fields here
    const fields = apiData.find((item: any) => Object.keys(item)[0] === "custom_fields");
    if (fields && fields.hasOwnProperty("custom_fields")) {
      // eslint-disable-next-line array-callback-return
      const customData: any[] = [];
      const ignoreFilterKeysConfig: ignoreFilterKeysType = getWidgetConstant(IGNORE_FILTER_KEYS_CONFIG);
      const ignoreCustomFieldKeys = ignoreFilterKeysConfig?.ignoreCustomFilterKeys || [];
      fields.custom_fields.forEach((field: any) => {
        if (ignoreCustomFieldKeys.includes((field?.name || "").toLowerCase())) return;
        const valuesRecord = apiData.find(item => Object.keys(item)[0] === field.key);
        if (valuesRecord) {
          const options: { key: string }[] = valuesRecord[Object.keys(valuesRecord)[0]];
          const customDatum = {
            name: field.name,
            key: field.key,
            values: options
          };
          customData.push(customDatum);
        }
      });
      return customData;
    } else return [];
  };

  const getSprintData = useCallback(() => {
    if (azureIterationSupportableReports.includes(reportType as any)) {
      if (reportType.includes("azure_")) {
        const values = sprintApiData.map((item: any) => ({
          key: item.name,
          parent_key: item.parent_sprint
        }));
        values.sort(stringSortingComparator());
        return [
          {
            ["azure_iteration@Azure Iteration"]: values || []
          }
        ];
      } else if (allSprintMetricsReport.includes(reportType as any)) {
        const values = sprintApiData.map((item: any) => ({
          key: item.name
        }));
        values.sort(stringSortingComparator());
        return [
          {
            "sprint_report@Sprint Report": values || []
          }
        ];
      }
    }
    return [];
  }, [sprintApiData]);

  const getCustomHygienes = () => {
    const cHygienes = apiData.find((item: any) => Object.keys(item)[0] === "custom_hygienes");
    let customHygieneOptions = cHygienes ? cHygienes.custom_hygienes : [];
    customHygieneOptions?.sort(stringSortingComparator());
    return customHygieneOptions;
  };

  const renderFilters = () => {
    if (application === "dev_productivity") {
      return (
        <DevProductivityFilters
          filters={filters}
          application={application}
          reportType={reportType}
          onFilterValueChange={onFilterValueChange}
          onMetadataChange={onMetadataChange}
          metaData={metaData}
        />
      );
    }

    if (
      [LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT, AZURE_LEAD_TIME_ISSUE_REPORT.LEAD_TIME_SINGLE_STAT].includes(
        reportType as any
      )
    ) {
      return (
        <>
          {apiData && (
            <DashboardGraphFilters
              application={application}
              customData={getCustomFieldData()}
              data={getSupportedFiltersData()}
              filters={filters}
              reportType={reportType}
              onFilterValueChange={onFilterValueChange}
              onMetadataChange={onMetadataChange}
              maxRecords={maxRecords}
              acrossOptions={getAcrossOptions()}
              onAggregationAcrossSelection={onAggregationAcrossSelection}
              onMaxRecordsSelection={onMaxRecordsSelection}
              applicationUse={false}
              onExcludeChange={onExcludeChange}
              onTimeRangeTypeChange={onTimeRangeTypeChange}
              metaData={metaData}
              onTimeFilterValueChange={onTimeFilterValueChange}
              onPartialChange={onPartialChange}
              partialFilterError={props.partialFilterError}
              hasNext={has_next}
              sprintData={getSprintData()}
              integrationIds={integrationIds}
              isMultiTimeSeriesReport={widget.isMultiTimeSeriesReport}
              isCompositeChild={isCompositeChild}
              dashboardMetaData={dashboardMetaData}
              fieldTypeList={fieldList}
              scmGlobalSettings={scmGlobalSettings}
              onSingleStatTypeFilterChange={onSingleStatTypeFilterChange}
            />
          )}
          {showOUFilters && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
            <OUFiltersComponent
              customFieldData={getCustomFieldData()}
              reportType={reportType}
              metaData={props.metaData}
              onMetadataChange={onMetadataChange}
            />
          )}
        </>
      );
    }

    if (application) {
      switch (application) {
        case "githubjira":
        case "jenkinsgithub":
        case "github":
        case "jenkins":
        case "sonarqube":
        case "praetorian":
        case "bullseye":
        case "nccgroup":
        case "snyk":
        case "coverity":
          return (
            <>
              {apiData && (
                <DashboardGraphFilters
                  application={application}
                  data={apiData}
                  filters={filters}
                  reportType={reportType}
                  onFilterValueChange={onFilterValueChange}
                  onMetadataChange={onMetadataChange}
                  acrossOptions={getAcrossOptions()}
                  maxRecords={maxRecords}
                  onAggregationAcrossSelection={onAggregationAcrossSelection}
                  onMaxRecordsSelection={onMaxRecordsSelection}
                  applicationUse={false}
                  onExcludeChange={onExcludeChange}
                  onTimeRangeTypeChange={onTimeRangeTypeChange}
                  metaData={metaData}
                  onTimeFilterValueChange={onTimeFilterValueChange}
                  onPartialChange={onPartialChange}
                  partialFilterError={props.partialFilterError}
                  hasNext={has_next}
                  handleLastSprintChange={handleLastSprintChange}
                  integrationIds={integrationIds}
                  onModifieldFilterValueChange={onModifieldFilterValueChange}
                  isMultiTimeSeriesReport={widget.isMultiTimeSeriesReport}
                  isCompositeChild={isCompositeChild}
                  dashboardMetaData={dashboardMetaData}
                  fieldTypeList={fieldList}
                  scmGlobalSettings={scmGlobalSettings}
                  onSingleStatTypeFilterChange={onSingleStatTypeFilterChange}
                />
              )}
              {showOUFilters && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
                <OUFiltersComponent
                  customFieldData={getCustomFieldData()}
                  reportType={reportType}
                  metaData={props.metaData}
                  onMetadataChange={onMetadataChange}
                />
              )}
            </>
          );
        case "pagerduty":
          switch (reportType) {
            case "pagerduty_hotspot_report":
            case "pagerduty_release_incidents":
            case "pagerduty_ack_trend":
            case "pagerduty_after_hours":
            case "pagerduty_incident_report_trends":
            case PAGERDUTY_REPORT.RESPONSE_REPORTS:
              return (
                <>
                  {apiData && (
                    <>
                      <PagerDutyFilters
                        reportType={reportType}
                        data={apiData}
                        onFilterValueChange={onFilterValueChange}
                        filters={filters}
                        acrossOptions={getAcrossOptions()}
                        onAggregationAcrossSelection={onAggregationAcrossSelection}
                        onTimeRangeTypeChange={onTimeRangeTypeChange}
                        metaData={metaData}
                        onTimeFilterValueChange={onTimeFilterValueChange}
                        dashboardMetaData={dashboardMetaData}
                        onMetadataChange={onMetadataChange}
                        isMultiTimeSeriesReport={widget.isMultiTimeSeriesReport}
                        isCompositeChild={isCompositeChild}
                      />
                      {showOUFilters && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
                        <OUFiltersComponent
                          customFieldData={getCustomFieldData()}
                          reportType={reportType}
                          metaData={props.metaData}
                          onMetadataChange={onMetadataChange}
                        />
                      )}
                    </>
                  )}
                </>
              );

            default:
              return (
                <div style={{ display: "flex", justifyContent: "center" }}>
                  <AntText style={{ fontSize: "12px" }}>This Report Type doesn't support any filters for now</AntText>
                </div>
              );
          }
        case AppName.MICROSOFT_APPLICATION_NAME:
        case "jirazendesk":
        case "jirasalesforce":
        case "jira":
        case "zendesk":
        case "testrails":
        case "azure_devops":
        case "salesforce": {
          switch (reportType) {
            case "hygiene_report":
            case "hygiene_report_trends":
            case "zendesk_hygiene_report":
            case "zendesk_hygiene_report_trends":
            case "salesforce_hygiene_report":
            case "azure_hygiene_report":
            case "azure_hygiene_report_trends":
              return (
                <>
                  {apiData && (
                    <HygieneFiltersContainer
                      data={getSupportedFiltersData()}
                      onFilterValueChange={onFilterValueChange}
                      onWeightChange={props.onWeightChange}
                      filters={filters}
                      application={application}
                      reportType={reportType}
                      acrossOptions={getAcrossOptions()}
                      weightError={weightError}
                      widgetWeights={widgetWeights}
                      customData={getCustomFieldData()}
                      customHygienes={getCustomHygienes()}
                      onExcludeChange={onExcludeChange}
                      onAggregationAcrossSelection={onAggregationAcrossSelection}
                      onTimeRangeTypeChange={onTimeRangeTypeChange}
                      metaData={metaData}
                      onTimeFilterValueChange={onTimeFilterValueChange}
                      onPartialChange={onPartialChange}
                      sprintData={getSprintData()}
                      partialFilterError={props.partialFilterError}
                      handleLastSprintChange={handleLastSprintChange}
                      onMetadataChange={onMetadataChange}
                      integrationIds={integrationIds}
                      isCompositeChild={isCompositeChild}
                      dashboardMetaData={dashboardMetaData}
                      fieldTypeList={fieldList}
                      queryParamDashboardOUId={queryParamDashboardOUId}
                    />
                  )}
                </>
              );
            default:
              return (
                <>
                  {apiData && (
                    <DashboardGraphFilters
                      application={application}
                      customData={getCustomFieldData()}
                      data={getSupportedFiltersData()}
                      filters={filters}
                      reportType={reportType}
                      onFilterValueChange={onFilterValueChange}
                      onMetadataChange={onMetadataChange}
                      maxRecords={maxRecords}
                      acrossOptions={getAcrossOptions()}
                      onAggregationAcrossSelection={onAggregationAcrossSelection}
                      onMaxRecordsSelection={onMaxRecordsSelection}
                      applicationUse={false}
                      onExcludeChange={onExcludeChange}
                      onTimeRangeTypeChange={onTimeRangeTypeChange}
                      metaData={metaData}
                      onTimeFilterValueChange={onTimeFilterValueChange}
                      onPartialChange={onPartialChange}
                      partialFilterError={props.partialFilterError}
                      hasNext={has_next}
                      sprintData={getSprintData()}
                      handleLastSprintChange={handleLastSprintChange}
                      integrationIds={integrationIds}
                      fieldTypeList={fieldList}
                      onModifieldFilterValueChange={onModifieldFilterValueChange}
                      isMultiTimeSeriesReport={widget.isMultiTimeSeriesReport}
                      isCompositeChild={isCompositeChild}
                      dashboardMetaData={dashboardMetaData}
                      scmGlobalSettings={scmGlobalSettings}
                      onSingleStatTypeFilterChange={onSingleStatTypeFilterChange}
                    />
                  )}
                  {[
                    ...jiraBAReports,
                    ...jiraBAStatReports,
                    ...azureBAReports,
                    ...extraReportWithTicketCategorizationFilter
                  ].includes(reportType as any) && (
                    <BAWidgetFiltersWrapper
                      reportType={reportType}
                      filters={filters}
                      metaData={metaData}
                      onFilterValueChange={onFilterValueChange}
                      onMetadataChange={onMetadataChange}
                      onTimeFilterValueChange={onTimeFilterValueChange}
                      dashboardMetadata={dashboardMetaData}
                    />
                  )}
                  {showOUFilters && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && (
                    <OUFiltersComponent
                      customFieldData={getCustomFieldData()}
                      reportType={reportType}
                      metaData={props.metaData}
                      onMetadataChange={onMetadataChange}
                    />
                  )}
                </>
              );
          }
        }
      }
    }
  };

  return <>{apiLoading ? getLoading : renderFilters()}</>;
};

export default DashboardGraphsFiltersContainer;
