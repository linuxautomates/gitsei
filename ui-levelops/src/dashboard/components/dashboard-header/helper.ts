import { getDiffInRange } from "utils/timeUtils";
import { toTitleCase } from "utils/stringUtils";
import { optionType } from "dashboard/dashboard-types/common-types";
import {
  AZURE_CUSTOM_FIELD_PREFIX,
  CONTAINS,
  CUSTOM_FIELD_PREFIX,
  STARTS_WITH,
  valuesToFilters
} from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import { capitalize, get, upperCase } from "lodash";
import {
  complexFilterKeys,
  jenkinsConfigTimePeriodOptions,
  uiFiltersMapping,
  valuesKeysForCount
} from "../dashboard-application-filters/AddFiltersComponent/filterConstants";
import { FILTER_NAME_MAPPING, scmCicdFilterOptionsMapping } from "../../constants/filter-name.mapping";
import { allDataSortingOptions } from "dashboard/constants/WidgetDataSortingFilter.constant";
import { ThemeType } from "antd/lib/icon";
import { updateTimeFiltersValue } from "shared-resources/containers/widget-api-wrapper/helper";
import { jiraEffortInvestmentTrendReportTimeRangeOptions } from "dashboard/graph-filters/components/Constants";
import {
  BA_COMPLETED_WORK_STATUS_BE_KEY,
  BA_EFFORT_ATTRIBUTION_BE_KEY,
  BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY,
  BA_IN_PROGRESS_STATUS_BE_KEY,
  BA_TIME_RANGE_FILTER_KEY,
  effortAttributionOptions
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { HeaderPivot } from "configurations/configuration-types/OUTypes";
import {
  doraReports,
  DORA_REPORTS,
  JIRA_SPRINT_REPORTS,
  LEAD_TIME_MTTR_REPORTS
} from "dashboard/constants/applications/names";
import { DORA_REPORT_TO_KEY_MAPPING } from "dashboard/graph-filters/components/helper";
import { getCalculationFieldLabel } from "dashboard/graph-filters/components/GenericFilterComponents/DoraGenericFilter/constants";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { HARNESS_CICD_FILTER_LABEL_MAPPING } from "configurations/pages/Organization/Filters/harnessng/harnessng-job-filter.config";
import { WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY } from "dashboard/reports/jira/constant";
import LocalStoreService from "services/localStoreService";
import {
  DashboardTimeRangeOptions,
  PreCalcDashboardTimeRangeOptions
} from "../dashboard-view-page-secondary-header/constants";
import { TENANTS_USING_PRE_CALC } from "./constants";
import { isDashboardTimerangeEnabled } from "helper/dashboard.helper";
import { DashboardSettingsModalTitleType } from "../dashboard-settings-modal/helper";

export enum DashboardActionMenuType {
  ADD_WIDGET = "add_widget",
  MODIFY_LAYOUT = "change_layout",
  MANAGE_SETTINGS = "dashboard_settings",
  EXPORT = "export",
  ADD_NOTES = "add_notes",
  CONFIGURE_COLOR_SCHEME = "configure_color_scheme"
}

const workflowProfileReportTypeMap = {
  change_failure_rate: "change_failure_rate",
  dora_lead_time_for_change: "lead_time_for_changes",
  dora_mean_time_to_restore: "mean_time_to_restore",
  deployment_frequency_report: "deployment_frequency",
  leadTime_changes: "lead_time_for_changes",
  meanTime_restore: "mean_time_to_restore"
};

export const getDashboardActionMenuLabel = (key: DashboardActionMenuType) => {
  const splitKeys = key.split("_");
  return `${capitalize(splitKeys[0])} ${capitalize(splitKeys[1])}`;
};

export enum DashboardActionButtonLabelType {
  EXPORT = "Export",
  GENERATING_REPORT = "Generating report...",
  LOADING_DASHBOARDS = "Loading insights...",
  GLOBAL_FILTERS = "Filters",
  EDIT = "Edit",
  ADD = "Add",
  EMPTY = "",
  ADD_NOTES = "Add Notes"
}

export enum DashboardTagType {
  DEFAULT = "Default"
}

export const dashboardConfigureActionButtonData = [
  {
    theme: "outlined" as ThemeType,
    icon_type: "setting",
    button_className: "more-button",
    button_label: DashboardActionButtonLabelType.EMPTY,
    menuData: [
      {
        key: "CONTENT",
        value: "CONTENT",
        icon_type: "",
        menu_class: "menu-divider"
      },
      {
        key: DashboardActionMenuType.ADD_WIDGET,
        value: getDashboardActionMenuLabel(DashboardActionMenuType.ADD_WIDGET),
        icon_type: "bar-chart"
      },
      {
        key: DashboardActionMenuType.ADD_NOTES,
        value: getDashboardActionMenuLabel(DashboardActionMenuType.ADD_NOTES),
        icon_type: "edit"
      },
      {
        key: DashboardActionMenuType.MODIFY_LAYOUT,
        value: getDashboardActionMenuLabel(DashboardActionMenuType.MODIFY_LAYOUT),
        icon_type: "layout"
      },
      {
        key: DashboardActionMenuType.CONFIGURE_COLOR_SCHEME,
        value: "Configure Color Scheme",
        icon_type: "bg-colors"
      },
      {
        key: "ACTIONS",
        value: "ACTIONS",
        icon_type: "",
        menu_class: "menu-divider"
      },
      {
        key: DashboardActionMenuType.MANAGE_SETTINGS,
        value: DashboardSettingsModalTitleType.DASHBOARD_SETTINGS,
        icon_type: "setting"
      },
      {
        key: DashboardActionMenuType.EXPORT,
        value: getDashboardActionMenuLabel(DashboardActionMenuType.EXPORT),
        icon_type: "export"
      }
    ]
  }
];

export const hasValue = (object: any) => {
  if ((Array.isArray(object) && object.length) || Object.keys(object || {}).length || typeof object === "number") {
    return true;
  }
  return false;
};

const getLabelKey = (filterKey: any, reportType: any) => {
  let labelKey = filterKey;
  const valueKeyObject = get(widgetConstants, [reportType as any, "valuesToFilters"], {});
  const exist = Object.values(valueKeyObject).includes(filterKey);
  if (exist) {
    labelKey = Object.keys(valueKeyObject).find((item: any) => valueKeyObject[item] === filterKey) || filterKey;
  }
  return labelKey;
};

export const partialCaseHelper = (partailObject: any, reportType?: any) => {
  const final_filters: { key: string; label: string; value: any; exclude?: boolean; partial?: string }[] = [];
  Object.keys(partailObject || {})
    .filter(key => !key.includes("customfield_") && !key.includes(AZURE_CUSTOM_FIELD_PREFIX))
    .forEach((partial_filter_label: string) => {
      const labelKey = getLabelKey(partial_filter_label, reportType) || partial_filter_label;
      if (
        hasValue(partailObject[partial_filter_label][STARTS_WITH]) ||
        hasValue(partailObject[partial_filter_label][CONTAINS])
      ) {
        if (labelKey !== "workitem_sprint_full_names") {
          final_filters.push({
            key: partial_filter_label,
            label: labelKey,
            value: [partailObject[partial_filter_label][STARTS_WITH] || partailObject[partial_filter_label][CONTAINS]],
            partial: partailObject[partial_filter_label][STARTS_WITH] ? "Start With" : "Contain"
          });
        } else if (labelKey === "workitem_sprint_full_names") {
          final_filters.push({
            key: partial_filter_label,
            label: "azure_iteration",
            value: [partailObject[partial_filter_label][STARTS_WITH] || partailObject[partial_filter_label][CONTAINS]],
            partial: partailObject[partial_filter_label][STARTS_WITH] ? "Start With" : "Contain"
          });
        }
      }
    });
  return final_filters;
};

export const excludeCaseHelper = (filterObject: any, reportType?: any) => {
  const final_filters: { key: string; label: string; value: any; exclude?: boolean; partial?: string }[] = [];
  Object.keys(filterObject || {})
    .filter(key => key !== "custom_fields" && key !== "workitem_custom_fields")
    .forEach((exclude_filter_label: string) => {
      const labelKey = getLabelKey(exclude_filter_label, reportType) || exclude_filter_label;
      if (hasValue(filterObject[exclude_filter_label]) && exclude_filter_label !== "azure_iteration") {
        if (exclude_filter_label === "partial_match") {
          let partialMatchData = partialCaseHelper(filterObject[exclude_filter_label], reportType);
          if (partialMatchData) {
            final_filters.push({
              key: partialMatchData[0]?.key,
              label: partialMatchData[0]?.label,
              value: partialMatchData[0]?.value,
              partial: partialMatchData[0]?.partial,
              exclude: true
            });
          }
        } else {
          final_filters.push({
            key: exclude_filter_label,
            label: labelKey === "workitem_sprint_full_names" ? "azure_iteration" : labelKey,
            value: filterObject[exclude_filter_label],
            exclude: true
          });
        }
      } else if (hasValue(filterObject[exclude_filter_label]) && exclude_filter_label === "azure_iteration") {
        final_filters.push({
          key: exclude_filter_label,
          label: labelKey,
          value: filterObject[exclude_filter_label].map((item: any) =>
            typeof item === "string" ? item : `${item.parent}\\${item.child}`
          ),
          exclude: true
        });
      }
    });
  return final_filters;
};

export const timeFilterCaseHelper = (key: string, label: any, timeFilterObject: any) => {
  const final_filters: { key: string; label: string; value: any; exclude?: boolean; partial?: string }[] = [];
  if (hasValue(timeFilterObject)) {
    final_filters.push({
      key: key,
      label: label,
      value: [
        timeFilterObject?.["$gt"] || timeFilterObject?.["$gte"] || timeFilterObject?.["min"] || "NA",
        timeFilterObject?.["$lt"] || timeFilterObject?.["$lte"] || timeFilterObject?.["max"] || "NA"
      ]
    });
  }
  return final_filters;
};
export const getJiraOrFiltersHelper = (
  filters: any,
  uri: string,
  reportType?: string,
  metadata?: any,
  dashboardMetaData?: any,
  workspaceProfile?: any,
  uniqApplicationName?: any
) => {
  let newFilters = { ...filters };
  const uiDetails = uiFiltersMapping[uri] || [];
  if (metadata && dashboardMetaData) {
    if (isDashboardTimerangeEnabled(dashboardMetaData)) {
      newFilters = { ...updateTimeFiltersValue(dashboardMetaData, metadata, newFilters) };
    }
  }
  const jiraOrFiltersValuesKeys = Object.keys(newFilters || {}) || [];
  let final_filters: {
    key: string;
    label: string;
    value: any;
    exclude?: boolean;
    partial?: string;
    missing_field?: boolean;
  }[] = [];
  if (jiraOrFiltersValuesKeys.length) {
    jiraOrFiltersValuesKeys.forEach((filter_key: string) => {
      const labelKey = getLabelKey(filter_key, reportType);

      switch (filter_key) {
        case "exclude":
          const excludefilters = excludeCaseHelper(newFilters[filter_key], reportType);
          final_filters = [...final_filters, ...excludefilters];
          break;

        case "partial_match":
          const partialFilters = partialCaseHelper(newFilters[filter_key], reportType);
          final_filters = [...final_filters, ...partialFilters];
          break;
        case "ideal_range":
        case "parent_story_points":
        case "story_points":
        case "workitem_story_points":
        case "workitem_parent_story_points":
        case "issue_updated_at":
        case "issue_created_at":
        case "issue_resolved_at":
        case "zendesk_created_at":
        case "salesforce_created_at":
        case "salesforce_updated_at":
        case "disclosure_range":
        case "publication_range":
        case "score_range":
        case "ingested_at":
        case "created_at":
        case "committed_at":
        case "end_time":
        case "start_time":
        case "pr_created_at":
        case "pr_closed_at":
        case "jira_pr_closed_at":
        case "pr_updated_at":
        case "jira_issue_resolved_at":
        case "jira_issue_created_at":
        case "jira_issue_updated_at":
        case "pr_merged_at":
        case "cicd_job_run_end_time":
        case "completed_at":
        case "snapshot_range":
        case "issue_due_at":
        case "workitem_created_at":
        case "workitem_updated_at":
        case "workitem_resolved_at":
        case "cov_defect_last_detected_at":
        case "cov_defect_first_detected_at":
        case "updated_at":
        case "jenkins_end_time":
        case "issue_closed_at":
        case BA_TIME_RANGE_FILTER_KEY:
        case "time_range":
        case "started_at":
        case "planned_ended_at":
        case "released_in":
          let label: string = labelKey;
          const keyLabelMapping = get(
            widgetConstants,
            // @ts-ignore
            [reportType, WIDGET_FILTER_PREVIEW_KEY_LABEL_MAPPING_KEY],
            undefined
          );
          if (keyLabelMapping && keyLabelMapping?.[labelKey]) {
            label = keyLabelMapping[labelKey];
          } else if (reportType && get(widgetConstants, [reportType, "application"], undefined) === "zendesk") {
            label = "zendesk_ticket_created_in";
          } else if (filter_key === BA_TIME_RANGE_FILTER_KEY) {
            label = "effort_investment_time_range";
          } else if (filter_key === "score_range") {
            label = "priority_score";
          } else if (filter_key === "disclosure_range") {
            label = "disclosure_time";
          } else if (filter_key === "publication_range") {
            label = "publication_time";
          } else if (filter_key === "issue_due_at") {
            label = "Jira Due Date";
          } else if (["cov_defect_first_detected_at", "cov_defect_last_detected_at"].includes(filter_key)) {
            label = label;
          } else if (filter_key.endsWith("_at")) {
            label = filter_key.replace(/_at$/, "_in");
          } else if (["code_volume_vs_deployment_report"].includes(reportType as any) && filter_key === "end_time") {
            label = "Job End Date";
          } else if (
            [DORA_REPORTS.CHANGE_FAILURE_RATE, DORA_REPORTS.DEPLOYMENT_FREQUENCY_REPORT].includes(reportType as any)
          ) {
            label = getCalculationFieldLabel(workspaceProfile, DORA_REPORT_TO_KEY_MAPPING[reportType as string]);
          }

          let timeFilters = timeFilterCaseHelper(filter_key, label, newFilters[filter_key]);
          if (
            filter_key === BA_TIME_RANGE_FILTER_KEY &&
            ["effort_investment_trend_report", "effort_investment_single_stat"].includes(reportType || "")
          ) {
            const mFactor = getDiffInRange(newFilters[filter_key]);
            const option = jiraEffortInvestmentTrendReportTimeRangeOptions.find(
              (item: any) => item.mFactor === mFactor
            );
            if (option) {
              timeFilters = [{ key: filter_key, label, value: option.label }];
            }
          }
          final_filters = [...final_filters, ...timeFilters];
          break;

        case "custom_fields":
          break;

        case "sort":
          if (hasValue(newFilters[filter_key][0]["id"])) {
            final_filters.push({
              key: filter_key,
              label: labelKey,
              value: [newFilters[filter_key][0]["id"]]
            });
          }
          break;
        case "agg_type":
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: labelKey,
              value: [newFilters[filter_key]]
            });
          }
          break;
        case "parameters":
          newFilters[filter_key]
            .filter((item: any) => item.name !== "" || item.values.length !== 0)
            .forEach((item: any) => {
              if (hasValue(item.values)) {
                final_filters.push({
                  key: item.name,
                  label: item.name + "(Parameter)",
                  value: item.values
                });
              }
            });
          break;
        case "missing_fields":
          if (!["review_collaboration_report"].includes(reportType as string)) {
            const missing_field_filters = newFilters["missing_fields"];
            Object.keys(missing_field_filters).forEach(filter => {
              if (!filter.includes(CUSTOM_FIELD_PREFIX)) {
                if (hasValue(missing_field_filters[filter])) {
                  final_filters.push({
                    key: filter,
                    label: filter,
                    value: missing_field_filters[filter] ? "True" : "False",
                    missing_field: true
                  });
                }
              }
            });
          }
          if (hasValue(newFilters[filter_key])) {
            const pr_merged = get(newFilters, [filter_key, "pr_merged"], false);
            final_filters.push({
              key: filter_key,
              label: "PR Filter",
              value: pr_merged ? "PR CLOSED" : "PR MERGED"
            });
          }
          break;
        case "metadata":
          break;
        case "hygiene_types":
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: "HYGIENE",
              value: newFilters[filter_key].map((item: any) => upperCase(item))
            });
          }
          break;
        case "n_last_reports":
          final_filters.push({
            key: filter_key,
            label: "Last Reports",
            value: newFilters[filter_key]
          });
          break;
        case "time_period":
          const item = uiDetails.find((filter: any) => filter.key === filter_key);
          const value = newFilters[filter_key];
          const option = jenkinsConfigTimePeriodOptions.find((option: any) => option.value === value);
          final_filters.push({
            key: filter_key,
            label: get(item, ["label"], "Time Period"),
            value: get(option, ["label"], value)
          });
          break;
        case "job_normalized_full_names":
          if (hasValue(newFilters[filter_key])) {
            let lableKeyName = "JENKINS JOB PATH";
            if (["jenkins_jobs_filter_values", "jenkins_pipelines_jobs_filter_values"].includes(uri)) {
              lableKeyName = "QUALIFIED NAME";
            }
            final_filters.push({
              key: filter_key,
              label: lableKeyName,
              value: newFilters[filter_key]
            });
          }
          break;
        case "ticket_categorization_scheme":
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: "Effort Investment Profile",
              value: newFilters[filter_key]
            });
          }
          break;
        case "velocity_config_id":
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: "Workflow Configuration Profile",
              value: newFilters[filter_key]
            });
          }
          break;
        case "sort_xaxis":
          if (hasValue(newFilters[filter_key])) {
            const sortLabel = allDataSortingOptions().find((item: optionType) => item.value === newFilters[filter_key]);
            final_filters.push({
              key: filter_key,
              label: "Sort X-Axis",
              value: sortLabel?.label ?? newFilters[filter_key]
            });
          }
          break;
        case "azure_iteration":
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: "Azure Iteration",
              value: newFilters[filter_key].map((item: any) =>
                typeof item === "string" ? item : `${item.parent}\\${item.child}`
              )
            });
          }
          break;
        case "workitem_attributes":
          if (hasValue(newFilters[filter_key])) {
            let moreFilters: any = [];
            if (newFilters[filter_key].hasOwnProperty("code_area") && newFilters[filter_key]["code_area"].length) {
              moreFilters = [
                ...moreFilters,
                {
                  key: filter_key,
                  label: "Azure Areas",
                  value: newFilters[filter_key]["code_area"].map((item: any) => `${item.child ?? item}`)
                }
              ];
            }
            if (newFilters[filter_key].hasOwnProperty("teams") && newFilters[filter_key]["teams"].length) {
              moreFilters = [
                ...moreFilters,
                {
                  key: filter_key,
                  label: "Azure Teams",
                  value: newFilters[filter_key]["teams"]
                }
              ];
            }
            final_filters.push(...moreFilters);
          }
          break;
        case "build_job":
          if (["code_volume_vs_deployment_report"].includes(reportType as any) && hasValue(newFilters[filter_key])) {
            const childFiltersKeys = Object.keys(newFilters[filter_key] || {});
            childFiltersKeys.forEach((key: any) => {
              if (hasValue(newFilters[filter_key][key])) {
                final_filters.push({
                  key: `build_${key}`,
                  label: `build_${key}`,
                  value: newFilters[filter_key][key]
                });
              }
            });
          }
          break;
        case "deploy_job":
          if (["code_volume_vs_deployment_report"].includes(reportType as any) && hasValue(newFilters[filter_key])) {
            const childFiltersKeys = Object.keys(newFilters[filter_key] || {});
            childFiltersKeys.forEach((key: any) => {
              if (hasValue(newFilters[filter_key][key])) {
                final_filters.push({
                  key: `deploy_${key}`,
                  label: `deploy_${key}`,
                  value: newFilters[filter_key][key]
                });
              }
            });
          }
          break;
        case "code_area":
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: "Azure Areas",
              value: newFilters[filter_key].map((item: any) => item?.key)
            });
          }
          break;
        case "teams":
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: "Azure Teams",
              value: newFilters[filter_key].map((item: any) => item?.key)
            });
          }
          break;
        case "workitem_sprint_full_names":
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: "Azure Iteration",
              value: newFilters[filter_key].map((item: any) => item?.key ?? item)
            });
          }
          break;
        case "visualization":
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: "Visualization",
              value: toTitleCase(newFilters[filter_key])
            });
          }
          break;
        case BA_EFFORT_ATTRIBUTION_BE_KEY:
          if (hasValue(newFilters[filter_key])) {
            const value = newFilters[filter_key];
            const baEffortAttributionValue = effortAttributionOptions.find(item => item.value === value);

            final_filters.push({
              key: filter_key,
              label: "Effort attribution",
              value: baEffortAttributionValue?.label
            });
          }
          break;
        case BA_IN_PROGRESS_STATUS_BE_KEY:
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: "Statuses of in progress issues",
              value: newFilters[filter_key]
            });
          }
          break;
        case BA_COMPLETED_WORK_STATUS_BE_KEY:
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: "Statuses of completed issues",
              value: newFilters[filter_key]
            });
          }
          break;
        case BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY:
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: "Filter assignees by status",
              value: newFilters[filter_key]
            });
          }
          break;
        case "names":
          if (hasValue(newFilters[filter_key])) {
            final_filters.push({
              key: filter_key,
              label: "Name",
              value: newFilters[filter_key]
            });
          }
          break;
        case "cicd_user_ids":
          final_filters.push({
            key: filter_key,
            label: "Triggered By",
            value: newFilters[filter_key]
          });
          break;
        case "statuses":
          if (["testrails_tests_report", "testrails_tests_trend_report"].includes(reportType as any)) {
            final_filters.push({
              key: filter_key,
              label: "Current status",
              value: newFilters[filter_key]
            });
          } else {
            final_filters.push({
              key: filter_key,
              label: filter_key,
              value: newFilters[filter_key]
            });
          }
          break;
        default:
          if (hasValue(newFilters[filter_key])) {
            const filterKey =
              Object.keys(valuesToFilters).find(
                (key: any) => valuesToFilters[key as keyof typeof valuesToFilters] === filter_key
              ) || "";
            let filterNameMapping = labelKey;
            if (filterKey && reportType) {
              filterNameMapping = get(widgetConstants, [reportType, FILTER_NAME_MAPPING, filterKey], filter_key);
            }
            final_filters.push({
              key: filter_key,
              label: filterNameMapping,
              value: newFilters[filter_key]
            });
          }
          break;
      }
    });
    return final_filters;
  } else {
    return [];
  }
};

export const getGlobalFiltersHelper = (filters: any, reportType?: string) => {
  const globalFiltersValuesKeys = Object.keys(filters || {}) || [];
  let final_filters: { label: string; value: any; exclude?: boolean; partial?: string }[] = [];
  if (globalFiltersValuesKeys.length) {
    globalFiltersValuesKeys
      .filter((item: any) => {
        return ![
          "github_prs_filter_values",
          "github_commits_filter_values",
          "scm_issues_filter_values",
          "microsoft_issues_filter_values",
          "metadata"
        ].includes(item);
      })
      .forEach((filter_key: string) => {
        const allTypesFilters = getJiraOrFiltersHelper(filters[filter_key], filter_key, reportType);
        final_filters = [...final_filters, ...allTypesFilters];
      });
    return final_filters;
  } else {
    return [];
  }
};

export const filterCount = (filters: { [filter: string]: any }) => {
  // we are getting undefined from somewhere
  let count = Object.keys(filters).filter(
    key => !!key && ![...complexFilterKeys, ...valuesKeysForCount, "undefined"].includes(key)
  ).length;

  if (filters.custom_fields) {
    count = count + Object.keys(filters.custom_fields).length;
  }

  if (filters.exclude) {
    count = count + Object.keys(filters.exclude).filter(key => key !== "custom_fields").length;

    if (filters.exclude.custom_fields) {
      count = count + Object.keys(filters.exclude.custom_fields).length;
    }
  }

  if (filters.partial_match) {
    count = count + Object.keys(filters.partial_match).length;
  }

  if (filters.missing_fields) {
    count = count + Object.keys(filters.missing_fields).length;
  }

  return count;
};

export const jiraOrFiltersCustomFiltersHelper = (filters: any) => {
  const customFields = get(filters, ["custom_fields"], {});
  const excludeCustomFields = get(filters, ["exclude", "custom_fields"], {});
  const partialFilters = get(filters, ["partial_match"], {});
  const partialobjectKeys = Object.keys(partialFilters);
  let partialCustomFields: any = {};
  partialobjectKeys.forEach((partialKey: any) => {
    if (partialKey.includes("customfield_")) {
      partialCustomFields = { ...partialCustomFields, [partialKey]: partialFilters[partialKey] };
    }
  });
  return {
    normalcustomfields: { ...customFields },
    exclude: { ...excludeCustomFields },
    partial: { ...partialCustomFields }
  };
};

export const globalCustomFieldFiltersHelper = (globalFilters: any, widgetMetadata?: any, dashboardMetadata?: any) => {
  const globalFiltersKeys = Object.keys(globalFilters);
  let normalCustomFields: any = {};
  let excludeCustomFields: any = {};
  let partialCustomFields: any = {};
  let missingCustomField: any = {};
  globalFiltersKeys.forEach((keys: any) => {
    const missingFieldPresent = globalFilters[keys]["missing_fields"];
    if (missingFieldPresent) {
      Object.keys(missingFieldPresent).forEach(field => {
        if (field.includes(CUSTOM_FIELD_PREFIX)) {
          missingCustomField = {
            ...missingCustomField,
            [field]: missingFieldPresent[field] ? "True" : "False"
          };
        }
      });
    }
  });
  globalFiltersKeys.forEach((keys: any) => {
    const customFieldKey = get(globalFilters, [keys, "jira_custom_fields"], undefined)
      ? "jira_custom_fields"
      : get(globalFilters, [keys, "workitem_custom_fields"], undefined)
      ? "workitem_custom_fields"
      : "custom_fields";
    const customPresent = globalFilters[keys][customFieldKey];
    if (customPresent) {
      normalCustomFields = { ...normalCustomFields, ...get(globalFilters, [keys, customFieldKey], {}) };
    }
  });

  globalFiltersKeys.forEach((keys: any) => {
    const customFieldKey = get(globalFilters, [keys, "exclude", "jira_custom_fields"], undefined)
      ? "jira_custom_fields"
      : get(globalFilters, [keys, "exclude", "workitem_custom_fields"], undefined)
      ? "workitem_custom_fields"
      : "custom_fields";

    const globalexcludeCustomFields = get(globalFilters, [keys, "exclude", customFieldKey], {});
    if (Object.keys(globalexcludeCustomFields).length) {
      excludeCustomFields = { ...excludeCustomFields, ...globalexcludeCustomFields };
    }
  });
  globalFiltersKeys.forEach((keys: any) => {
    const globalpartialobject = get(globalFilters, [keys, "partial_match"], {});
    const globalpartialobjectKeys = Object.keys(globalpartialobject);
    globalpartialobjectKeys.forEach((partialKey: any) => {
      if (partialKey.includes("customfield_") || partialKey.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
        partialCustomFields = { ...partialCustomFields, [partialKey]: globalpartialobject[partialKey] };
      }
    });
  });
  if (widgetMetadata && dashboardMetadata) {
    normalCustomFields = {
      ...normalCustomFields,
      ...updateTimeFiltersValue(dashboardMetadata, widgetMetadata, { ...normalCustomFields })?.custom_fields
    };
  }
  return {
    normalcustomfields: { ...normalCustomFields },
    exclude: { ...excludeCustomFields },
    partial: { ...partialCustomFields },
    missing_fields: { ...missingCustomField }
  };
};

export const getReportFilterCount = (filters: any, filterConfig?: any[]) => {
  if (!filterConfig) {
    return 0;
  }
  const configKeys = filterConfig.map((config: any) => config.field);

  let count = Object.keys(filters)
    .filter(key => !complexFilterKeys.includes(key))
    .filter(key => configKeys.includes(key)).length;

  if (filters.custom_fields) {
    count = count + Object.keys(filters.custom_fields).filter(key => configKeys.includes(key)).length;
  }

  if (filters.workitem_custom_fields) {
    count = count + Object.keys(filters.workitem_custom_fields).length;
  }

  if (filters.exclude) {
    count =
      count +
      Object.keys(filters.exclude)
        .filter(key => key !== "custom_fields")
        .filter(key => configKeys.includes(key)).length;

    if (filters.exclude.custom_fields) {
      count = count + Object.keys(filters.exclude.custom_fields).filter(key => configKeys.includes(key)).length;
    }

    if (filters.exclude.workitem_custom_fields) {
      count = count + Object.keys(filters.exclude.workitem_custom_fields).length;
    }
  }

  if (filters.partial_match) {
    count = count + Object.keys(filters.partial_match).length;
  }

  if (filters.missing_fields) {
    count = count + Object.keys(filters.missing_fields).length;
  }

  if (filters.workitem_attributes) {
    count = count + Object.keys(filters.workitem_attributes).length;
  }
  if (filters.time_range) {
    count = count + 1;
  }
  return count;
};

export const lt_gt_format_fields = [
  "parent_story_points",
  "workitem_parent_story_points",
  "workitem_story_points",
  "story_points",
  "score_range",
  "state_transition",
  "complexity_score",
  "num_approvers",
  "num_reviewers",
  "ideal_range"
];

export const time_Range_Filters_fields = [
  "issue_updated_at",
  "issue_created_at",
  "issue_resolved_at",
  "jira_issue_created_at",
  "jira_issue_updated_at",
  "zendesk_created_at",
  "salesforce_created_at",
  "salesforce_updated_at",
  "disclosure_range",
  "publication_range",
  "ingested_at",
  "created_at",
  "committed_at",
  "end_time",
  "pr_created_at",
  "pr_closed_at",
  "jira_pr_closed_at",
  "pr_updated_at",
  "jira_issue_resolved_at",
  "pr_merged_at",
  "cicd_job_run_end_time",
  "completed_at",
  "snapshot_range",
  "workitem_resolved_at",
  "workitem_created_at",
  "workitem_updated_at",
  "issue_due_at",
  BA_TIME_RANGE_FILTER_KEY,
  "cov_defect_last_detected_at",
  "cov_defect_first_detected_at",
  "updated_at",
  "jenkins_end_time",
  "start_time",
  "issue_closed_at",
  "time_range",
  "started_at",
  "planned_ended_at",
  "released_in"
];
export const different_value_format_fields = [...lt_gt_format_fields, ...time_Range_Filters_fields, "parameters"];

export const orderedOUList = (root: HeaderPivot, arr: Array<HeaderPivot>, orderArray: Array<HeaderPivot>) => {
  let parents: any = root;
  if (orderArray.length !== arr.length) {
    arr.forEach((element: any) => {
      if (element.parent_ref_id == parents.id) {
        orderArray.push(element);
        orderedOUList(element, arr, orderArray);
      }
    });
  }
  return orderArray;
};

export const getDashboardTimeRangeIfPreCalc = () => {
  const ls = new LocalStoreService();
  if (TENANTS_USING_PRE_CALC.includes(ls.getUserCompany() || "")) {
    return PreCalcDashboardTimeRangeOptions;
  }
  return DashboardTimeRangeOptions;
};
