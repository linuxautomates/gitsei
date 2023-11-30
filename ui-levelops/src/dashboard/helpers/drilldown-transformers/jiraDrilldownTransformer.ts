import { AZURE_CUSTOM_FIELD_PREFIX, valuesToFilters } from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import { forEach, get, set, unset } from "lodash";
import { combineAllFilters } from "shared-resources/containers/widget-api-wrapper/helper";
import { genericDrilldownTransformer } from "./genericDrilldownTransformer";
import moment from "moment";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  BA_COMPLETED_WORK_STATUS_BE_KEY,
  BA_EFFORT_ATTRIBUTION_BE_KEY,
  BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY,
  BA_IN_PROGRESS_STATUS_BE_KEY,
  BA_TIME_RANGE_FILTER_KEY,
  jiraBAReports,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  TICKET_CATEGORIZATION_SCHEME_CATEGORY_KEY,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY,
  WORKITEM_STATUS_CATEGORIES_ADO
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { isSanitizedArray, sanitizeObject } from "utils/commonUtils";
import {
  ISSUE_MANAGEMENT_REPORTS,
  SPRINT,
  COMPARE_X_AXIS_TIMESTAMP,
  LABEL_TO_TIMESTAMP,
  JIRA_MANAGEMENT_TICKET_REPORT,
  HYGIENE_TREND_REPORT
} from "dashboard/constants/applications/names";
import { DateFormats, isValidDateHandler } from "../../../utils/dateUtils";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import { IntervalType, jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { azureBAReportTypes } from "dashboard/constants/enums/azure-ba-reports.enum";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const mergeCustomHygieneFilters = (firstFilters: any = {}, secondFilters: any = {}, thirdFilters: any = {}) => {
  const newFilters = {
    ...firstFilters,
    filter: {
      ...(firstFilters.filter || {}),
      ...(secondFilters.filter || {}),
      missing_fields: {
        ...get(firstFilters, ["missing_fields"], {}),
        ...get(secondFilters, ["missing_fields"], {})
      },
      exclude: {
        ...get(firstFilters, ["filter", "exclude"], {}),
        ...get(secondFilters, ["filter", "exclude"], {}),
        custom_fields: {
          ...get(firstFilters, ["filter", "exclude", "custom_fields"], {}),
          ...get(secondFilters, ["filter", "exclude", "custom_fields"], {}),
          ...get(thirdFilters, ["filter", "exclude", "custom_fields"], {})
        }
      },
      custom_fields: {
        ...get(firstFilters, ["filter", "custom_fields"], {}),
        ...get(secondFilters, ["filter", "custom_fields"], {}),
        ...get(thirdFilters, ["filter", "custom_fields"], {})
      },
      hygiene_types: []
    }
  };

  return newFilters;
};

export const azureMergeCustomHygieneFilters = (
  firstFilters: any = {},
  secondFilters: any = {},
  thirdFilters: any = {}
) => {
  const newFilters = {
    ...firstFilters,
    filter: {
      ...(firstFilters.filter || {}),
      ...(secondFilters.filter || {}),
      missing_fields: {
        ...get(firstFilters, ["missing_fields"], {}),
        ...get(secondFilters, ["missing_fields"], {})
      },
      exclude: {
        ...get(firstFilters, ["filter", "exclude"], {}),
        ...get(secondFilters, ["filter", "exclude"], {}),
        workitem_custom_fields: {
          ...get(firstFilters, ["filter", "exclude", "workitem_custom_fields"], {}),
          ...get(secondFilters, ["filter", "exclude", "workitem_custom_fields"], {}),
          ...get(thirdFilters, ["filter", "exclude", "workitem_custom_fields"], {})
        }
      },
      workitem_custom_fields: {
        ...get(firstFilters, ["filter", "workitem_custom_fields"], {}),
        ...get(secondFilters, ["filter", "workitem_custom_fields"], {}),
        ...get(thirdFilters, ["filter", "workitem_custom_fields"], {})
      }
    }
  };

  return newFilters;
};

export const jiraHygieneDrilldownTranformer = (data: any) => {
  const { drillDownProps, widget, dashboardQuery } = data;
  const { x_axis } = drillDownProps;
  let { across, ...remainData } = widget.query;
  across = "hygiene_type";
  const widgetFilter = get(widgetConstants, [widget.type, "filters"], {});
  const hiddenFilters = get(widgetConstants, [widget.type, "hidden_filters"], {});

  const initialFilters = combineAllFilters(remainData, widgetFilter, hiddenFilters);

  let filters = {
    filter: {
      ...(initialFilters || {}),
      ...(dashboardQuery || {})
    },
    across
  };

  if (typeof x_axis === "string") {
    let filterValue = get(valuesToFilters, [across], across);
    let acrossFilterValue: any = [x_axis && x_axis.includes("UNASSIGNED") ? "_UNASSIGNED_" : x_axis] || [];
    let custom_fields = (dashboardQuery || {}).custom_fields || {};
    if (across.includes("customfield_")) {
      custom_fields[across] = [x_axis];
    }
    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        custom_fields: {
          ...get(remainData, "custom_fields", {}),
          ...custom_fields
        },
        [filterValue]: acrossFilterValue
      }
    };

    if (
      [ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT, ISSUE_MANAGEMENT_REPORTS.HYGIENE_REPORT_TREND].includes(widget?.type)
    ) {
      filters = {
        ...filters,
        filter: {
          ...(filters.filter || {}),
          [filterValue]:
            acrossFilterValue[0] === "_UNASSIGNED_" ? acrossFilterValue : [(acrossFilterValue[0] || "").toLowerCase()]
        }
      };
    }
  }

  if (typeof x_axis === "object") {
    filters = mergeCustomHygieneFilters(x_axis, filters, { filter: remainData });
  }

  return { acrossValue: across, filters };
};

export const azureHygieneDrilldownTranformer = (data: any) => {
  const { drillDownProps, widget, dashboardQuery } = data;
  const { x_axis } = drillDownProps;
  let { across, ...remainData } = widget.query;
  across = "workitem_hygiene_types";
  const widgetFilter = get(widgetConstants, [widget.type, "filters"], {});
  const hiddenFilters = get(widgetConstants, [widget.type, "hidden_filters"], {});

  const initialFilters = combineAllFilters(remainData, widgetFilter, hiddenFilters);

  let filters = {
    filter: {
      ...(initialFilters || {}),
      ...(dashboardQuery || {})
    },
    across
  };

  if (typeof x_axis === "string") {
    let filterValue = get(valuesToFilters, [across], across);
    let acrossFilterValue: any = [x_axis && x_axis.includes("UNASSIGNED") ? "_UNASSIGNED_" : x_axis] || [];
    let custom_fields = (dashboardQuery || {}).custom_fields || {};
    if (across.includes("customfield_")) {
      custom_fields[across] = [x_axis];
    }
    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        custom_fields: {
          ...get(remainData, "custom_fields", {}),
          ...custom_fields
        },
        [filterValue]:
          acrossFilterValue[0] === "_UNASSIGNED_" ? acrossFilterValue : [(acrossFilterValue[0] || "").toLowerCase()]
      }
    };
  }

  if (typeof x_axis === "object") {
    filters = azureMergeCustomHygieneFilters(x_axis, filters, { filter: remainData });
  }

  return { acrossValue: across, filters };
};

export const jiraDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  const { dashboardQuery, drillDownProps, widget } = data;
  const { across, ...remainData } = widget.query;
  const { x_axis } = drillDownProps;
  let no_update_time_field = undefined;
  let no_update_dashboard_time = undefined;
  if (typeof x_axis === "string") {
    let custom_fields = (dashboardQuery || {}).custom_fields || {};
    if (acrossValue?.includes("customfield_") || acrossValue?.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
      custom_fields[acrossValue] = [x_axis];
    }
    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        custom_fields: {
          ...get(remainData, "custom_fields", {}),
          ...custom_fields
        },
        no_update_time_field,
        no_update_dashboard_time
      }
    };
  }
  if (HYGIENE_TREND_REPORT.includes(widget.type as any)) {
    if (
      Object.keys(drillDownProps?.additionFilter || {}).length === 1 &&
      drillDownProps?.additionFilter?.hygiene_types
    ) {
      const filterKey =
        drillDownProps.application === IntegrationTypes.AZURE ? "workitem_hygiene_types" : "hygiene_types";
      filters = {
        ...filters,
        filter: {
          ...filters?.filter,
          [filterKey]: get(drillDownProps, ["additionFilter", "hygiene_types"], [])
        }
      };
    } else {
      filters = mergeCustomHygieneFilters(filters, drillDownProps?.additionFilter || {}, { filter: remainData });
    }
  }
  forEach(Object.keys(filters?.filter || {}), key => {
    const value = get(filters || {}, ["filter", key]);
    if (!!value && Array.isArray(value) && !isSanitizedArray(value)) {
      unset(filters, ["filter", key]);
    }
  });

  // generic changes to jiraBAReports filters
  if (jiraBAReports.includes(widget?.type) && !["jira_effort_investment_engineer_report"].includes(widget?.type)) {
    unset(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY]);
    unset(filters, ["filter", "completed_at"]);
  }

  if (acrossValue === "trend") {
    const labelToTimestamp = get(widgetConstants, [widget?.type, LABEL_TO_TIMESTAMP], true);
    let xaxisTimestamp = labelToTimestamp ? moment.utc(x_axis, DateFormats.DAY).unix() : parseInt(x_axis);
    const compareXAxisTimestamp = get(widgetConstants, [widget?.type, COMPARE_X_AXIS_TIMESTAMP], false);
    const currentTimeStamp = moment().unix();
    if (compareXAxisTimestamp && currentTimeStamp < xaxisTimestamp) {
      xaxisTimestamp = currentTimeStamp;
    }
    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        ingested_at: xaxisTimestamp
      }
    };

    delete filters.filter.across;
  }

  if (data.drillDownProps.application === IntegrationTypes.AZURE && "sprint" in filters.filter) {
    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        workitem_sprint_full_names: filters.filter["sprint"]
      }
    };
    unset(filters, ["filter", "sprint"]);
  }

  return { acrossValue, filters };
};

export const azureDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = jiraDrilldownTransformer(data);
  const { drillDownProps, widget } = data;
  if (acrossValue === "code_area") {
    filters = {
      ...filters,
      filter: {
        ...filters.filter,
        code_area: [drillDownProps?.["x_axis"]]
      }
    };
  }
  if (acrossValue === "parent_workitem_id" && widget?.type === ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT) {
    unset(filters, ["filter", "workitem_parent_workitem_types"]);
  }
  return { acrossValue, filters };
};

export const jiraBurndownDrilldownTransformer = (data: any) => {
  const { acrossValue, filters } = jiraDrilldownTransformer(data);
  const keysToUnset = [BA_TIME_RANGE_FILTER_KEY];
  forEach(keysToUnset, key => {
    unset(filters, ["filter", key]);
  });
  return { acrossValue, filters };
};

export const sprintMetricTrendDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  const { drillDownProps } = data;
  const keysToUnset = ["metric", "week", "month", "bi_week", "sprint"];
  forEach(keysToUnset, key => {
    unset(filters, ["filter", key]);
  });

  if (acrossValue === SPRINT) {
    const x_axis = get(drillDownProps, ["x_axis"], undefined);
    if (x_axis) {
      filters = {
        ...filters,
        filter: {
          ...(filters?.filter || {}),
          sprint_report: [x_axis],
          include_issue_keys: true,
          include_workitem_ids: true
        }
      };
    }
  } else {
    filters = {
      ...filters,
      filter: {
        ...filters?.filter,
        include_total_count: true
      }
    };
  }

  return { acrossValue, filters };
};

export const epicPriorityDrilldownTransformer = (data: any) => {
  const { acrossValue, filters } = jiraDrilldownTransformer(data);
  const keysToUnset = ["across", "interval", "sort", BA_TIME_RANGE_FILTER_KEY];
  forEach(keysToUnset, key => {
    unset(filters, ["filter", key]);
  });
  return { acrossValue, filters };
};

export const effortInvestmentTrendReportDrilldownTransformer = (data: any) => {
  let { drillDownProps } = data;
  let { x_axis, application } = drillDownProps;
  let { start_date, dataKeyClicked } = x_axis;

  data = {
    ...(data ?? {}),
    drillDownProps
  };

  let { acrossValue, filters } = jiraDrilldownTransformer(data);
  const { dashboardMetadata } = data;

  const keysToUnset = [
    "across",
    BA_TIME_RANGE_FILTER_KEY,
    ACTIVE_WORK_UNIT_FILTER_KEY,
    "committed_at",
    "interval",
    BA_COMPLETED_WORK_STATUS_BE_KEY,
    BA_IN_PROGRESS_STATUS_BE_KEY
  ];

  /** Handling status categories filter for Jira and ADO */
  if (application === IntegrationTypes.JIRA) {
    const statuses: string[] = get(filters, ["filter", BA_COMPLETED_WORK_STATUS_BE_KEY], []);
    if (statuses.length) {
      filters = {
        ...(filters || {}),
        filter: sanitizeObject({
          ...get(filters, ["filter"], {}),
          statuses
        })
      };
    } else {
      filters = {
        ...(filters || {}),
        filter: sanitizeObject({
          ...get(filters, ["filter"], {}),
          status_categories: ["Done"]
        })
      };
    }
  } else if (application === IntegrationTypes.AZURE) {
    filters = {
      ...(filters || {}),
      filter: {
        ...get(filters, ["filter"], {}),
        workitem_status_categories: WORKITEM_STATUS_CATEGORIES_ADO,
        workitem_ticket_categorization_scheme: get(filters, ["filter", TICKET_CATEGORIZATION_SCHEMES_KEY], "")
      }
    };
    keysToUnset.push(...[TICKET_CATEGORIZATION_SCHEMES_KEY, TICKET_CATEGORIZATION_SCHEME_CATEGORY_KEY]);
  }

  // ---------------------------------------------------------------------------------------------------

  /** Handling drilldown click */
  const interval = get(filters, ["filter", "interval"], "");
  const now = moment().utc().endOf("d").unix();

  let $lt: number = -1,
    $gt: number = -1,
    filterKey = acrossValue;

  if (typeof start_date === "string") {
    switch (interval) {
      case IntervalType.MONTH:
        $gt = moment.utc(start_date, DateFormats.MONTH).unix();
        $lt = Math.min(moment.unix($gt).utc().endOf("M").unix(), now);
        filters = {
          ...(filters || {}),
          filter: {
            ...get(filters, ["filter"], {}),
            [filterKey]: { $gt: $gt.toString(), $lt: $lt.toString() }
          }
        };
        break;
      case IntervalType.WEEK:
        $gt = moment.utc(start_date, DateFormats.DAY).unix();
        $lt = Math.min(moment.unix($gt).utc().endOf("W").unix(), now);
        filters = {
          ...(filters || {}),
          filter: {
            ...get(filters, ["filter"], {}),
            [filterKey]: { $gt: $gt.toString(), $lt: $lt.toString() }
          }
        };
        break;
      case IntervalType.BI_WEEK:
        $gt = moment.utc(start_date, DateFormats.DAY).unix();
        $lt = Math.min(moment.unix($gt).utc().add(2, "weeks").unix(), now);
        filters = {
          ...(filters || {}),
          filter: {
            ...get(filters, ["filter"], {}),
            [filterKey]: { $gt: $gt.toString(), $lt: $lt.toString() }
          }
        };
        break;
      case IntervalType.QUARTER:
        $gt = moment.utc(start_date, DateFormats.DAY).unix();
        $lt = Math.min(moment.unix($gt).utc().endOf("Q").unix(), now);
        filters = {
          ...(filters || {}),
          filter: {
            ...get(filters, ["filter"], {}),
            [filterKey]: { $gt: $gt.toString(), $lt: $lt.toString() }
          }
        };
        break;
    }
  }

  // ---------------------------------------------------------------------------------------------------

  /**  handling effort investment profile filter from dashboard header */
  const effort_investment_profile = get(dashboardMetadata, ["effort_investment_profile"], false);

  if (effort_investment_profile) {
    const effort_investment_profile_filter = get(dashboardMetadata, ["effort_investment_profile_filter"]);
    filters = {
      ...filters,
      filter: { ...filters.filter, ticket_categorization_scheme: effort_investment_profile_filter }
    };
  }

  // ---------------------------------------------------------------------------------------------------

  /** unsetting keys that are not required in drilldown payload */
  forEach(keysToUnset, key => {
    unset(filters, ["filter", key]);
  });

  set(filters, ["filter", "no_update_time_field"], true);
  set(filters, ["filter", "no_update_dashboard_time"], true);

  // Setting the category filter based on the one that the user clicked on
  if (dataKeyClicked) {
    filters = {
      ...filters,
      filter: {
        ...filters.filter,
        [application === IntegrationTypes.AZURE ? "workitem_ticket_categories" : "ticket_categories"]: [dataKeyClicked]
      }
    };
  }

  return { acrossValue, filters };
};

export const jiraBacklogDrillDownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  const { dashboardQuery, drillDownProps, widget } = data;
  const { across, ...remainData } = widget.query;
  const { x_axis } = drillDownProps;
  let custom_fields = (dashboardQuery || {}).custom_fields || {};
  if (acrossValue?.includes("customfield_")) {
    custom_fields[acrossValue] = [x_axis];
  }
  let xaxisTimestamp = x_axis;
  const compareXAxisTimestamp = get(widgetConstants, [widget?.type, COMPARE_X_AXIS_TIMESTAMP], false);
  const currentTimeStamp = moment().unix();
  if (compareXAxisTimestamp && currentTimeStamp < xaxisTimestamp) {
    xaxisTimestamp = currentTimeStamp;
  }
  filters = {
    ...filters,
    filter: {
      ...(filters.filter || {}),
      custom_fields: {
        ...get(remainData, "custom_fields", {}),
        ...custom_fields
      },
      ingested_at: parseInt(xaxisTimestamp)
    }
  };
  return { acrossValue, filters };
};
const acrossSupported = ["github_prs_stat", "github_issues_stat"];
const supportIssueUpdatedAt = ["jira_stat", "github_prs_stat", "github_commits_stat", "github_issues_stat"];
const supportStartTime = ["jenkins_github_stat", "jenkins_github_job_runs_stat"];
const supportChangeTime = ["jenkins_job_config_stat"];

export const statDrilldownTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  const { application } = data.drillDownProps;

  if (!acrossSupported.includes(application)) {
    delete filters.filter?.across;
    delete filters.across;
  } else {
    delete filters.filter[acrossValue];
  }

  let updatedTimeFilters = {};

  if (supportIssueUpdatedAt.includes(application)) {
    const issue_updated_at = getUpdatedFilterValue(filters.filter);
    updatedTimeFilters = { issue_updated_at };
  }

  if (supportStartTime.includes(application)) {
    const job_started_at = getUpdatedFilterValue(filters.filter, "job_started_at");
    updatedTimeFilters = { ...updatedTimeFilters, job_started_at };
  }

  if (supportChangeTime.includes(application)) {
    const job_config_changed_at = getUpdatedFilterValue(filters.filter, "job_config_changed_at");
    updatedTimeFilters = { ...updatedTimeFilters, job_config_changed_at };
  }

  filters = {
    ...filters,
    filter: {
      ...(filters.filter || {}),
      ...updatedTimeFilters
    }
  };

  return { acrossValue: filters.across, filters };
};

const getUpdatedFilterValue = (filter: any, key: string = "issue_updated_at") => {
  const { time_period } = filter;
  const now = moment().unix();
  const prev = now - time_period * 86400;

  if (Object.keys(filter[key] || {}).length) {
    const _filter = filter[key];
    return {
      $lt: (parseInt(_filter["$lt"] || "") < now ? _filter["$lt"] : now).toString(),
      $gt: (parseInt(_filter["$gt"] || "") > prev ? _filter["$gt"] : prev).toString()
    };
  }

  return {
    $lt: now.toString(),
    $gt: prev.toString()
  };
};

export const jiraBounceReportDrillDownTransformer = (data: any) => {
  const { drillDownProps } = data;
  const { x_axis } = drillDownProps;
  const { acrossValue, filters } = jiraDrilldownTransformer(data);
  let newFilters = { ...filters };
  if (["issue_updated", "issue_created"].includes(acrossValue)) {
    const gt = moment.utc(x_axis, DateFormats.DAY).startOf("day").unix();
    const lt = moment.utc(x_axis, DateFormats.DAY).endOf("day").unix();
    newFilters = {
      ...newFilters,
      filter: {
        ...(newFilters.filter || {}),
        [`${acrossValue}_at`]: {
          $gt: gt.toString(),
          $lt: lt.toString()
        },
        no_update_time_field: true
      }
    };
  }

  return { acrossValue, filters: newFilters };
};

export const azureBacklogTrendReportTransformer = (data: any) => {
  let { acrossValue, filters } = genericDrilldownTransformer(data);
  const { dashboardQuery, drillDownProps, widget } = data;
  const { across, ...remainData } = widget.query;
  const { x_axis, supportedCustomFields } = drillDownProps;
  let no_update_time_field = undefined;
  let no_update_dashboard_time = undefined;
  if (typeof x_axis === "string") {
    let custom_fields = (dashboardQuery || {}).custom_fields || {};
    if (acrossValue?.includes("customfield_") || acrossValue?.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
      const CustomFieldType = supportedCustomFields?.find((item: any) => across === item?.field_key)?.field_type;
      const isValidDate = isValidDateHandler(x_axis);
      custom_fields[acrossValue] = [x_axis];
      if ((CustomTimeBasedTypes.includes(CustomFieldType) && isValidDate) || isValidDate) {
        const timeStamp = moment(x_axis, "DD-MM-YYYY HH:mm:ss").unix().toString();
        custom_fields[acrossValue] = { $gt: timeStamp, $lt: timeStamp };
        no_update_time_field = true;
        no_update_dashboard_time = true;
      }
    }
    filters = {
      ...filters,
      filter: {
        ...(filters.filter || {}),
        custom_fields: {
          ...get(remainData, "custom_fields", {}),
          ...custom_fields
        },
        no_update_time_field,
        no_update_dashboard_time
      }
    };
  }
  forEach(Object.keys(filters?.filter || {}), key => {
    const value = get(filters || {}, ["filter", key]);
    if (!!value && Array.isArray(value) && !isSanitizedArray(value)) {
      unset(filters, ["filter", key]);
    }
  });

  let xaxisTimestamp = x_axis;
  const compareXAxisTimestamp = get(widgetConstants, [widget?.type, COMPARE_X_AXIS_TIMESTAMP], false);
  const currentTimeStamp = moment().unix();
  if (compareXAxisTimestamp && currentTimeStamp < xaxisTimestamp) {
    xaxisTimestamp = currentTimeStamp;
  }
  filters = {
    ...filters,
    filter: {
      ...(filters.filter || {}),
      ingested_at: xaxisTimestamp
    }
  };

  return { acrossValue, filters: filters };
};

export const effortInvestmentEngineerReportDrilldownTransformer = (data: any) => {
  let { drillDownProps } = data;
  const application = get(drillDownProps, "application", "jira");
  const additionalData = get(drillDownProps, application, {});
  const x_axis = get(drillDownProps, "x_axis", undefined);
  const currentAllocation = get(additionalData, ["additional_data", "current_allocation"], false);
  let filterKey = "assignee_display_names";
  let { acrossValue, filters } = jiraDrilldownTransformer(data);
  const uriUnit = get(filters, ["filter", "uri_unit"], undefined);
  const { dashboardMetadata } = data;
  const keysToUnset: any = [
    "across",
    BA_TIME_RANGE_FILTER_KEY,
    ACTIVE_WORK_UNIT_FILTER_KEY,
    "interval",
    "completed_at",
    BA_COMPLETED_WORK_STATUS_BE_KEY,
    BA_IN_PROGRESS_STATUS_BE_KEY,
    BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY,
    TICKET_CATEGORIZATION_UNIT_FILTER_KEY
  ];
  const baAttributionMode = get(filters, ["filter", "ba_attribution_mode"], undefined);
  if (uriUnit !== "commit_count_fte") {
    keysToUnset.push(get(valuesToFilters, acrossValue, acrossValue), "committed_at");
    if (baAttributionMode === "current_and_previous_assignees") {
      filterKey = "historical_assignees";
    }
  } else {
    keysToUnset.push(get(valuesToFilters, acrossValue, acrossValue), "issue_resolved_at", BA_EFFORT_ATTRIBUTION_BE_KEY);
    filterKey = "author";
    if (!currentAllocation) {
      acrossValue = "author";
    }
  }

  filters = {
    ...filters,
    filter: {
      ...filters.filter,
      [filterKey]: [x_axis]
    }
  };

  if (currentAllocation) {
    keysToUnset.push(...[BA_EFFORT_ATTRIBUTION_BE_KEY]);
  }
  if (!currentAllocation) {
    filters = {
      ...(filters || {}),
      filter: sanitizeObject({
        ...get(filters, ["filter"], {}),
        status_categories: ["Done"]
      })
    };
  }

  if (currentAllocation && filters.filter.hasOwnProperty(BA_IN_PROGRESS_STATUS_BE_KEY)) {
    filters = {
      ...(filters || {}),
      filter: sanitizeObject({
        ...get(filters, ["filter"], {}),
        statuses: [
          ...get(filters, ["filter", BA_COMPLETED_WORK_STATUS_BE_KEY], []),
          ...get(filters, ["filter", BA_IN_PROGRESS_STATUS_BE_KEY], [])
        ]
      })
    };
  }

  if (filters.filter.hasOwnProperty(BA_COMPLETED_WORK_STATUS_BE_KEY)) {
    filters = {
      ...(filters || {}),
      filter: sanitizeObject({
        ...get(filters, ["filter"], {}),
        statuses: get(filters, ["filter", BA_COMPLETED_WORK_STATUS_BE_KEY], [])
      })
    };
    keysToUnset.push("status_categories");
  }

  /**  handling effort investment profile filter from dashboard header */
  const effort_investment_profile = get(dashboardMetadata, ["effort_investment_profile"], false);

  if (effort_investment_profile) {
    const effort_investment_profile_filter = get(dashboardMetadata, ["effort_investment_profile_filter"]);
    filters = {
      ...filters,
      filter: { ...filters.filter, ticket_categorization_scheme: effort_investment_profile_filter }
    };
  }

  forEach(keysToUnset, key => {
    unset(filters, ["filter", key]);
  });

  return { acrossValue, filters };
};
