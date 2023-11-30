import {
  CUSTOM_FIELD_PREFIX,
  CUSTOM_FIELD_STACK_FLAG,
  valuesToFilters
} from "dashboard/constants/constants";
import widgetConstants from 'dashboard/constants/widgetConstants'
import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { cloneDeep, forEach, get, isArray, set, unset } from "lodash";
import moment from "moment";
import {
  getOUExclusionKeys,
  getSupportedApplications,
  removeFiltersWithEmptyValues,
  sanitizeCustomDateFilters,
  trimPartialStringFilters,
  updateTimeFiltersValue
} from "shared-resources/containers/widget-api-wrapper/helper";
import { baseColumnConfig } from "utils/base-table-config";
import { isSanitizedValue, removeEmptyKeys, sanitizeObject } from "utils/commonUtils";
import { DateFormats, DEFAULT_DATE_FORMAT, DateRange, getMomentFromInterval } from "../../../utils/dateUtils";
import {
  AZURE_SPRINT_REPORTS,
  JENKINS_AZURE_REPORTS,
  JIRA_SPRINT_REPORTS,
  JIRA_TICKETS_REPORT_NAME,
  leadTimeReports,
  SCM_FILES_REPORT,
  scmCicdReportTypes,
  ISSUE_MANAGEMENT_REPORTS,
  issueManagementReports,
  azureLeadTimeIssueReports,
  NO_LONGER_SUPPORTED_FILTER,
  JENKINS_REPORTS,
  azureIterationSupportableReports,
  JIRA_SPRINT_DISTRIBUTION_REPORTS,
  PAGERDUTY_REPORT,
  JIRA_MANAGEMENT_TICKET_REPORT,
  jiraAzureScmAllLeadTimeReportsApplication,
  INCLUDE_INTERVAL_IN_PAYLOAD,
  ALL_VELOCITY_PROFILE_REPORTS,
  LEAD_TIME_BY_STAGE_REPORTS,
  LEAD_TIME_REPORTS,
  DORA_REPORTS,
  TESTRAILS_REPORTS
} from "../../constants/applications/names";
import {
  allTimeFilterKeys
} from "../../graph-filters/components/helper";
import {
  AZURE_TIME_FILTERS_KEYS,
  GROUP_BY_TIME_FILTERS,
  IGNORE_X_AXIS_KEYS,
  PAGERDUTY_TIME_FILTER_KEYS,
  SCM_PRS_TIME_FILTERS_KEYS
} from "constants/filters";
import { PriorityOrderMapping } from "shared-resources/charts/jira-prioirty-chart/helper";
import {
  FILTER_KEY_MAPPING,
  INCLUDE_ACROSS_OU_EXCLUSIONS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY
} from "../../constants/filter-key.mapping";
import { ACTIVE_SPRINT_TYPE_FILTER_KEY } from "dashboard/components/dashboard-application-filters/AddFiltersComponent/filterConstants";
import { CodeVolumeVsDeployementIntervalMapping } from "constants/dashboard";
import { SCM_PRS_REPORTS } from "shared-resources/charts/helper";
import { sanitizeTimeFilters } from "utils/timeUtils";
import { RestTicketCategorizationCategory } from "classes/RestTicketCategorizationScheme";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { GITHUB_APPLICATIONS } from "utils/reportListUtils";
import { leadtimeOUDesignationQueryBuilder } from "dashboard/helpers/widget-build-query/leadtimePayloadQuery";
import { APPLICATIONS_SUPPORTING_OU_FILTERS } from "./helper-constants";
import { TIME_INTERVAL_TYPES, WEEK_DATE_FORMAT } from "constants/time.constants";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { transformAzureWidgetQueryForCustomFields } from "dashboard/helpers/helper";
import { ADO_APPLICATION } from "dashboard/reports/azure/constant";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";
import { IM_ADO } from "./drilldownColumnsHelper";
import { sanitizePartialStringFilters } from "utils/filtersUtils";
import { updateIssueCreatedAndUpdatedFilters } from "dashboard/graph-filters/components/updateFilter.helper";

export const integrationDeriveSupport = [
  "github_prs_report",
  "github_prs_single_stat",
  "github_prs_response_time_report",
  "github_prs_response_time_single_stat",
  "github_prs_report_trends",
  "github_prs_merge_single_stat",
  "github_prs_merge_trends",
  "github_prs_first_review_single_stat",
  "github_prs_first_review_to_merge_single_stat",
  "scm_pr_lead_time_by_stage_report",
  "scm_pr_lead_time_trend_report",
  "review_collaboration_report",
  DORA_REPORTS.DEPLOYMENT_FREQUENCY_REPORT,
  DORA_REPORTS.CHANGE_FAILURE_RATE,
  DORA_REPORTS.LEADTIME_CHANGES,
  LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT
];

const GITHUB_PRS_REPORTS = [
  "github_prs_report_trends",
  "github_prs_merge_trends",
  "github_prs_first_review_trends",
  "github_prs_first_review_to_merge_trends"
];

const PR_ACROSS_VALUES = ["pr_created", "pr_updated", "pr_merged", "pr_reviewed", "pr_closed"];

export const OR_QUERY_APPLICATIONS = [
  IntegrationTypes.GITHUBJIRA,
  IntegrationTypes.JIRA,
  IntegrationTypes.JIRAZENDESK,
  IntegrationTypes.JIRA_SALES_FORCE
];

export const LEAD_TIME_STAGE_FILTER_KEY = "histogram_stage_name";

export type DrillDownProps = {
  application: string;
  dashboardId: string;
  widgetId: string;
  x_axis: any;
  click_node?: string;
  widgetMetaData?: any;
  additionFilter?: any;
  drillDownTitle?: any;
  supportedCustomFields?: any[];
  scmGlobalSettings?: any;
  availableIntegrations?: any[];
  doraProfileIntegrationType?: string;
  stackFilters?: string[];
};

export const buildDrillDownFilters = (
  drillDownProps: DrillDownProps,
  widget: any,
  dashboardQuery: any,
  metaData: any,
  dashboardMetadata?: any,
  queryParamOU?: any,
  contextFilters?: any
) => {
  const drillDownTransformFunction = get(
    widgetConstants,
    [widget.type, "drilldown", "drilldownTransformFunction"],
    undefined
  );
  if (drillDownTransformFunction) {
    let { acrossValue, filters } = drillDownTransformFunction({
      drillDownProps,
      widget,
      dashboardQuery,
      metaData,
      dashboardMetadata,
      queryParamOU,
      contextFilters
    });
    const reportCustomFieldKey = get(widgetConstants, [widget?.type || "", "custom_fields"], undefined);
    const partialStringFilters = get(filters, ["filter", "partial_match"], undefined);

    if (partialStringFilters) {
      const sanitizedPartialFilters = sanitizePartialStringFilters(partialStringFilters);
      if (Object.keys(sanitizedPartialFilters).length > 0) {
        filters = {
          ...(filters || {}),
          filter: {
            ...(filters.filter || {}),
            partial_match: sanitizedPartialFilters
          }
        };
        filters = {
          ...(filters || {}),
          filter: { ...(trimPartialStringFilters(filters.filter) || {}) }
        };
      } else {
        unset(filters, ["filter", "partial_match"]);
      }
    }

    const noUpdate = get(filters, ["filter", "no_update_time_field"], undefined);

    if (!noUpdate) {
      filters = updateIssueCreatedAndUpdatedFilters(
        filters,
        metaData,
        widget.type,
        get(drillDownProps, ["jirazendesk", "type"], "")
      );
    }

    const noUpdateDashboardTime = get(filters, ["filter", "no_update_dashboard_time"], undefined);
    if (!noUpdateDashboardTime) {
      const ignoreKeys = [acrossValue].filter(_val => _val?.startsWith("customfield_"));
      filters = {
        ...(filters || {}),
        filter: {
          ...updateTimeFiltersValue(dashboardMetadata, metaData, { ...filters.filter }, ignoreKeys)
        }
      };
    }

    unset(filters, ["filter", "no_update_dashboard_time"]);
    unset(filters, ["filter", "no_update_time_field"]);

    if (SCM_PRS_REPORTS.includes(widget?.type) && SCM_PRS_TIME_FILTERS_KEYS.includes(acrossValue)) {
      const resolved_at = get(filters, ["filter", `${acrossValue}_at`], undefined);
      const x_axis = get(drillDownProps, ["x_axis"], "");
      let interval = get(filters, ["filter", "interval"], "day");
      const weekStartsOnMonday = getWidgetConstant(widget.type, "weekStartsOnMonday", false);
      if (interval === "week" && weekStartsOnMonday) {
        interval = "isoWeek";
      }
      let gt: any = getMomentFromInterval(x_axis, interval, DateRange.FROM);
      let lt: any = getMomentFromInterval(x_axis, interval, DateRange.TO);
      if (resolved_at) {
        const startDate = moment.unix(resolved_at?.$gt || resolved_at?.$gte);
        gt = gt.isAfter(startDate) ? gt : startDate;
        const endDate = moment.unix(resolved_at?.$lt);
        lt = lt.isAfter(endDate) ? endDate : lt;
      }
      const filterKey = `${acrossValue}_at`;
      let ltKey = `$lt`;
      let gtKey = `$gt`;
      filters = {
        ...filters,
        filter: {
          ...filters?.filter,
          [filterKey]: {
            [ltKey]: moment.isMoment(lt) ? lt.unix().toString() : moment(lt).unix().toString(),
            [gtKey]: moment.isMoment(gt) ? gt.unix().toString() : moment(gt).unix().toString()
          }
        }
      };
    }
    const x_axis = get(drillDownProps, ["x_axis"], "");
    if (
      [
        "resolution_time_report",
        "tickets_report",
        "zendesk_tickets_report",
        "scm_issues_time_resolution_report",
        PAGERDUTY_REPORT.RESPONSE_REPORTS,
        ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT,
        ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
        ISSUE_MANAGEMENT_REPORTS.HOPS_REPORT,
        ISSUE_MANAGEMENT_REPORTS.FIRST_ASSIGNEE_REPORT,
        ISSUE_MANAGEMENT_REPORTS.BOUNCE_REPORT
      ].includes(widget.type) &&
      [
        "ticket_created",
        ...GROUP_BY_TIME_FILTERS,
        ...AZURE_TIME_FILTERS_KEYS,
        ...SCM_PRS_TIME_FILTERS_KEYS,
        ...PAGERDUTY_TIME_FILTER_KEYS
      ].includes(acrossValue) &&
      !IGNORE_X_AXIS_KEYS.includes(x_axis)
    ) {
      const dependencyAnalysisFlag = widget.type === "tickets_report" && get(filters, ["filter", "links"], []).length;
      const resolved_at = get(filters, ["filter", `${acrossValue?.replace("_at", "")}_at`], undefined);
      const x_axis = get(drillDownProps, ["x_axis"], "");
      let interval = get(filters, ["filter", "interval"], "day");
      const weekStartsOnMonday = getWidgetConstant(widget.type, "weekStartsOnMonday", false);
      if (interval === "week" && weekStartsOnMonday) {
        interval = "isoWeek";
      }
      const weekDayFormat = get(metaData, "weekdate_format", WEEK_DATE_FORMAT.DATE);
      let gt: any = getMomentFromInterval(x_axis, interval, DateRange.FROM, weekDayFormat);
      let lt: any = getMomentFromInterval(x_axis, interval, DateRange.TO, weekDayFormat);
      if (resolved_at) {
        const startDate = moment.unix(resolved_at?.$gt || resolved_at?.$gte).utc();
        gt = gt.isAfter(startDate) ? gt : dependencyAnalysisFlag ? gt : startDate;
        const endDate = moment.unix(resolved_at?.$lt || resolved_at?.$lte).utc();
        lt = dependencyAnalysisFlag ? lt : lt.isAfter(endDate) ? endDate : lt;
      }

      let filterKey = `${acrossValue}_at`;
      if (widget.type === "zendesk_tickets_report") {
        filterKey = "created_at";
      }

      if (widget.type === PAGERDUTY_REPORT.RESPONSE_REPORTS) {
        filterKey = acrossValue;
      }

      if (
        [
          ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT,
          ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
          ISSUE_MANAGEMENT_REPORTS.HOPS_REPORT,
          ISSUE_MANAGEMENT_REPORTS.FIRST_ASSIGNEE_REPORT,
          ISSUE_MANAGEMENT_REPORTS.BOUNCE_REPORT
        ].includes(widget.type)
      ) {
        filterKey = acrossValue;
      }

      let ltKey = `$lt`;
      let gtKey = `$gt`;
      if (["tickets_report"].includes(widget.type) && filterKey === "issue_due_at") {
        ltKey = `$lte`;
        gtKey = `$gte`;
      }

      filters = {
        ...filters,
        filter: {
          ...filters?.filter,
          [filterKey]: {
            [ltKey]: lt?.unix()?.toString(),
            [gtKey]: gt?.unix()?.toString()
          }
        }
      };

      if (dependencyAnalysisFlag) {
        let widgetFilterOverRide = {};
        const acrossValueAt = filters.filter[filterKey];
        if (widget?.query[filterKey]) {
          widgetFilterOverRide = { [filterKey]: widget?.query[filterKey] }
        }
        filters = {
          ...filters,
          filter: {
            ...filters?.filter,
            ...widgetFilterOverRide,
            [`linked_${acrossValue}_at`]: acrossValueAt,
          }
        }
        unset(filters, ["filter", `linked_${acrossValue}`]);
      }
    }

    if (
      [JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_REPORT, ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_REPORT].includes(
        widget.type
      )
    ) {
      let x_axis = get(drillDownProps, ["x_axis"], "");
      let stage = get(drillDownProps, ["x_axis", "stage"], "");
      if (typeof x_axis === "object" && x_axis.hasOwnProperty("value")) {
        x_axis = get(x_axis, "value");
      }
      const stageKey = widget.type === ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_REPORT ? "workitem_stages" : "stages";
      const acrossKey =
        widget.type === ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_REPORT ? acrossValue : `${acrossValue}_at`;
      if ([...GROUP_BY_TIME_FILTERS, ...AZURE_TIME_FILTERS_KEYS].includes(acrossValue)) {
        let _format = DEFAULT_DATE_FORMAT;
        const resolved_at = get(filters, ["filter", acrossKey], undefined);
        let interval = get(filters, ["filter", "interval"], "day");
        const weekStartsOnMonday = getWidgetConstant(widget.type, "weekStartsOnMonday", false);
        if (interval === "week" && weekStartsOnMonday) {
          interval = "isoWeek";
        }

        let gt: any = getMomentFromInterval(x_axis, interval, DateRange.FROM);
        let lt: any = getMomentFromInterval(x_axis, interval, DateRange.TO);

        if (resolved_at) {
          const startDate = moment.unix(resolved_at?.$gt || resolved_at?.$gte);
          gt = gt.isAfter(startDate) ? gt : startDate;
          const endDate = moment.unix(resolved_at?.$lt);
          lt = lt.isAfter(endDate) ? endDate : lt;
        }

        let ltKey = `$lt`;
        let gtKey = `$gt`;
        filters = {
          ...filters,
          filter: {
            ...filters?.filter,
            [stageKey]: stage || [],
            [acrossKey]: {
              [ltKey]: lt?.unix()?.toString(),
              [gtKey]: gt?.unix()?.toString()
            }
          }
        };
      } else {
        const value = x_axis;
        const widgetConstantFilterValue = get(
          widgetConstants,
          [widget?.type || "", "valuesToFilters", acrossValue],
          undefined
        );
        const filterValue = widgetConstantFilterValue || get(valuesToFilters, [acrossValue], acrossValue);
        filters = {
          ...filters,
          filter: {
            ...filters?.filter,
            [stageKey]: stage || [],
            [filterValue]: [typeof value === "object" ? value?.id : value]
          }
        };
      }
    }

    if (
      ["github_commits_report", "github_commits_single_stat", "scm_rework_report"].includes(widget.type) &&
      ["trend"].includes(acrossValue)
    ) {
      const newAcross = "committed";
      let _format = DEFAULT_DATE_FORMAT;
      const committed_at = get(filters, ["filter", `${newAcross}_at`], undefined);
      const x_axis = get(drillDownProps, ["x_axis"], "");
      let interval = get(filters, ["interval"], "day");
      if (["scm_rework_report"].includes(widget.type)) {
        interval = get(filters, ["filter", "interval"], "day");
      }
      if (interval === "week") {
        interval = "isoWeek";
      }
      const weekDayFormat = get(metaData, "weekdate_format", WEEK_DATE_FORMAT.DATE);
      let gt: any = getMomentFromInterval(x_axis, interval, DateRange.FROM, weekDayFormat);
      let lt: any = getMomentFromInterval(x_axis, interval, DateRange.TO, weekDayFormat);

      if (committed_at) {
        const startDate = moment.unix(committed_at?.$gt || committed_at?.$gte);
        gt = gt.isAfter(startDate) ? gt : startDate;
        const endDate = moment.unix(committed_at?.$lt);
        lt = lt.isAfter(endDate) ? endDate : lt;
      }
      let ltKey = `$lt`;
      let gtKey = `$gt`;
      let filterKey = `${newAcross}_at`;
      filters = {
        ...filters,
        filter: {
          ...filters?.filter,
          [filterKey]: {
            [ltKey]: lt?.unix()?.toString(),
            [gtKey]: gt?.unix()?.toString()
          }
        }
      };
    }

    if (widget.type === JIRA_TICKETS_REPORT_NAME && acrossValue === "parent") {
      const x_axis = get(drillDownProps, ["x_axis"], undefined);
      if (x_axis) {
        !!filters?.filter?.parent && unset(filters?.filter, ["parent"]);
        filters = {
          ...filters,
          filter: {
            ...(filters?.filter || {}),
            parent_keys: [x_axis]
          }
        };
      }
    }

    if (
      [...GITHUB_PRS_REPORTS, JENKINS_REPORTS.SCM_PRS_RESPONSE_TIME_REPORT].includes(widget.type) &&
      PR_ACROSS_VALUES.includes(acrossValue)
    ) {
      let _format = DEFAULT_DATE_FORMAT;
      const resolved_at = get(filters, ["filter", `${acrossValue}_at`], undefined);
      const x_axis = get(drillDownProps, ["x_axis"], "");
      let interval = get(filters, ["filter", "interval"], "day");
      const weekStartsOnMonday = getWidgetConstant(widget.type, "weekStartsOnMonday", false);
      if (interval === "week" && weekStartsOnMonday) {
        interval = "isoWeek";
      }

      let gt: any = getMomentFromInterval(x_axis, interval, DateRange.FROM);
      let lt: any = getMomentFromInterval(x_axis, interval, DateRange.TO);

      if (resolved_at) {
        const startDate = moment.unix(resolved_at?.$gt);
        gt = gt.isAfter(startDate) ? gt : startDate.add(moment().utcOffset(), "m");
      }

      if (filters.filter[acrossValue]) {
        unset(filters.filter, [acrossValue]);
      }

      filters = {
        ...filters,
        filter: {
          ...filters?.filter,
          [`${acrossValue}_at`]: {
            $lt: lt?.unix()?.toString(),
            $gt: gt?.unix()?.toString()
          }
        }
      };
    }

    if (ALL_VELOCITY_PROFILE_REPORTS.includes(widget.type)) {
      const x_axis = get(drillDownProps, ["x_axis"], "");
      delete filters?.filter?.values;

      let workItemsType = getWorkItemsType(drillDownProps.application);
      if (workItemsType) {
        set(filters, ["filter", "work_items_type"], workItemsType);
      }

      let stageKey: any = "value_stage_names";

      if (LEAD_TIME_BY_STAGE_REPORTS.includes(widget.type)) {
        stageKey = LEAD_TIME_STAGE_FILTER_KEY;
        if (x_axis === "Total") {
          stageKey = null;
        }
      }

      if (typeof x_axis === "object") {
        let stageNameValue = x_axis?.stageName
          ? stageKey === LEAD_TIME_STAGE_FILTER_KEY
            ? x_axis.stageName
            : [x_axis.stageName]
          : undefined;
        filters = {
          ...filters,
          across: "values",
          filter: {
            ...filters?.filter,
            [stageKey]: stageNameValue,
            value_jira_issue_types: x_axis?.taskType ? [x_axis.taskType] : undefined,
            value_trend_keys: x_axis?.value ? [x_axis.value] : undefined
          }
        };
      } else if (LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT !== widget.type) {
        let stageNameValue = stageKey === LEAD_TIME_STAGE_FILTER_KEY ? x_axis : [x_axis];
        filters = {
          ...filters,
          across: "values",
          filter: {
            ...filters?.filter,
            [stageKey]: stageNameValue
          }
        };
      }
    }

    if ([...issueManagementReports, ...ALL_VELOCITY_PROFILE_REPORTS].includes(widget.type as any)) {
      const customFields = get(filters, ["filter", "custom_fields"], {});
      const excludeFields = get(filters, ["filter", "exclude"], {});
      const excludeCustomFields = get(filters, ["filter", "exclude", "custom_fields"], {});
      const partialFilterKey = getWidgetConstant(widget.type, PARTIAL_FILTER_KEY, JIRA_SCM_COMMON_PARTIAL_FILTER_KEY);
      let partialFields = get(filters, ["filter", partialFilterKey], {});
      let custom_field_prefix = "jira_";
      let custom_field_key = reportCustomFieldKey || "jira_custom_fields";
      if ([...issueManagementReports, ...azureLeadTimeIssueReports].includes(widget.type as any)) {
        custom_field_key = "workitem_custom_fields";
        custom_field_prefix = "workitem_";
      }
      if (LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT.includes(widget.type as string)) {
        custom_field_key = "custom_fields";
        custom_field_prefix = "";
      }
      if (Object.keys(partialFields).length > 0 || filters.filter.hasOwnProperty("custom_fields")) {
        Object.keys(partialFields || {}).forEach(field => {
          if (field.includes(CUSTOM_FIELD_PREFIX)) {
            const val = partialFields[field];
            delete partialFields[field];
            partialFields = {
              ...partialFields,
              [`${custom_field_prefix}${field}`]: val
            };
          }
        });
        filters = {
          ...(filters || {}),
          filter: {
            ...(filters?.filter || {}),
            [partialFilterKey]: {
              ...(partialFields || {})
            }
          }
        };
      }
      if (Object.keys(customFields).length > 0) {
        delete filters.filter.custom_fields;
        filters = {
          ...(filters || {}),
          filter: {
            ...(filters?.filter || {}),
            [custom_field_key]: {
              ...(customFields || {})
            }
          }
        };
      }
      if (Object.keys(excludeFields).length > 0 && excludeFields?.custom_fields) {
        delete filters.filter.exclude.custom_fields;
        filters = {
          ...(filters || {}),
          filter: {
            ...(filters?.filter || {}),
            exclude: {
              ...filters.filter.exclude,
              [custom_field_key]: { ...excludeCustomFields }
            }
          }
        };
      }
    }

    if (widget.type === "tickets_report") {
      const updatedFilters = Object.keys(filters.filter).reduce((acc: any, next: any) => {
        if (next === "resolutions" && filters.filter[next].includes("UNRESOLVED")) {
          let resolutions = filters.filter[next].filter((item: any) => item !== "UNRESOLVED");
          resolutions.push("");
          return { ...acc, [next]: [...resolutions] };
        }
        return { ...acc, [next]: filters.filter[next] };
      }, {});
      filters = {
        ...filters,
        filter: { ...updatedFilters }
      };
    }

    if (scmCicdReportTypes.includes(widget.type) && (acrossValue === "job_end" || acrossValue === "trend")) {
      let _format = DEFAULT_DATE_FORMAT;
      const x_axis = get(drillDownProps, ["x_axis"], "");
      let interval = get(filters, ["filter", "interval"], "day");

      switch (interval) {
        case "day":
        case "week":
          _format = DateFormats.DAY;
          break;
        case "month":
          _format = DateFormats.MONTH;
          break;
        case "quarter":
          _format = DateFormats.QUARTER;
          break;
      }

      let gt: any = getMomentFromInterval(x_axis, interval, DateRange.FROM);
      let lt: any = getMomentFromInterval(x_axis, interval, DateRange.TO);

      filters = {
        ...filters,
        filter: {
          ...filters?.filter,
          end_time: {
            $lt: lt?.unix()?.toString(),
            $gt: gt?.unix()?.toString()
          }
        }
      };
    }
    if (JENKINS_AZURE_REPORTS.includes(widget.type)) {
      filters = {
        ...filters,
        filter: {
          ...filters?.filter,
          cicd_integration_ids: get(dashboardQuery, ["integration_ids"], []),
          integration_ids: get(dashboardQuery, ["integration_ids"], [])
        }
      };
    }

    if (widget.type === SCM_FILES_REPORT) {
      filters = {
        ...filters,
        filter: {
          ...filters?.filter,
          filename: drillDownProps?.additionFilter?.filename
        }
      };
    }

    if ([JIRA_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT].includes(widget.type)) {
      filters = {
        ...filters,
        filter: {
          ...filters?.filter,
          include_issue_keys: true,
          include_total_count: true
        }
      };
    }

    if ([AZURE_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT].includes(widget.type)) {
      filters = {
        ...filters,
        filter: {
          ...filters?.filter,
          include_workitem_ids: true
        }
      };
    }

    if (
      !get(drillDownProps?.widgetMetaData || {}, "disable_or_filters", false) &&
      OR_QUERY_APPLICATIONS.filter(key => drillDownProps?.application.includes(key)).length > 0
    ) {
      let key = drillDownProps?.application === IntegrationTypes.JIRA ? "or" : "jira_or";
      const jiraOrFilterKey = get(widgetConstants, [widget?.type, "jira_or_filter_key"]);
      if (jiraOrFilterKey) {
        key = jiraOrFilterKey;
      }
      filters = {
        ...(filters || {}),
        filter: {
          ...(filters?.filter || {}),
          [key]: removeFiltersWithEmptyValues(get(dashboardMetadata, ["jira_or_query"], {}))
        }
      };
    }

    if (
      issueManagementReports.includes(widget.type) &&
      drillDownProps.application.includes(IntegrationTypes.AZURE) &&
      filters.across === "priority"
    ) {
      const workItemPrioritiesValue = (filters.filter.workitem_priorities ?? [])
        .filter((priority: string) => isSanitizedValue(priority))
        .map((priority: string) => {
          const value = get(PriorityOrderMapping, [priority], priority);
          return value.toString();
        });
      filters = {
        ...(filters || {}),
        filter: {
          ...(filters?.filter || {}),
          workitem_priorities: workItemPrioritiesValue
        }
      };
    }

    let updatedFilter = removeEmptyKeys(filters?.filter);

    // these two keys are only supported by azure reports
    ["teams", "code_area"].forEach((key: string) => {
      if (updatedFilter.hasOwnProperty(key)) {
        updatedFilter = {
          ...updatedFilter,
          workitem_attributes: {
            ...(updatedFilter.workitem_attributes || {}),
            [key]: updatedFilter[key]
          }
        };

        unset(updatedFilter, key);
      }
    });
    const azureCodeAreaValues: any = get(updatedFilter, ["workitem_attributes", "code_area"], undefined);
    if (azureCodeAreaValues) {
      const newAzureCodeAreaValues = azureCodeAreaValues.map((value: any) => {
        if (typeof value === "object") {
          return `${value.child}`;
        } else {
          // This is just for backward compatibility with old version that had string values
          return value;
        }
      });
      let key = "workitem_attributes";
      unset(updatedFilter, ["workitem_attributes", "code_area"]);
      updatedFilter = {
        ...(updatedFilter || {}),
        [key]: {
          ...updatedFilter.workitem_attributes,
          ["code_area"]: newAzureCodeAreaValues
        }
      };

      // Also updating filters for code_area
      // Not writing generic code because it might effect other reports
      filters = {
        ...filters,
        filter: {
          ...(filters.filter || {}),
          workitem_attributes: {
            ...(filters?.filter?.workitem_attributes || {}),
            ["code_area"]: newAzureCodeAreaValues
          }
        }
      };
    }

    const azureIterationValues: any = get(updatedFilter, ["azure_iteration"], undefined);
    const excludeAzureIterationValues: any = get(updatedFilter, ["exclude", "azure_iteration"], undefined);
    const workitemSprintFullNames: string[] = get(updatedFilter, ["workitem_sprint_full_names"], undefined);
    const partialAzureIterationValue: any = get(updatedFilter, ["partial_match", "azure_iteration"], undefined);
    if (excludeAzureIterationValues) {
      if (!workitemSprintFullNames) {
        const newExcludeAzureIterationValues = excludeAzureIterationValues.map((value: any) => {
          if (typeof value === "object") {
            return `${value.parent}\\${value.child}`;
          } else {
            // This is just for backward compatibility with old version that had string values
            return value;
          }
        });
        let key = "workitem_sprint_full_names";
        updatedFilter = {
          ...(updatedFilter || {}),
          exclude: {
            ...updatedFilter.exclude,
            [key]: newExcludeAzureIterationValues
          }
        };
      }

      unset(updatedFilter, ["exclude", "azure_iteration"]);
    }
    if (partialAzureIterationValue) {
      unset(updatedFilter, ["partial_match", "azure_iteration"]);
      updatedFilter = {
        ...(updatedFilter || {}),
        partial_match: {
          ...updatedFilter?.partial_match,
          workitem_milestone_full_name: partialAzureIterationValue
        }
      };
    }

    if (azureIterationValues) {
      if (!workitemSprintFullNames) {
        const newAzureIterationValues = azureIterationValues.map((value: any) => {
          if (typeof value === "object") {
            return `${value.parent}\\${value.child}`;
          } else {
            // This is just for backward compatibility with old version that had string values
            return value;
          }
        });
        let key = "workitem_sprint_full_names";

        updatedFilter = {
          ...(updatedFilter || {}),
          [key]: newAzureIterationValues
        };
      }
      unset(updatedFilter, ["azure_iteration"]);
    }

    if (["sprint_goal"].includes(widget?.type)) {
      const state = get(updatedFilter, ["state"], "");
      if (state === "active") {
        unset(updatedFilter, ["completed_at"]);
      }
      const sprint_filter_val = get(updatedFilter, ["sprint"], null);
      if (sprint_filter_val !== null) {
        const partialFilterKey = getWidgetConstant(widget.type, PARTIAL_FILTER_KEY, JIRA_SCM_COMMON_PARTIAL_FILTER_KEY);
        unset(updatedFilter, ["sprint"]);
        updatedFilter = {
          ...updatedFilter,
          [partialFilterKey]: {
            ...updatedFilter[partialFilterKey],
            ["sprint"]: { $contains: sprint_filter_val }
          }
        };
      }
      if (get(updatedFilter, "jira_sprint_states", undefined)) {
        const filterValue = updatedFilter["jira_sprint_states"];
        updatedFilter["state"] = filterValue[0];
        unset(updatedFilter, "jira_sprint_states");
      }
    }

    unset(updatedFilter, [WIDGET_DATA_SORT_FILTER_KEY]);
    unset(updatedFilter, ["teams"]);

    if (
      [
        "sprint_metrics_percentage_trend",
        "azure_sprint_metrics_percentage_trend",
        "sprint_metrics_trend",
        "azure_sprint_metrics_trend"
      ].includes(widget.type) &&
      ["week", "bi_week", "month"].includes(acrossValue)
    ) {
      const mapping: any = {
        week: "isoWeek",
        bi_week: "isoWeek",
        month: "month"
      };
      const x_axis = get(drillDownProps, ["x_axis"], undefined);
      const gt = moment.utc(x_axis).startOf(mapping[acrossValue]).unix();
      let lt = moment.utc(x_axis).endOf(mapping[acrossValue]).unix();

      if (["bi_week"].includes(acrossValue)) {
        lt = moment.utc(x_axis).add("week", 1).endOf(mapping[acrossValue]).unix();
      }

      updatedFilter = {
        ...(updatedFilter || {}),
        completed_at: {
          $gt: gt.toString(),
          $lt: lt.toString()
        }
      };
    }
    const filterKeyMapping = getWidgetConstant(widget.type, FILTER_KEY_MAPPING, {});

    if (Object.keys(filterKeyMapping).length) {
      Object.keys(filterKeyMapping).forEach(key => {
        const val = get(updatedFilter, [key], null);
        if (val !== null) {
          unset(updatedFilter, [key]);
          updatedFilter = {
            ...updatedFilter,
            [filterKeyMapping[key]]: val
          };
        }
        const partialFilterKey = getWidgetConstant(widget.type, PARTIAL_FILTER_KEY, JIRA_SCM_COMMON_PARTIAL_FILTER_KEY);
        const partial_val = get(updatedFilter, [partialFilterKey, key], null);
        if (partial_val !== null) {
          unset(updatedFilter, [partialFilterKey, key]);
          updatedFilter = {
            ...updatedFilter,
            [partialFilterKey]: {
              ...updatedFilter[partialFilterKey],
              [filterKeyMapping[key]]: partial_val
            }
          };
        }
        const exclude_val = get(updatedFilter, ["exclude", key], null);
        if (exclude_val !== null) {
          unset(updatedFilter, ["exclude", key]);
          updatedFilter = {
            ...updatedFilter,
            ["exclude"]: {
              ...updatedFilter["exclude"],
              [filterKeyMapping[key]]: exclude_val
            }
          };
        }
      });
    }

    const interval = get(filters, ["filter", "interval"], "");
    if (["code_volume_vs_deployment_report"].includes(widget.type)) {
      const mapping: any = {
        weekly: "week",
        "bi-weekly": "biweekly",
        monthly: "month",
        week: "week",
        biweekly: "biweekly",
        month: "month"
      };

      delete updatedFilter?.job_end;
      const x_axis = get(drillDownProps, ["x_axis"], undefined);
      let gt = x_axis?.id;
      const weekStartsOnMonday = getWidgetConstant(widget.type, "weekStartsOnMonday", false);
      const intervalMapping = mapping[interval] === "week" && weekStartsOnMonday ? "isoWeek" : mapping[interval];
      if (typeof x_axis === "string") {
        gt = JSON.parse(x_axis)?.id;
      }
      let lt: any = getMomentFromInterval(moment.unix(gt).toString(), intervalMapping, DateRange.TO);

      if (intervalMapping === "biweekly") {
        lt = moment.unix(gt).utc().add("week", 1).endOf("W").unix();
      }
      filters = {
        ...filters,
        interval: get(CodeVolumeVsDeployementIntervalMapping, [filters.interval], filters.interval)
      };

      updatedFilter = {
        ...(updatedFilter || {}),
        end_time: {
          $gt: gt?.toString(),
          $lt: lt?.toString()
        }
      };
    }

    const supportedCustomFields = drillDownProps?.supportedCustomFields || [];

    if (isArray(supportedCustomFields) && supportedCustomFields.length) {
      filters = sanitizeCustomDateFilters({ ...filters, filter: { ...updatedFilter } }, supportedCustomFields);
    } else {
      filters = { ...filters, filter: { ...updatedFilter } };
    }

    if (["jira_velocity"].includes(drillDownProps?.application) && filters?.filter?.sprint_states) {
      filters = {
        ...filters,
        filter: { ...filters.filter, [ACTIVE_SPRINT_TYPE_FILTER_KEY]: filters?.filter?.sprint_states }
      };
      unset(filters, ["filter", "sprint_states"]);
    }

    // handling existing values
    if (
      [IntegrationTypes.JIRA].includes(drillDownProps?.application as IntegrationTypes) &&
      filters?.filter?.[ACTIVE_SPRINT_TYPE_FILTER_KEY]
    ) {
      filters = {
        ...filters,
        filter: { ...filters.filter, sprint_states: filters?.filter?.[ACTIVE_SPRINT_TYPE_FILTER_KEY] }
      };
      unset(filters, ["filter", ACTIVE_SPRINT_TYPE_FILTER_KEY]);
    }

    const ou_ids = queryParamOU ? [queryParamOU] : get(dashboardMetadata, "ou_ids", []);

    const application = get(widgetConstants, [widget?.type, "application"], "");
    if (
      ou_ids.length &&
      (APPLICATIONS_SUPPORTING_OU_FILTERS.includes(application) ||
        [PAGERDUTY_REPORT.RESPONSE_REPORTS].includes(widget?.type))
    ) {
      filters = {
        ...filters,
        ou_ids
      };

      let combinedOUFilters = {
        ...get(dashboardMetadata, "ou_user_filter_designation", {}),
        ...sanitizeObject(get(drillDownProps?.widgetMetaData, "ou_user_filter_designation", {})),
        ...sanitizeObject(get(metaData, "ou_user_filter_designation", {}))
      };

      const supportedApplications = getSupportedApplications(widget?.type);
      const widgetConstantFilterValue = get(
        widgetConstants,
        [widget?.type || "", "valuesToFilters", acrossValue],
        undefined
      );
      const filterValue = widgetConstantFilterValue || get(valuesToFilters, [acrossValue], acrossValue);

      Object.keys(combinedOUFilters).forEach((key: string) => {
        if (!supportedApplications.includes(key)) {
          delete combinedOUFilters?.[key];
        }
      });

      if ([IntegrationTypes.JIRA, IntegrationTypes.AZURE, IntegrationTypes.GITHUBJIRA].includes(application)) {
        let sprint = undefined;
        if (azureIterationSupportableReports.includes(widget?.type as any)) {
          sprint = "sprint_report";
        } else {
          const sprintCustomField = (drillDownProps?.supportedCustomFields || []).find((item: any) =>
            (item.name || "").toLowerCase().includes("sprint")
          );
          if (sprintCustomField) {
            sprint = sprintCustomField?.field_key;
          }
        }
        combinedOUFilters = {
          ...combinedOUFilters,
          sprint: [sprint]
        };
      }
      if (
        combinedOUFilters?.hasOwnProperty("sprint") &&
        (!combinedOUFilters?.sprint || !combinedOUFilters?.sprint?.[0])
      ) {
        delete combinedOUFilters?.sprint;
      }

      if (Object.keys(combinedOUFilters).length) {
        filters = {
          ...filters,
          ou_user_filter_designation: combinedOUFilters
        };
      }
      const includeAcrossInOUExclusions = get(widgetConstants, [widget.type, INCLUDE_ACROSS_OU_EXCLUSIONS], true);

      const ouExclusions = includeAcrossInOUExclusions ? filterValue : undefined;

      if (ouExclusions?.length && !allTimeFilterKeys.includes(acrossValue)) {
        filters = {
          ...filters,
          ou_exclusions: [ouExclusions]
        };
      }
    }

    const removeNoLongerSupportedFilter = get(widgetConstants, [widget.type, NO_LONGER_SUPPORTED_FILTER], undefined);
    if (removeNoLongerSupportedFilter) {
      filters = {
        ...filters,
        filter: { ...removeNoLongerSupportedFilter(filters.filter) }
      };
    }

    // key for custom fields stacks is stacks itself not custom_stacks (jira)
    // this is the check for fixing the same
    if (issueManagementReports.includes(widget.type)) {
      const customStacks = get(filters, ["filter", "custom_stacks"], []);
      const stacks = get(filters, ["stacks"], []).filter((st: string) => st !== CUSTOM_FIELD_STACK_FLAG);
      if (customStacks.length) {
        set(filters, "stacks", customStacks);
        unset(filters, ["filter", "custom_stacks"]);
      } else if (!stacks.length) {
        unset(filters, ["filter", "custom_stacks"]);
        unset(filters, ["stacks"]);
      }
    }

    if (JIRA_SPRINT_DISTRIBUTION_REPORTS.SPRINT_DISTRIBUTION_REPORT.includes(widget.type)) {
      if (typeof drillDownProps?.x_axis === "string") {
        drillDownProps.x_axis = JSON.parse(drillDownProps?.x_axis);
      }
      filters.filter.keys = drillDownProps?.x_axis?.keys;
    }
    // can't take risk to use this for all of the reports
    // so using this for current use case
    if (["github_commits_report", "github_commits_single_stat"].includes(widget.type)) {
      filters = {
        ...(filters || {}),
        filter: {
          ...sanitizeTimeFilters(filters?.filter || {}, ["committed_at"])
        }
      };
    }

    const resolutionFilter = get(filters, ["filter", "resolutions"], undefined);
    if (resolutionFilter) {
      filters.filter["resolutions"] = resolutionFilter.map((resolution: string) =>
        resolution === "UNRESOLVED" ? "" : resolution
      );
    }

    // Getting all available applications.
    const availableApplications: string[] | undefined = drillDownProps?.availableIntegrations?.map(
      (integration: any) => integration?.application
    );
    // If dashboard doesn't have any integrations to it, availableApplications try to fetch all the possible integrations
    const allowedIntegrations = get(filters, ["filter", "integration_ids"], []);
    // currently filtering integration ids only for lead time reports based on issue management system.
    if (
      jiraAzureScmAllLeadTimeReportsApplication.includes(drillDownProps.application as IntegrationTypes) &&
      availableApplications?.includes(IntegrationTypes.JIRA) &&
      availableApplications?.includes(IntegrationTypes.AZURE) &&
      allowedIntegrations.length > 0
    ) {
      let newIntegrationIds: string[] = [];
      forEach(drillDownProps.availableIntegrations, (integration: any) => {
        let valid = false;
        if (application === IntegrationTypes.GITHUBJIRA) {
          valid = [IntegrationTypes.GITHUB, IntegrationTypes.JIRA].includes(
            integration?.application as IntegrationTypes
          );
        } else if (application == IntegrationTypes.GITHUB) {
          // azure_devops is also supported by github widgets
          valid = [IntegrationTypes.GITHUB, IntegrationTypes.AZURE, ...GITHUB_APPLICATIONS].includes(
            integration?.application as IntegrationTypes
          );
        } else {
          valid = integration?.application === application;
        }
        if (valid) {
          newIntegrationIds.push(integration?.id);
        }
      });

      filters = {
        ...filters,
        filter: {
          ...get(filters, ["filter"], {}),
          integration_ids: newIntegrationIds
        }
      };
    }

    filters = leadtimeOUDesignationQueryBuilder(filters, widget?.type, drillDownProps.availableIntegrations);

    const includeIntervalInPayload = get(widgetConstants, [widget?.type, INCLUDE_INTERVAL_IN_PAYLOAD], false);

    if (includeIntervalInPayload) {
      unset(filters, ["filter", "interval"]);
      filters = {
        ...filters,
        interval
      };
    } else {
      unset(filters, ["filter", "interval"]);
      unset(filters, ["interval"]);
    }

    if (metaData.hasOwnProperty("apply_ou_on_velocity_report")) {
      filters = {
        ...filters,
        apply_ou_on_velocity_report: metaData.apply_ou_on_velocity_report
      };
    }
    const doraProfileIntegrationType = drillDownProps?.doraProfileIntegrationType;
    if (
      (issueManagementReports.includes(widget.type) && application === ADO_APPLICATION) ||
      (application === "any" && doraProfileIntegrationType === IM_ADO)
    ) {
      filters = transformAzureWidgetQueryForCustomFields(filters, drillDownProps?.supportedCustomFields ?? []);
      const widgetFiltersTransformer = get(widgetConstants, [widget.type, "widget_filter_transform"]);
      if (widgetFiltersTransformer) {
        filters = widgetFiltersTransformer(filters);
      }
    }

    if (TESTRAILS_REPORTS.TESTRAILS_TESTS_TRENDS_REPORT.includes(widget?.type) && x_axis) {
      let gt: any = getMomentFromInterval(x_axis, TIME_INTERVAL_TYPES.DAY, DateRange.FROM);
      let lt: any = getMomentFromInterval(x_axis, TIME_INTERVAL_TYPES.DAY, DateRange.TO);
  
      let filterKey: string = '';
      if (filters?.filter?.metric === 'test_case_count') filterKey = 'created_on';
  
      if (filters?.filter?.metric === 'test_count') filterKey = 'created_at';
  
      if (filterKey) {
        filters = {
          ...filters,
          filter: {
            ...filters.filter,
            [filterKey]: {
              ['$lt']: moment.isMoment(lt) ? lt.unix().toString() : moment(lt).unix().toString(),
              ['$gt']: moment.isMoment(gt) ? gt.unix().toString() : moment(gt).unix().toString()
            }
          }
        };
      }
    }

    return { acrossValue, filters };
  }
  return {};
};

export const getWorkItemsType = (application: string) => {
  if ([IntegrationTypes.AZURE].includes(application as IntegrationTypes)) return "work_item";
  if ([IntegrationTypes.JIRA, IntegrationTypes.JIRA_VELOCITY].includes(application as IntegrationTypes))
    return IntegrationTypes.JIRA;

  return null;
};

export const TicketLifeTimeCloumn = (xaxis: string | undefined, recordKey: string) => {
  return {
    ...baseColumnConfig("Ticket Lifetime", "ticket_lifetime"),
    render: (value: any, record: any) => {
      const endTimeStamp = typeof xaxis === "string" ? parseInt(xaxis) : xaxis;
      const timeStamp = recordKey === "issue_created_at" ? record[recordKey] : record[recordKey] / 1000;
      const startdate = moment.unix(timeStamp).utc().format(DateFormats.DAY);
      const endDate = moment
        .unix(endTimeStamp ?? moment.now())
        .utc()
        .format(DateFormats.DAY);

      const start = moment(startdate, DateFormats.DAY);
      const end = moment(endDate, DateFormats.DAY);
      let days = moment.duration(end.diff(start)).asDays();
      days = Math.round(days);
      return days === 1 ? days + " Day" : days + " Days";
    }
  };
};

export enum JenkinsBuildTypeReportsType {
  PIPELINE_JOBS_DURATION_REPORT = "cicd_pipeline_jobs_duration_report",
  PIPELINE_JOBS_DURATION_TREND_REPORT = "cicd_pipeline_jobs_duration_trend_report",
  SCM_JOBS_COUNT_REPORT = "cicd_scm_jobs_count_report",
  PIPELINE_JOBS_COUNT_REPORT = "cicd_pipeline_jobs_count_report",
  PIPELINE_JOBS_COUNT_TREND_REPORT = "cicd_pipeline_jobs_count_trend_report"
}

export const jenkinsBuildTypeReports = [
  JenkinsBuildTypeReportsType.PIPELINE_JOBS_COUNT_REPORT,
  JenkinsBuildTypeReportsType.PIPELINE_JOBS_COUNT_TREND_REPORT,
  JenkinsBuildTypeReportsType.PIPELINE_JOBS_DURATION_REPORT,
  JenkinsBuildTypeReportsType.PIPELINE_JOBS_DURATION_TREND_REPORT,
  JenkinsBuildTypeReportsType.SCM_JOBS_COUNT_REPORT
];

export const DRILLDOWN_UUID = "drilldown-preview";

export const getCategoryColorMapping = (categories: RestTicketCategorizationCategory[]) => {
  let mapping: basicMappingType<string> = {};
  forEach(categories ?? [], category => {
    mapping[category?.name ?? ""] = category?.background_color ?? get(category, ["color"], "");
  });
  return mapping;
};

export const getAcross = (across: any, widget: any) => {
  let acrossValue = across;
  if (widget && (widget.type === "hygiene_report" || widget.type === "azure_hygiene_report")) {
    acrossValue = get(widget, ["query", "across"], "");
  }
  return acrossValue;
};

export const getSupportedCustomFields = (
  azureFieldsSelector: any,
  jiraFieldsSelector: any,
  zendeskFieldsSelector: any,
  testrailsFieldsSelector: any,
  application: string
) => {
  let customFields: IntegrationTransformedCFTypes[] = [];
  if (application === IntegrationTypes.JIRA) {
    customFields = [...customFields, ...get(jiraFieldsSelector, "data", [])];
  }

  if (application === IntegrationTypes.AZURE) {
    customFields = [...customFields, ...get(azureFieldsSelector, "data", [])];
  }

  if (application === IntegrationTypes.ZENDESK) {
    customFields = [...customFields, ...get(zendeskFieldsSelector, "data", [])];
  }

  if (application === IntegrationTypes.TESTRAILS) {
    customFields = [...customFields, ...get(testrailsFieldsSelector, "data", [])];
  }
  return customFields;
};
