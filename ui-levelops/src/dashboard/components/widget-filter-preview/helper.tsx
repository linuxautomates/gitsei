import { getAcrossValue } from "configurable-dashboard/helpers/helper";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  azureActiveWorkUnitOptions,
  azureEffortInvestmentUnitFilterOptions,
  jiraActiveWorkUnitOptions,
  ticketCategorizationUnitFilterOptions,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { azureBAReportTypes } from "dashboard/constants/enums/azure-ba-reports.enum";
import { METADATA_FILTERS_PREVIEW, WIDGET_FILTER_PREVIEW_COUNT } from "dashboard/constants/filter-name.mapping";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { buildMetricOptions, getOptionKey } from "dashboard/graph-filters/components/bullseye-filters/options.constant";
import { statSprintMetricsOptions } from "dashboard/graph-filters/components/sprintFilters.constant";
import { capitalize, get, isArray, uniqBy } from "lodash";
import { toTitleCase } from "utils/stringUtils";
import { sanitizeObject } from "../../../utils/commonUtils";
import * as AppName from "../../constants/applications/names";
import { jiraSprintMetricOptions, LEAD_TIME_REPORTS, scmCicdReportTypes } from "../../constants/applications/names";
import {
  AZURE_CUSTOM_FIELD_PREFIX,
  CUSTOM_FIELD_PREFIX,
  jiraTicketsTrendReportOptions,
  leadTimeReportXAxisOptions,
  scmMergeTrendTypes,
  sprintReportXAxisOptions
} from "../../constants/constants";
import widgetConstants from "../../constants/widgetConstants";
import { jiraBAReportTypes } from "../../constants/enums/jira-ba-reports.enum";
import {
  effortInvestmentTrendReportSampleInterval,
  jiraResolutionTimeMetricsOptions,
  jiraTicketsReportTimeAcrossOptions,
  jiraTimeAcrossStagesMetricOptions,
  leadTimeMetricOptions,
  scmCodingMetricsOptions,
  scmResolutionTimeMetricsOptions,
  sonarQubeMetricsOptions,
  ticketReportMetricOptions
} from "../../graph-filters/components/Constants";
import { excludeCaseHelper, globalCustomFieldFiltersHelper, partialCaseHelper } from "../dashboard-header/helper";
import { includeIntervalForWidgetFilterCount } from "./constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const graphFilterKeys = [
  "across",
  "across_limit",
  "calculation",
  "interval",
  "metric",
  "metrics",
  "stacks",
  "custom_stacks",
  "idle",
  "poor_description",
  "filter_across_values",
  "limit_to_only_applicable_data",
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY,
  "last_sprint"
];

const removeSnakeCase = (text: string) => text?.split("_").join(" ");

export const getFiltersCount = (filters: any, reportType: string, uri: string) => {
  let count = 0;
  const graphFilters = Object.keys(filters).filter((key: string) => {
    if (key === "across" && !get(widgetConstants, [reportType, "xaxis"], undefined)) {
      return false;
    } else if (key === "stacks" && !get(widgetConstants, [reportType, "stack_filters"], undefined)) {
      return false;
    } else if (key === "calculation" && reportType !== LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT) {
      return false;
    } else if (key === "interval" && !includeIntervalForWidgetFilterCount.includes(reportType as any)) {
      return false;
    } else if (["custom_stacks", "limit_to_only_applicable_data"].includes(key)) {
      return false;
    }
    return graphFilterKeys.includes(key);
  });

  count = count + graphFilters.length;
  const customFieldFilters = globalCustomFieldFiltersHelper({ [uri]: filters });

  if (Object.keys(customFieldFilters || {}).length) {
    count =
      count +
      Object.keys(sanitizeObject(customFieldFilters?.exclude || {})).length +
      Object.keys(sanitizeObject(customFieldFilters?.partial || {})).length +
      Object.keys(sanitizeObject(customFieldFilters?.normalcustomfields || {})).length +
      Object.keys(sanitizeObject(customFieldFilters?.missing_fields || {})).length;
  }
  const customFieldKeys = [
    ...Object.keys(customFieldFilters?.exclude || {}),
    ...Object.keys(customFieldFilters?.partial || {}),
    ...Object.keys(customFieldFilters?.normalcustomfields || {}),
    ...Object.keys(customFieldFilters?.missing_fields || {})
  ];

  if (["github_commits_filter_values", "github_prs_filter_values", "microsoft_issues_filter_values"].includes(uri)) {
    const partialFilters = Object.keys(filters).filter((key: string) => ["exclude", "partial_match"].includes(key));

    partialFilters.forEach((key: string) => {
      const value = filters[key];
      if (key === "exclude") {
        count = count + (excludeCaseHelper(value) || []).length;
      } else if (key === "partial_match") {
        count = count + (partialCaseHelper(value) || []).length;
      }
    });
    const filter = Object.keys(filters)
      .filter(
        (key: string) =>
          !graphFilterKeys.includes(key) && !customFieldKeys.includes(key) && !partialFilters.includes(key)
      )
      .reduce((acc: any, next: any) => {
        return {
          ...acc,
          [next]: get(filters, [uri, next], undefined)
        };
      }, {});

    count = count + Object.keys(filter).length;
  } else if (get(widgetConstants, [reportType, WIDGET_FILTER_PREVIEW_COUNT], undefined)) {
    const filterCount = get(widgetConstants, [reportType, WIDGET_FILTER_PREVIEW_COUNT], undefined);
    count = filterCount(filters);
  } else {
    const partialFilters = Object.keys(filters).filter((key: string) => ["exclude", "partial_match"].includes(key));

    partialFilters.forEach((key: string) => {
      const value = filters[key];
      if (key === "exclude") {
        count = count + (excludeCaseHelper(value) || []).length;
      } else if (key === "partial_match") {
        count = count + (partialCaseHelper(value) || []).length;
      }
    });
    const normalFilters = Object.keys(filters).filter(
      (key: string) =>
        ![
          ...customFieldKeys,
          ...partialFilters,
          ...graphFilterKeys,
          "custom_fields",
          "stacked_metrics",
          "hideScore",
          "workitem_attributes"
        ].includes(key)
    );

    if (filters.hasOwnProperty("workitem_attributes") && Object.keys(filters?.workitem_attributes).length > 0) {
      if (filters?.workitem_attributes.hasOwnProperty("teams") && filters?.workitem_attributes?.teams.length > 0) {
        count = count + 1;
      }
      if (
        filters?.workitem_attributes.hasOwnProperty("code_area") &&
        filters?.workitem_attributes?.code_area.length > 0
      ) {
        count = count + 1;
      }
    }

    count = count + Object.keys(normalFilters).length;
  }
  return count;
};

export const getAcrossOptions = (filters: any, reportType: any): { label: string; value: any }[] => {
  const application = get(widgetConstants, [reportType, "application"], undefined);
  let data: any;

  if (
    (["jenkinsgithub", "pagerduty", "praetorian", "nccgroup", "snyk", "azure_devops"].includes(application) &&
      !scmCicdReportTypes.includes(reportType as any)) ||
    [
      "github_prs_report_trends",
      "github_issues_report_trends",
      "sonarqube_metrics_report",
      "github_issues_first_response_report_trends",
      "github_prs_response_time_single_stat",
      "github_prs_single_stat",
      "github_issues_count_single_stat",
      "sonarqube_metrics_trend_report",
      "github_issues_first_response_count_single_stat",
      "cicd_jobs_count_report",
      "cicd_scm_jobs_duration_report",
      "cicd_pipeline_jobs_duration_report",
      "testrails_tests_report"
    ].includes(reportType) ||
    scmMergeTrendTypes.includes(reportType)
  ) {
    const acrossLabelMapping = get(widgetConstants, [reportType, "acrossFilterLabelMapping"], {});
    data = get(widgetConstants, [reportType, "across"], []).map((item: any) => {
      return {
        label: get(acrossLabelMapping, item, toTitleCase(item.replace(/_/g, " "))),
        value: item
      };
    });
  } else {
    const supportedFilters = get(widgetConstants, [reportType, "supported_filters"], undefined) || [];
    let supportedFilterData: any[] = [];
    if (isArray(supportedFilters)) {
      supportedFilterData = supportedFilters.reduce((acc: any, next: any) => {
        return [...acc, ...(next["values"] || [])];
      }, []);
    } else {
      if ("values" in supportedFilters && supportedFilters?.values) {
        supportedFilterData = supportedFilters.values;
      }
    }

    data = supportedFilterData.map((item: any) => ({
      label: toTitleCase(item.replace(/_/g, " ")),
      value: item
    }));

    if (application === IntegrationTypes.JIRA) {
      if (reportType === "resolution_time_report") {
        data.push(
          { label: "Issue Created By Date", value: "issue_created" },
          { label: "Issue Updated By Date", value: "issue_updated" },
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
        { label: "Project", value: "project" }
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
  }

  const appendAcrossOptions = get(widgetConstants, [reportType, "appendAcrossOptions"], undefined);
  let acrossOptions = [
    jiraBAReportTypes.JIRA_PROGRESS_REPORT,
    jiraBAReportTypes.JIRA_BURNDOWN_REPORT,
    azureBAReportTypes.AZURE_ISSUES_PROGRESS_REPORT
  ].includes(reportType as any)
    ? []
    : data;
  if (
    application === IntegrationTypes.GITHUB &&
    acrossOptions &&
    reportType === "github_commits_report" &&
    !scmMergeTrendTypes.includes(reportType)
  ) {
    if (!acrossOptions.includes("technology")) {
      acrossOptions.push("technology");
    }
  }

  if (application === IntegrationTypes.JIRA) {
    acrossOptions = acrossOptions.map((option: any) => {
      const mapping = get(widgetConstants, [reportType, "filterOptionMap"], undefined);
      return { ...option, label: get(mapping, [option.value], option.label) };
    });
  }

  if (
    application === IntegrationTypes.JIRA &&
    reportType === "tickets_report" &&
    acrossOptions &&
    !acrossOptions.find((f: any) => f.value === "epic")
  ) {
    // add epics
    acrossOptions && acrossOptions.push({ label: "EPIC", value: "epic" });
  } else if (
    application === IntegrationTypes.JIRA &&
    reportType !== "tickets_report" &&
    acrossOptions &&
    acrossOptions.find((f: any) => f.value === "epics")
  ) {
    // remove epics
    const index = acrossOptions.findIndex((f: any) => f.value === "epic");
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
    acrossOptions = acrossOptions.filter((option: any) => {
      if (option.value === "model") return false;
      return !(filters?.stacks && filters.stacks.includes(option.value));
    });
  }

  if (isArray(appendAcrossOptions) && appendAcrossOptions.length) {
    acrossOptions = acrossOptions.filter(
      (option: any) => !appendAcrossOptions.find(item => item.value === option.value)
    );
    acrossOptions = [...acrossOptions, ...appendAcrossOptions].map(item => ({
      ...item,
      label: (item.label || "").toUpperCase()
    }));
  }
  if (
    [
      "sprint_metrics_trend",
      "sprint_metrics_percentage_trend",
      "azure_sprint_metrics_trend",
      "azure_sprint_metrics_percentage_trend"
    ].includes(reportType)
  ) {
    acrossOptions = sprintReportXAxisOptions;
  }

  if (reportType === LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_TYPE_REPORT) {
    acrossOptions = leadTimeReportXAxisOptions;
  }

  if (["scm_rework_report"].includes(reportType as any)) {
    const aggFilters = [
      { label: "Commit Week", value: "trend_week" },
      { label: "Commit Month", value: "trend_month" }
    ];
    acrossOptions = [...acrossOptions, ...aggFilters];
  }
  // @ts-ignore
  return uniqBy(acrossOptions, "value");
};

export const getWidgetFilters = (filters: any, metaData: any, reportType: string, customFieldsList: any[]) => {
  const filterKeys = Object.keys(filters || {});
  if (filterKeys.length) {
    let final_filters: any[] = [];
    filterKeys.forEach((filter_key: string) => {
      switch (filter_key) {
        case "across":
          if (get(widgetConstants, [reportType, "xaxis"], undefined) && filters[filter_key]) {
            const acrossValue = filters[filter_key];
            const customField: any = customFieldsList.find((item: any) => item.key === acrossValue);
            if (customField) {
              final_filters.push({
                label: "X-Axis",
                value: customField.name ?? acrossValue
              });
            } else {
              const value = getAcrossValue(filters, reportType);
              const option = getAcrossOptions(filters, reportType).find((option: any) => option.value === value);
              final_filters.push({
                label: "X-Axis",
                value: get(option, ["label"], value)
              });
            }
          }
          break;
        case "across_limit":
          final_filters.push({
            label: "Max X-Axis Entries",
            value: filters[filter_key]
          });
          break;
        case "calculation":
          if (!!filters[filter_key] && reportType === LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT) {
            const option = leadTimeMetricOptions.find((option: any) => option.value === filters[filter_key]);
            final_filters.push({
              label: "Metrics",
              value: get(option, ["label"], filters[filter_key])
            });
          }
          break;
        case "interval":
          if (
            ![
              "resolution_time_report",
              "scm_issues_time_resolution_report",
              "github_prs_response_time_report",
              "github_coding_days_report",
              "scm_rework_report",
              AppName.ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
              "effort_investment_trend_report",
              "effort_investment_single_stat",
              AppName.DEV_PRODUCTIVITY_REPORTS.PRODUCTIVITY_SCORE_BY_ORG_UNIT,
              AppName.DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_SCORE_REPORT
            ].includes(reportType)
          ) {
            final_filters.push({
              label: filter_key,
              value: [filters[filter_key]]
            });
          }
          if (["effort_investment_trend_report", "effort_investment_single_stat"].includes(reportType || "")) {
            const option = effortInvestmentTrendReportSampleInterval.find(
              (item: any) => item.value === filters[filter_key]
            );
            if (option) {
              final_filters.push({
                label: "Sample Interval",
                value: option.label
              });
            }
          }
          if (AppName.DEV_PROD_TABLE_REPORTS.includes((reportType as any) || "")) {
            final_filters.push({
              label: filter_key,
              value: [toTitleCase(filters[filter_key])]
            });
          }
          break;
        case "metric":
        case "metrics":
          let value: any = undefined;
          let options: any[] = [];
          switch (reportType) {
            case "resolution_time_report":
            case AppName.ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT:
              options = jiraResolutionTimeMetricsOptions;
              break;
            case "scm_issues_time_resolution_report":
              options = scmResolutionTimeMetricsOptions;
              break;
            case "tickets_report":
            case "azure_tickets_report":
              options = ticketReportMetricOptions;
              break;
            case "bullseye_branch_coverage_report":
            case "bullseye_branch_coverage_trend_report":
            case "bullseye_decision_coverage_report":
            case "bullseye_decision_coverage_trend_report":
            case "bullseye_code_coverage_report":
            case "bullseye_code_coverage_trend_report":
            case "bullseye_function_coverage_trend_report":
            case "bullseye_function_coverage_report":
              const optionsKey = getOptionKey(reportType);
              options = buildMetricOptions(optionsKey as any);
              break;
            case "sonarqube_code_complexity_report":
            case "sonarqube_code_complexity_trend_report":
              options = sonarQubeMetricsOptions;
              break;
            case "sprint_metrics_percentage_trend":
            case "sprint_metrics_trend":
            case "sprint_commit_to_done_stat":
            case "sprint_creep_done_stat":
            case "sprint_creep_stat":
            case "sprint_impact_estimated_ticket_report":
            case "azure_sprint_metrics_percentage_trend":
            case "azure_sprint_metrics_trend":
            case "azure_sprint_commit_to_done_stat":
            case "azure_sprint_creep_done_stat":
            case "azure_sprint_creep_stat":
            case "azure_sprint_impact_estimated_ticket_report":
              options = (jiraSprintMetricOptions as any)?.[reportType.replace("azure_", "")] || [];
              break;
            case "sprint_metrics_single_stat":
            case "azure_sprint_metrics_single_stat":
              options = statSprintMetricsOptions;
              break;
            case "jira_time_across_stages":
            case "scm_issues_time_across_stages_report":
            case AppName.ISSUE_MANAGEMENT_REPORTS.TIME_ACROSS_STAGES:
              options = jiraTimeAcrossStagesMetricOptions;
              break;
            case AppName.JENKINS_REPORTS.SCM_CODING_DAYS_REPORT:
              options = scmCodingMetricsOptions;
              break;
          }

          if (isArray(filters[filter_key])) {
            value = filters[filter_key].map((key: string) => {
              const option = options.find((option: any) => option?.value === key);
              return option ? option.label : capitalize(removeSnakeCase(key));
            });
          } else {
            const option = options.find((option: any) => option?.value === filters[filter_key]);
            value = option ? option.label : capitalize(removeSnakeCase(filters[filter_key]));
          }
          if ((isArray(value) && value.length) || (typeof value === "string" && !["metrics", "metric"].includes(value)))
            final_filters.push({
              label: "Metrics",
              value
            });
          break;
        case "stacks":
          if (get(widgetConstants, [reportType, "stack_filters"], undefined)) {
            const stacks = filters[filter_key];
            if (isArray(stacks)) {
              const updatedStacks = stacks.map((stackItem: string) => {
                const customField: any = customFieldsList.find((item: any) => item.key === stackItem);
                if (customField) {
                  return customField.name;
                } else {
                  return getStackOptions(stackItem, reportType);
                }
              });
              if (updatedStacks.length) {
                final_filters.push({
                  label: "Stacks",
                  value: updatedStacks
                });
              }
            }
          }
          break;
        case "filter_across_values":
          final_filters.push({
            label: "X-Axis Labels",
            value: filters[filter_key] ? "True" : "False"
          });
          break;
        case TICKET_CATEGORIZATION_UNIT_FILTER_KEY:
          const applications = get(widgetConstants, [reportType, "application"], "jira");
          const filterOptions =
            applications === "azure_devops"
              ? azureEffortInvestmentUnitFilterOptions
              : ticketCategorizationUnitFilterOptions;
          const option = filterOptions.find((option: any) => option.value === filters[filter_key]);
          final_filters.push({
            label: "Effort Unit",
            value: get(option, ["label"], filters[filter_key])
          });
          break;
        case ACTIVE_WORK_UNIT_FILTER_KEY:
          const application = getWidgetConstant(reportType, "application");
          const activeWorkOptions =
            application === IntegrationTypes.AZURE ? azureActiveWorkUnitOptions : jiraActiveWorkUnitOptions;
          const activeWorkOption = activeWorkOptions.find((option: any) => option.value === filters[filter_key]);
          final_filters.push({
            label: "Active Work Unit",
            value: get(activeWorkOption, ["label"], filters[filter_key])
          });
          break;
        case "idle":
          final_filters.push({
            label: "Idle Length",
            value: filters[filter_key]
          });
          break;
        case "poor_description":
          final_filters.push({
            label: "Poor Description Length",
            value: filters[filter_key]
          });
          break;
        case "last_sprint":
          final_filters.push({
            label: "Last Sprint",
            value: filters[filter_key]
          });
          break;
      }
    });
    const getMetadataFilters = get(widgetConstants, [reportType, METADATA_FILTERS_PREVIEW], undefined);
    if (getMetadataFilters) {
      final_filters = [...final_filters, ...getMetadataFilters(metaData)];
    }
    return final_filters;
  } else {
    return [];
  }
};

export const getLtGtFieldValue = (value: any) => {
  if (value?.["$lt"] || value?.["$gt"]) {
    return `${value?.["$gt"]} - ${value?.["$lt"]}`;
  } else if (value?.from_state || value?.to_state) {
    return `${value?.from_state} - ${value?.to_state}`;
  }
  return `${value?.[0]} - ${value?.[1]}`;
};

export const getStackOptions = (keyName: any, reportType: any) => {
  if (["cicd_jobs_count_report", "testrails_tests_report"].includes(reportType)) {
    const stackLabelMapping = get(widgetConstants, [reportType, "stackFilterLabelMapping"], {});
    return stackLabelMapping[keyName] || keyName;
  }
  return keyName;
};
