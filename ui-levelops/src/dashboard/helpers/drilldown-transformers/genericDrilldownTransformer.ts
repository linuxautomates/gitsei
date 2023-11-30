import { get, isEmpty, unset } from "lodash";
import { AZURE_CUSTOM_FIELD_PREFIX, AZURE_ISSUE_CUSTOM_FIELD_PREFIX, TESTRAILS_CUSTOM_FIELD_PREFIX, valuesToFilters } from "dashboard/constants/constants";
import widgetConstants from 'dashboard/constants/widgetConstants'
import { combineAllFilters } from "shared-resources/containers/widget-api-wrapper/helper";
import {
  JENKINS_REPORTS,
  leadTimeReports,
  azureLeadTimeIssueReports,
  issueManagementReports,
  JIRA_SPRINT_DISTRIBUTION_REPORTS,
  IMPLICITY_INCLUDE_DRILLDOWN_FILTER
} from "../../constants/applications/names";
import {
  AZURE_TIME_FILTERS_KEYS,
  GROUP_BY_TIME_FILTERS,
  PAGERDUTY_TIME_FILTER_KEYS,
  SCM_PRS_TIME_FILTERS_KEYS
} from "constants/filters";
import { TESTRAILS_REPORTS } from "../../constants/applications/names";

export const genericDrilldownTransformer = (drillDownData: any) => {
  const { drillDownProps, widget, dashboardQuery, contextFilters } = drillDownData;
  let { x_axis, supportedCustomFields } = drillDownProps || {};
  let { across, ...data } = widget?.query || {};
  const widgetFilter = get(widgetConstants, [widget?.type || "", "filters"], {});
  const drilldown = get(widgetConstants, [widget?.type, "drilldown"], {});
  const includeContextFilter = get(widgetConstants, [widget?.type, "includeContextFilter"], false);
  if (widgetFilter.across) {
    // Some reports have hardcoded user-unchangable across that comes from widgetConstants.
    across = widgetFilter.across;
  }

  const hiddenFilters = get(widgetConstants, [widget?.type || "", "hidden_filters"], {});

  const initialFilters = combineAllFilters(data, widgetFilter, hiddenFilters);
  const implicityIncludeDrilldownFilter = get(widgetConstants, [widget?.type, IMPLICITY_INCLUDE_DRILLDOWN_FILTER], {});

  let filters: { [x: string]: any } = {
    filter: {
      ...(initialFilters || {}),
      ...(dashboardQuery || {}),
      ...implicityIncludeDrilldownFilter
    },
    across
  };

  if (includeContextFilter) {
    filters = {
      ...filters,
      filter: {
        ...filters.filter,
        ...contextFilters
      }
    };
  }

  if ([...azureLeadTimeIssueReports, ...leadTimeReports].includes(widget?.type || "")) {
    across = "values";
  }

  if (
    across &&
    x_axis &&
    typeof x_axis === "string" &&
    (
      ![
        "trend",
        "issue_updated",
        "issue_created",
        "ticket_created",
        "first_comment",
        "column",
        "links",
        ...PAGERDUTY_TIME_FILTER_KEYS,
        ...GROUP_BY_TIME_FILTERS,
        ...SCM_PRS_TIME_FILTERS_KEYS,
        ...AZURE_TIME_FILTERS_KEYS,
        AZURE_CUSTOM_FIELD_PREFIX
      ].includes(across) ||
      (widget?.type === "tickets_report" && get(filters, ["filter", "links"], []).length)
    )
  ) {
    const widgetConstantFilterValue = get(widgetConstants, [widget?.type || "", "valuesToFilters", across], undefined);
    let filterValue = widgetConstantFilterValue || get(valuesToFilters, [across], across);
    const changeUnassigned =
      x_axis &&
      x_axis.includes("UNASSIGNED") &&
      ![
        JENKINS_REPORTS.CICD_SCM_JOBS_DURATION_REPORT,
        JENKINS_REPORTS.CICD_JOBS_COUNT_REPORT,
        JENKINS_REPORTS.CICD_PIPELINE_JOBS_DURATION_REPORT,
        TESTRAILS_REPORTS.TESTRAILS_TESTS_REPORT,
        TESTRAILS_REPORTS.TESTRAILS_TESTS_ESTIMATE_REPORT,
        TESTRAILS_REPORTS.TESTRAILS_TESTS_ESTIMATE_FORECAST_REPORT
      ].includes(widget?.type);
    let acrossFilterValue: any = [changeUnassigned ? "_UNASSIGNED_" : x_axis] || [];

    if (issueManagementReports.includes(widget?.type) && filterValue === "workitem_story_points") {
      acrossFilterValue = { $gte: x_axis, $lte: x_axis };
    }

    if (["reviewer_count", "approver_count"].includes(across)) {
      filterValue = across === "reviewer_count" ? "num_reviewers" : "num_approvers";
      acrossFilterValue = { $gte: x_axis, $lte: x_axis };
    }

    if (widget?.type === "tickets_report" && get(filters, ["filter", "links"], []).length) {
      filterValue = `linked_${filterValue}`;
    }

    if (["assignee", "first_assignee"].includes(across) && acrossFilterValue.includes("_UNASSIGNED_")) {
      filterValue = "missing_fields";
      acrossFilterValue = {
        ...(filters.filter?.missing_fields || {}),
        [across]: true
      };
    }

    if ([...issueManagementReports, ...azureLeadTimeIssueReports].includes(widget.type as any) && across.includes(AZURE_ISSUE_CUSTOM_FIELD_PREFIX) && !across.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
      across = `Custom.${across}`;
    }

    if (filterValue.includes("customfield_") || filterValue.includes("Custom.") || filterValue.includes(TESTRAILS_CUSTOM_FIELD_PREFIX)) {

      let findCustomValue = (supportedCustomFields || [])?.find((customVal: any) => customVal?.field_key === filterValue);

      if (!isEmpty(findCustomValue)) {
        acrossFilterValue = findCustomValue?.field_type === 'CHECKBOX' ? acrossFilterValue[0] : acrossFilterValue;
      }

      filters = {
        filter: {
          ...filters.filter,
          custom_fields: { ...filters.filter.custom_fields, [filterValue]: acrossFilterValue }
        },
        across
      };
    } else {
      filters = {
        filter: {
          ...filters.filter,
          [filterValue]: acrossFilterValue
        },
        across
      };
    }
  }

  if (typeof x_axis === "object" && ["_UNASSIGNED_"].includes(x_axis?.name)) {
    const acrossFilterValue = {
      ...(filters?.filter?.missing_fields || {}),
      [across]: true
    };
    filters = {
      filter: {
        ...filters.filter,
        ["missing_fields"]: acrossFilterValue
      },
      across
    };
  }
  if (JIRA_SPRINT_DISTRIBUTION_REPORTS.SPRINT_DISTRIBUTION_REPORT.includes(widget?.type)) {
    across = drillDownProps?.x_axis?.across;
  }
  // handling case where sort is present multiple times
  if (get(drilldown, ["defaultSort"], undefined)) {
    unset(filters, ["filter", "sort"]);
  }

  return { acrossValue: across, filters };
};
