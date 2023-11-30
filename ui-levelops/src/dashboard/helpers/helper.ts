import { cloneDeep, forEach, get, parseInt, set, uniq, unset } from "lodash";
import { v1 as uuid } from "uuid";
import { sanitizeObject } from "utils/commonUtils";
import widgetConstants from "dashboard/constants/widgetConstants";
import { ALLOWED_WIDGET_DATA_SORTING, coverityDefectFiltersMapping } from "dashboard/constants/filter-name.mapping";
import { widgetDataSortingOptionsNodeType } from "dashboard/constants/WidgetDataSortingFilter.constant";
import { allTimeFilterKeys, getValueFromTimeRange, rangeMap } from "dashboard/graph-filters/components/helper";
import { JiraReports } from "dashboard/constants/enums/jira-reports.enum";
import { DEPRECATED_NOT_ALLOWED, ISSUE_MANAGEMENT_REPORTS } from "dashboard/constants/applications/names";
import { JIRA_SCM_COMMON_PARTIAL_FILTER_KEY, RANGE_FILTER_CHOICE } from "../constants/filter-key.mapping";
import { RelativeTimeRangePayload } from "../../model/time/time-range";
import { RelativeTimeRangeUnits } from "../../shared-resources/components/relative-time-range/constants";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { filterValueChangeKey } from "./drilldown-transformers/helper";
import { azureCommonPrevQueryTansformer } from "./previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { RestWidget } from "classes/RestDashboards";

export enum WidgetType {
  STATS = "stats",
  CONFIGURE_WIDGET_STATS = "configurewidgetstats",
  GRAPH = "graph",
  COMPOSITE_GRAPH = "compositegraph",
  CONFIGURE_WIDGET = "configurewidget",
  STATS_NOTES = "stats_notes",
  GRAPH_NOTES = "graph_notes"
}

export enum ChartContainerType {
  WIDGET_API_WRAPPER = "WidgetApiWrapper",
  HYGIENE_API_WRAPPER = "HygieneAPIWrapper",
  SANKEY_API_WRAPPER = "SankeyAPIWrapper",
  PRODUCTS_AGGS_API_WRAPPER = "ProductsAggsAPIWrapper",
  CONFIG_TABLE_API_WRAPPER = "ConfigTableAPIWrapper",
  SPRINT_API_WRAPPER = "SprintApiWrapper",
  BA_WIDGET_API_WRAPPER = "BAWidgetApiWrapper",
  DEV_PROD_WRAPPER = "DevProductivityAPIWrapper",
  DORA_API_WRAPPER = "DoraAPIWrapper",
  TABLE_WIDGET_API_WRAPPER = "TableWidgetApiWrapper"
}

export enum CSVDownloadSagaType {
  GENERIC_CSV_DOWNLOAD = "genericCsvDownload", // This constant is used when genericTableCSVDownload.saga is to be used
  TRIAGE_CSV_DOWNLOAD = "triageCsvDownload", // This constant is used for TriageGridView.page in order to use csvDownloadTriageGridViewEffectSaga
  DRILLDOWN_CSV_DOWNLOAD = "drilldownCsvDownload" // This constant is used for drilldown reports in order to use csvDownloadDrilldownEffectSaga
}

export const widgetTypes = [
  WidgetType.STATS,
  WidgetType.CONFIGURE_WIDGET,
  WidgetType.COMPOSITE_GRAPH,
  WidgetType.GRAPH,
  WidgetType.CONFIGURE_WIDGET_STATS,
  WidgetType.STATS_NOTES,
  WidgetType.GRAPH_NOTES
];

export const levelopsApiFiltersKeys: Array<string> = [
  "tags",
  "tag_ids",
  "questionnaire_template_id",
  "questionnaire_template_ids",
  "product_id",
  "product_ids",
  "assignees",
  "assignee_user_ids"
];

export const cloneWidgets = (widgets: any) => {
  let updatedWidgets: any[] = [];
  let excludeIds: any[] = [];
  let compositeWidgets = widgets.filter((widget: any) => {
    const widgetType = get(widget, ["metadata", "widget_type"], undefined);
    return !!(widgetType && widgetType === WidgetType.COMPOSITE_GRAPH);
  });

  if (compositeWidgets.length > 0) {
    let newChildIds: { [x: string]: Array<any> } = {};
    compositeWidgets.forEach((widget: any) => {
      excludeIds.push(widget.id);
      const newCompositeId = uuid();
      const children = get(widget, ["metadata", "children"], []);
      if (children.length > 0) {
        excludeIds.push(...children);
        children.forEach((childId: any) => {
          const childWidget = widgets.find((widget: any) => widget.id === childId);
          if (childWidget) {
            const newChildId = uuid();
            updatedWidgets.push({ ...childWidget, id: newChildId });
            newChildIds = {
              ...newChildIds,
              [newCompositeId]: [...(newChildIds[newCompositeId] || []), newChildId]
            };
          }
        });
      }
      updatedWidgets.push({
        ...widget,
        id: newCompositeId,
        metadata: {
          ...(widget["metadata"] || {}),
          children: newChildIds[newCompositeId] || []
        }
      });
    });
  }

  let graphOnly = widgets.filter((widget: any) => !excludeIds.includes(widget.id));

  if (graphOnly.length > 0) {
    graphOnly.forEach((widget: any) => {
      updatedWidgets.push({ ...widget, id: uuid() });
    });
  }

  return updatedWidgets;
};

export const mapFiltersToIds = (filters: any) => {
  let mappedFilters: any = {};
  const validFilters = sanitizeObject(filters);
  Object.keys(validFilters).forEach(key => {
    if (key === "questionnaire_template_id" || key === "questionnaire_template_ids") {
      mappedFilters = {
        ...mappedFilters,
        questionnaire_template_ids: uniq([
          ...(validFilters[key] || []),
          ...(mappedFilters["questionnaire_template_ids"] || [])
        ])
      };
    }
    if (key === "tags" || key === "tag_ids") {
      mappedFilters = {
        ...mappedFilters,
        tag_ids: uniq([...(validFilters[key] || []), ...(mappedFilters["tag_ids"] || [])])
      };
    }
    if (key === "product_ids") {
      mappedFilters = {
        ...mappedFilters,
        product_ids: uniq([...(validFilters[key] || []), ...(mappedFilters["product_ids"] || [])])
      };
    }

    if (key === "product_id") {
      mappedFilters = {
        ...mappedFilters,
        product_ids: uniq([validFilters[key] || "", ...(mappedFilters["product_ids"] || [])])
      };
    }
    if (key === "assignees" || key === "assignee_user_ids") {
      mappedFilters = {
        ...mappedFilters,
        user_ids: uniq([...(validFilters[key] || []), ...(mappedFilters["user_ids"] || [])])
      };
    }
  });

  return mappedFilters;
};

export const mapNonApiFilters = (filters: any) => {
  let mappedFilters: any = {};
  Object.keys(filters).forEach(key => {
    if (!levelopsApiFiltersKeys.includes(key)) {
      mappedFilters = {
        ...mappedFilters,
        [key]: filters[key]
      };
    }
    if (key === "reporters") {
      mappedFilters = {
        ...mappedFilters,
        reporter: filters[key]?.[0]?.label
      };
    }

    if (["completed", "submitted"].includes(key)) {
      mappedFilters = {
        ...mappedFilters,
        [key]: filters[key] === "true"
      };
    }
  });
  unset(mappedFilters, ["reporters"]);
  return mappedFilters;
};

export const mapUserIdsToAssigneeOrReporter = (usersData: any, filters: any) => {
  let mappedFilters: any = {};
  Object.keys(filters).forEach(key => {
    if (key === "assignees" || key === "assignee_user_ids") {
      mappedFilters = {
        ...mappedFilters,
        assignee_user_ids: uniq([...(filters[key] || []), ...(mappedFilters["assignee_user_ids"] || [])])
          .map(userId => {
            const userData = usersData.find((user: { id: any }) => user.id === userId);
            return { key: userData?.id, label: userData?.email };
          })
          .filter(user => user.key !== undefined)
      };
    }
  });
  return mappedFilters;
};

export const filtersMap: { [x: string]: string } = {
  project_name: "Project Name",
  instance_name: "Instance Name"
};

export const getTextWidth = (text: string) => {
  const canvas = document.createElement("canvas");
  const context = canvas.getContext("2d");

  // @ts-ignore
  context.font = "500 14px Inter";

  // @ts-ignore
  return Math.round(context?.measureText(text).width + 24) || 0;
};

type rootDeteminerType = "value" | "label" | "startDate" | "endDate";
export const getWidgetDataSortingSortValueNonTimeBased = (across: string, sortOption: string, valueSortKey: string) => {
  if (!sortOption) return {};
  const sortSplitted = sortOption.split("_");
  const rootDeteminer: rootDeteminerType = sortSplitted[0] as rootDeteminerType;
  const sortingOrder: string = sortSplitted[1];
  const getSortFilter = (id: string, order: "asc" | "desc") => {
    return [{ id, desc: !(order === "asc") }];
  };
  const getSortingOrder = () => {
    if (["low-high", "old-latest"].includes(sortingOrder)) return "asc";
    return "desc";
  };
  switch (rootDeteminer) {
    case "value":
      return getSortFilter(valueSortKey, getSortingOrder());
    case "startDate":
      return getSortFilter("start_date", getSortingOrder());
    case "endDate":
      return getSortFilter("end_date", getSortingOrder());
    default:
      return getSortFilter(across, getSortingOrder());
  }
};

export const widgetFilterOptionsNode = (across: string, customeDateTimeKeysFields?: Array<string>) => {
  if (across.toLowerCase() === "sprint") {
    return widgetDataSortingOptionsNodeType.SPRINT_TIME_BASED;
  }
  if (allTimeFilterKeys.concat(customeDateTimeKeysFields || []).includes(across)) {
    return widgetDataSortingOptionsNodeType.TIME_BASED;
  }
  return widgetDataSortingOptionsNodeType.NON_TIME_BASED;
};

export const isCustomSprint = (across: string, customOptions: any[]) => {
  const customOption = (customOptions || []).find(option => option.value === across);
  return customOption && (customOption.label || "").toLowerCase() === "sprint";
};

export const allowWidgetDataSorting = (reportType: string, filters: any) => {
  let across = get(filters, ["across"], "");
  const ignoreAcrossKeysForSorting = ["priority", "none"];
  let allowSorting =
    get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING]) && !ignoreAcrossKeysForSorting.includes(across);

  if (
    [JiraReports.RESOLUTION_TIME_REPORT, ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT].includes(reportType as any)
  ) {
    const metric: string = filters?.filter ? get(filters, ["filter", "metric"], []) : get(filters, ["metric"], []);
    allowSorting = allowSorting && metric;
  }
  return allowSorting;
};

// for transforming query of aleady existing coverity report
export const transformCoverityPrevReportQuery = (widget: any) => {
  const query = cloneDeep(widget?.query || {});
  let newQuery: any = {};
  let nExclude: any = {};
  let nPartialMatch: any = {};
  const exclude = get(query, ["exclude"], {});
  const partialMatch = get(query, ["partial_match"], {});
  let normalKeys = Object.keys(query).filter(key => !["exclude", "partial_match"].includes(key));
  let excludeKeys = Object.keys(exclude);
  let partialMatchKeys = Object.keys(partialMatch);
  forEach(normalKeys, key => {
    const filterKey = get(coverityDefectFiltersMapping, [key], key);
    newQuery[filterKey] = get(query, [key]);
  });
  forEach(excludeKeys, key => {
    const filterKey = get(coverityDefectFiltersMapping, [key], key);
    nExclude[filterKey] = get(query, ["exclude", key]);
  });
  forEach(partialMatchKeys, key => {
    const filterKey = get(coverityDefectFiltersMapping, [key], key);
    nPartialMatch[filterKey] = get(query, ["partial_match", key]);
  });
  if (Object.keys(nExclude).length) {
    newQuery["exclude"] = nExclude;
  }
  if (Object.keys(nPartialMatch).length) {
    newQuery["partial_match"] = nPartialMatch;
  }

  set(widget, ["query"], newQuery);
};

export const transformCodeVolumeVsDeploymentPrevQuery = (widget: any) => {
  const mapping: any = {
    weekly: "week",
    "bi-weekly": "biweekly",
    monthly: "month"
  };
  const query = cloneDeep(widget?.query || {});
  if (query.interval) {
    const newInterval = mapping[query.interval];
    set(query, ["interval"], newInterval);
  }
  set(widget, ["query"], query);
};

const updateStatReportPrevQuery = (widget: any, filterKey: any, exclusiveFilters: string[]) => {
  const query = widget.query;
  let metaData = widget.metadata;

  if (query.hasOwnProperty("interval")) {
    const _value: RelativeTimeRangePayload = {
      next: { unit: RelativeTimeRangeUnits.TODAY },
      last: { unit: query.interval, num: 1 }
    };

    const key = get(rangeMap, filterKey, filterKey);
    metaData = {
      ...metaData,
      [RANGE_FILTER_CHOICE]: {
        ...(metaData[RANGE_FILTER_CHOICE] || {}),
        [key]: {
          type: "relative",
          relative: _value
        }
      }
    };

    delete query.interval;

    query[filterKey] = getValueFromTimeRange(_value);

    exclusiveFilters
      .filter(key => key !== filterKey)
      .forEach(key => {
        delete query[key];
        const metaKey = get(rangeMap, key, key);
        delete (metaData[RANGE_FILTER_CHOICE] || {})[metaKey];
      });

    widget.query = query;
    widget.metadata = metaData;
  }

  return widget;
};

const updateResolutionTimeStatReportPrevQuery = (widget: any, filterKey: any, exclusiveFilters: string[]) => {
  const query = widget.query;
  let metaData = widget.metadata;

  if (query.hasOwnProperty("interval")) {
    const _value: RelativeTimeRangePayload = {
      next: { unit: RelativeTimeRangeUnits.TODAY },
      last: { unit: query.interval, num: 1 }
    };

    const key = get(rangeMap, filterKey, filterKey);
    metaData = {
      ...metaData,
      [RANGE_FILTER_CHOICE]: {
        ...(metaData[RANGE_FILTER_CHOICE] || {}),
        [key]: {
          type: "relative",
          relative: _value
        }
      }
    };

    delete query.interval;

    query[filterKey] = getValueFromTimeRange(_value);
  }
  exclusiveFilters
    .filter(key => key !== filterKey)
    .forEach(key => {
      delete query[key];
      const metaKey = get(rangeMap, key, key);
      delete (metaData[RANGE_FILTER_CHOICE] || {})[metaKey];
    });

  widget.query = query;
  widget.metadata = metaData;
  return widget;
};

export const transformIssuesSingleStatReportPrevQuery = (widget: any) => {
  const query = widget.query;
  const filterKey = `${query.across || "issue_created"}_at`;
  const exclusiveFilters = ["issue_created_at", "issue_updated_at", "issue_resolved_at", "issue_due_at"];

  if (!query.across) {
    query.across = "issue_created";
  }

  widget.query = query;

  return updateStatReportPrevQuery(widget, filterKey, exclusiveFilters);
};

export const transformIssueResolutionTimeSingleStatReportPrevQuery = (widget: any) => {
  const query = widget.query;
  const filterKey = `${query.across || "issue_created"}_at`;

  const exclusiveFilters = ["issue_created_at", "issue_updated_at", "issue_resolved_at", "issue_due_at"];

  if (!query.across) {
    query.across = "issue_created";
  }

  widget.query = query;

  return updateResolutionTimeStatReportPrevQuery(widget, filterKey, exclusiveFilters);
};

export const transformAzureIssuesSingleStatReportPrevQuery = (widget: any) => {
  const query = widget.query;
  const filterKey = query.across || "workitem_created_at";
  const exclusiveFilters = ["workitem_created_at", "workitem_updated_at", "workitem_resolved_at", "workitem_due_at"];

  if (!query.across) {
    query.across = "workitem_created_at";
  }

  widget.query = query;
  widget = azureCommonPrevQueryTansformer(widget);
  return updateStatReportPrevQuery(widget, filterKey, exclusiveFilters);
};

export const transformAzureIssuesResolutionTimeSingleStatReportPrevQuery = (widget: any) => {
  const query = widget.query;
  const filterKey = `${query?.across?.replace("_at", "") || "workitem_created"}_at`;

  const exclusiveFilters = ["workitem_created_at", "workitem_resolved_at", "workitem_updated_at"];
  query.across = `${query?.across?.replace("_at", "") || "workitem_created"}_at`;
  if (!query.across) {
    query.across = "workitem_created_at";
  }
  widget.query = query;

  widget = azureCommonPrevQueryTansformer(widget);

  return updateResolutionTimeStatReportPrevQuery(widget, filterKey, exclusiveFilters);
};

export const transformLeadTimeReportPrevQuery = (widget: any) => {
  let query = cloneDeep(widget?.query || {});
  const jira_status_categories = get(query, ["jira_status_categories"], []);
  const jira_status_category = get(query, ["jira_status_category"], []);
  if (jira_status_categories.length || jira_status_category.length) {
    query.jira_status_categories = [...jira_status_categories, ...jira_status_category];
    delete query?.jira_status_category;
  }
  query = filterValueChangeKey("jira_fix_version", "jira_fix_versions", query);

  set(widget, ["query"], query);
};

export const transformLeadTimeStageReportPrevQuery = (widget: any) => {
  let query = cloneDeep(widget?.query || {});
  const jira_status_categories = get(query, ["jira_status_categories"], []);
  const jira_status_category = get(query, ["jira_status_category"], []);
  if (jira_status_categories.length || jira_status_category.length) {
    query.jira_status_categories = [...jira_status_categories, ...jira_status_category];
    delete query?.jira_status_category;
  }
  query = filterValueChangeKey("jira_fix_version", "jira_fix_versions", query);
  const ratings = get(query, "ratings", undefined);
  // setting default value for ratings
  if (!ratings) {
    query = { ...query, ratings: ["good", "slow", "needs_attention"] };
  }
  // because key has been changed to ratings
  if (query.rating) {
    delete query.rating;
  }
  set(widget, ["query"], query);
};

export const transformIssueBacklogTrendReportPrevQuery = (widget: any) => {
  const metadata = widget?.metadata || {};

  const metrics = get(metadata, "metrics", []);

  if (metrics.length) {
    metadata.leftYAxis = "total_tickets";
    metadata.rightYAxis = "median";

    metrics.forEach((metric: string) => {
      if (["mean", "median", "p90"].includes(metric)) {
        metadata.rightYAxis = metric;
      }
    });

    delete metadata.metrics;
  }
};

export const transformSCMCommitToCICDJobLeadTimeSingleStatReportPrevQuery = (widget: any) => {
  const query = widget.query;
  let metaData = widget.metadata;

  if (query.hasOwnProperty("time_period")) {
    const _value: RelativeTimeRangePayload = {
      next: { unit: RelativeTimeRangeUnits.TODAY },
      last: { unit: RelativeTimeRangeUnits.DAYS, num: query.time_period }
    };
    metaData = {
      ...metaData,
      [RANGE_FILTER_CHOICE]: {
        ...(metaData[RANGE_FILTER_CHOICE] || {}),
        start_time: {
          type: "relative",
          relative: _value
        }
      }
    };

    query.start_time = getValueFromTimeRange(_value);

    delete query.time_period;

    widget.query = query;
    widget.metadata = metaData;
  }

  return widget;
};

export const transformSprintMetricsTrendReportPrevQuery = (widget: any) => {
  const query = widget.query;
  if (query.hasOwnProperty("sprint_states")) {
    delete query.sprint_states;
    widget.query = query;
  }
  return widget;
};

export const transformIssuesReportPrevQuery = (widget: any) => {
  const { query, metadata } = widget;
  const { custom_stacks } = query;
  const { visualization } = metadata;

  if (visualization === IssueVisualizationTypes.DONUT_CHART && custom_stacks?.length > 0) {
    unset(widget, ["query", "custom_stacks"]);
  }

  const visualizationInMetaData = metadata?.visualization;

  if (visualizationInMetaData) {
    unset(widget, ["metadata", "visualization"]);
    set(widget, ["query", "visualization"], visualizationInMetaData);
  }

  widget = azureCommonPrevQueryTansformer(widget);
  return widget;
};

export const transformCICDJobReport = (widget: any) => {
  const { query } = widget;
  const { stacks } = query;

  // handling stacks: [null] filter for couple of existing widgets
  if (stacks && stacks.length > 0) {
    const filteredStacks = stacks.filter((value: any) => !!value);
    if (filteredStacks.length) {
      query["stacks"] = filteredStacks;
    } else {
      unset(query, "stacks");
    }
  }

  widget.query = query;
  return widget;
};

export const transformIssueHygienePrevQuery = (widget: any) => {
  const { query } = widget;
  if (query.hasOwnProperty("sprint_states")) {
    const value = query.sprint_states;
    delete query.sprint_states;
    query["jira_sprint_states"] = value;
    widget.query = query;
  }
  return widget;
};

export const transformSCMPrevQuery = (widget: any) => {
  const { query } = widget;
  const { across } = query;
  if (!["trend"].includes(across) && query?.interval) {
    delete query?.interval;
  }
  widget.query = query;
  return widget;
};

export const transformAzureLeadTimeStageReportPrevQuery = (widget: any) => {
  const { query } = widget;
  const ratings = get(query, "ratings", undefined);
  // setting default value for ratings
  if (!ratings) {
    const newQuery = { ...query, ratings: ["good", "slow", "needs_attention"] };
    widget.query = newQuery;
  }
  // because key has been changed to ratings
  if (widget?.query?.rating) {
    delete widget.query.rating;
  }
  return widget;
};

export const transformAzureWidgetQueryForCustomFields = (
  filters: any,
  customFieldRecords: Array<{ field_key: string; metadata: { transformed: string } }>
) => {
  let newFilters = cloneDeep(filters);
  const customFieldKey = "workitem_custom_fields";
  const customFilters = get(filters, ["filter", customFieldKey], {});
  const excludeCustomFilters = get(filters, ["filter", "exclude", customFieldKey], {});
  const partialFilters = get(filters, ["filter", JIRA_SCM_COMMON_PARTIAL_FILTER_KEY], {});
  const getReducedFilters = (cfilters: any) =>
    Object.keys(cfilters).reduce((acc: any, next: string) => {
      const config = customFieldRecords.find((item: any) => item.field_key === next || next?.includes(item.field_key));
      if (config?.hasOwnProperty("metadata") && config?.metadata?.transformed) {
        const key = next.replace("Custom.", "");
        return { ...acc, [key]: cfilters[next] };
      }
      return { ...acc, [next]: cfilters[next] };
    }, {});
  const newCustomFilters = getReducedFilters(customFilters);
  const newExcludeCustomFilters = getReducedFilters(excludeCustomFilters);
  const newPartialFilters = getReducedFilters(partialFilters);
  if (Object.keys(newCustomFilters).length) {
    set(newFilters, ["filter", customFieldKey], newCustomFilters);
  }
  if (Object.keys(newExcludeCustomFilters).length) {
    set(newFilters, ["filter", "exclude", customFieldKey], newExcludeCustomFilters);
  }
  if (Object.keys(partialFilters).length) {
    set(newFilters, ["filter", JIRA_SCM_COMMON_PARTIAL_FILTER_KEY], newPartialFilters);
  }
  return newFilters;
};

export const filteredDeprecatedWidgets = (widgets: RestWidget[]) => {
  return (widgets || []).filter(widget => {
    const isDeprecatedAndNotAllowed = get(widgetConstants, [widget?.type, DEPRECATED_NOT_ALLOWED], false);
    if (isDeprecatedAndNotAllowed) {
      return false;
    }
    return true;
  });
};

export const checkTimeSecondOrMiliSecond = (value: number) => {
  const secondsSinceEpoch = Math.round(Date.now() / 1000);
  if (value > secondsSinceEpoch) {
    // it is in milliseconds need to convert to second, moment.unix() accepts seconds only
    return Math.round(value / 1000);
  }
  return value;
};
