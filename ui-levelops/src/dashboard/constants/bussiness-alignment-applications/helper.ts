import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { set, unset, get } from "lodash";
import moment from "moment";
import widgetConstants from "../widgetConstants";
import { getMappedSelectedTimeRange } from "reduxConfigs/sagas/saga-helpers/BASprintReport.helper";
import {
  jiraEffortInvestmentTrendReportTimeRangeOptions,
  modificationMappedValues
} from "dashboard/graph-filters/components/helper";
import { getWidgetConstant } from "../widgetConstants";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  BA_EFFORT_ATTRIBUTION_BE_KEY,
  BA_TIME_RANGE_FILTER_KEY,
  EffortAttributionOptions,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY
} from "./constants";
import {
  ActiveEffortUnitType,
  EffortUnitType,
  IntervalType,
  IntervalTypeDisplay,
  jiraBAReportTypes
} from "../enums/jira-ba-reports.enum";
import { azureCommonPrevQueryTansformer } from "dashboard/helpers/previous-query-transformers/azure/azureCommonPrevQuery.transformer";
import { IntegrationTypes } from "constants/IntegrationTypes";
import {
  DEFAULT_CATEGORY_NAME,
  FILTER_FIELD_UNCATEGORIZED_NAME
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";

/** adding backward compatability to BA reports */

export const effortInvestmentTrendBComTransformer = (widget: basicMappingType<any>) => {
  /** Backward Compatibilty for interval */

  if (!widget?.query?.interval || widget?.query?.interval === "bi_week") {
    widget.query.interval = get(
      widgetConstants || {},
      [widget?.type || "", "default_query", "interval"],
      IntervalType.BI_WEEK
    );
  }

  const effortUnit = get(widget?.query ?? {}, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY]);

  /** Backward Compatibility for time range */
  const isCommitCount = [EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(effortUnit);
  const INITIAL_TIME_RANGE = {
    // required filters and default is last month
    $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  };

  let baTimeRange = get(
    widget?.query ?? {},
    [BA_TIME_RANGE_FILTER_KEY],
    modificationMappedValues("last_4_month", jiraEffortInvestmentTrendReportTimeRangeOptions)
  );

  const mappedTimeInterval = getMappedSelectedTimeRange(baTimeRange);
  switch (mappedTimeInterval) {
    case "last_4_week":
      baTimeRange = {
        $gt: moment().utc().startOf("week").subtract(3, "week").unix().toString(),
        $lt: moment().utc().endOf("d").unix().toString()
      };
      break;
    case "last_4_month":
      baTimeRange = {
        $gt: moment().utc().startOf("month").subtract(3, "month").unix().toString(),
        $lt: moment().utc().endOf("d").unix().toString()
      };
      break;
    case "last_8_week":
      baTimeRange = {
        $gt: moment().utc().startOf("week").subtract(8, "week").unix().toString(),
        $lt: moment().utc().endOf("d").unix().toString()
      };
      break;
    case "last_4_quarter":
      baTimeRange = {
        $gt: moment().utc().startOf("Q").subtract(3, "Q").unix().toString(),
        $lt: moment().utc().endOf("d").unix().toString()
      };
      break;
  }

  const application = getWidgetConstant(widget?.type, "application");

  const filterKey = isCommitCount
    ? "committed_at"
    : application === IntegrationTypes.AZURE
    ? "workitem_resolved_at"
    : "issue_resolved_at";

  const timeFilterAlreadyExits = get(widget?.query ?? {}, [filterKey], undefined);

  if (!timeFilterAlreadyExits || !Object.keys(timeFilterAlreadyExits).length) {
    set(widget?.query ?? {}, [filterKey], baTimeRange ?? INITIAL_TIME_RANGE);
  }

  unset(widget?.query ?? {}, [BA_TIME_RANGE_FILTER_KEY]);

  /** Backward Compatibilty for Active work unit */

  let activeWorkURI = get(widget?.query ?? {}, [ACTIVE_WORK_UNIT_FILTER_KEY], undefined);
  if (!activeWorkURI) {
    set(
      widget?.query ?? {},
      [ACTIVE_WORK_UNIT_FILTER_KEY],
      application === IntegrationTypes.AZURE
        ? ActiveEffortUnitType.AZURE_TICKETS_COUNT
        : ActiveEffortUnitType.JIRA_TICKETS_COUNT
    );
  }

  // Adding here for now. Later will create a seperate generic function to handle common changes.
  /** Backward Compatibility for Effort Attribution */
  const effortAttributionFilter = get(widget?.query ?? {}, [BA_EFFORT_ATTRIBUTION_BE_KEY], undefined);
  if (!effortAttributionFilter && jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT === widget?.type) {
    set(widget?.query ?? {}, [BA_EFFORT_ATTRIBUTION_BE_KEY], EffortAttributionOptions.CURRENT_ASSIGNEE);
  }

  if (application === IntegrationTypes.AZURE) {
    widget = azureCommonPrevQueryTansformer(widget as any);
  }
};

export const effortInvestmentSingleStatBComTransformer = (widget: basicMappingType<any>) => {
  const INITIAL_TIME_RANGE = {
    // required filters and default is last month
    $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  };
  const application = getWidgetConstant(widget?.type, "application");
  const effortUnit = get(widget?.query ?? {}, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY]);
  const isCommitCount = [EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(effortUnit);
  const filterKey = isCommitCount
    ? "committed_at"
    : application === IntegrationTypes.AZURE
    ? "workitem_resolved_at"
    : "issue_resolved_at";
  const baTimeRange = get(widget?.query ?? {}, [BA_TIME_RANGE_FILTER_KEY], INITIAL_TIME_RANGE);
  const issueResolvedAt = get(widget?.query ?? {}, [filterKey]);
  if (issueResolvedAt && Object.keys(issueResolvedAt).length) {
    unset(widget?.query ?? {}, [BA_TIME_RANGE_FILTER_KEY]);
  } else {
    set(widget?.query ?? {}, [filterKey], baTimeRange ?? INITIAL_TIME_RANGE);
  }
  unset(widget?.query ?? {}, ["interval"]);
  unset(widget?.query ?? {}, ["time_period"]);

  // Adding here for now. Later will create a seperate generic function to handle common changes.
  /** Backward Compatibility for Effort Attribution */
  const effortAttributionFilter = get(widget?.query ?? {}, [BA_EFFORT_ATTRIBUTION_BE_KEY], undefined);
  if (!effortAttributionFilter && jiraBAReportTypes.EFFORT_INVESTMENT_SINGLE_STAT === widget?.type) {
    set(widget?.query ?? {}, [BA_EFFORT_ATTRIBUTION_BE_KEY], EffortAttributionOptions.CURRENT_ASSIGNEE);
  }

  if (application === IntegrationTypes.AZURE) {
    widget = azureCommonPrevQueryTansformer(widget as any);
  }
};

export const effortInvestmentEngineerBComTransformer = (widget: basicMappingType<any>) => {
  let activeWorkURI = get(widget?.query ?? {}, [ACTIVE_WORK_UNIT_FILTER_KEY], undefined);
  const application = getWidgetConstant(widget?.type, "application");
  if (!activeWorkURI) {
    set(
      widget?.query ?? {},
      [ACTIVE_WORK_UNIT_FILTER_KEY],
      application === IntegrationTypes.AZURE
        ? ActiveEffortUnitType.AZURE_TICKETS_COUNT
        : ActiveEffortUnitType.JIRA_TICKETS_COUNT
    );
  }

  // Adding here for now. Later will create a seperate generic function to handle common changes.
  /** Backward Compatibility for Effort Attribution */
  const effortAttributionFilter = get(widget?.query ?? {}, [BA_EFFORT_ATTRIBUTION_BE_KEY], undefined);
  if (!effortAttributionFilter && jiraBAReportTypes.JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT === widget?.type) {
    set(widget?.query ?? {}, [BA_EFFORT_ATTRIBUTION_BE_KEY], EffortAttributionOptions.CURRENT_ASSIGNEE);
  }
};

export const effortInvestmentTrendChartOnClicked = (params: { data: any; apiData: any[]; dataKeyClicked: any }) => {
  const { data } = params;
  let dataKeyClicked = params.dataKeyClicked;

  // For the BE, "Uncategorized" is mapped as "Other"
  if (dataKeyClicked && dataKeyClicked === DEFAULT_CATEGORY_NAME) dataKeyClicked = FILTER_FIELD_UNCATEGORIZED_NAME;

  return {
    start_date: data.start_date,
    end_date: data.end_date,
    dataKeyClicked: dataKeyClicked || null,
    selectedInterval: data?.selected_interval
  };
};

export const getDrilldownTitleEffortInvestmentTrendReport = (params: { xAxis: any }) => {
  const { xAxis } = params;

  if (xAxis?.start_date && xAxis?.selectedInterval === IntervalTypeDisplay.MONTH) {
    return xAxis.start_date;
  }

  if (xAxis?.start_date && xAxis?.end_date) {
    return `${xAxis.start_date} - ${xAxis.end_date}`;
  }

  return "";
};

export const getCommaSeparatedTotalAndPercentage = (data: any) => {
  const valueArr = (data || "")?.split("|");
  return [valueArr[0] || "0", `${valueArr[1] || "0.00"}%`];
};

export const getConditionalUriForFilterPreview = (params: any) => {
  const { query, defaultUri } = params;
  const uri_unit = query?.uri_unit;
  if (!uri_unit) {
    return defaultUri;
  }
  switch (uri_unit) {
    case EffortUnitType.COMMIT_COUNT:
    case EffortUnitType.AZURE_COMMIT_COUNT:
      return "github_commits_filter_values";
    case EffortUnitType.AZURE_STORY_POINT_REPORT:
    case EffortUnitType.AZURE_TICKETS_REPORT:
    case EffortUnitType.AZURE_TICKET_TIME_SPENT:
      return "issue_management_workitem_values";
    default:
      return "jira_filter_values";
  }
};

export const mapFiltersForWidgetApiIssueProgressReport = (filter: any) => {
  // THIS CODE IS COMMENTED BECAUSE WE ARE REVERTING THE CHANGES OF CDK GLOBAL
  // let epicsFilter = {};
  // if (filter.filter?.across && filter.filter?.across === "epic" ) {
  //   epicsFilter = {
  //     "issue_types": ["EPIC"],
  //   }
  //   if(filter.filter.hasOwnProperty("epics")){
  //     epicsFilter = {
  //       ...epicsFilter,
  //       "keys": filter.filter?.epics || []
  //     }
  //     unset(filter?.filter ?? {}, ["epics"]);
  //   }
  // }
  const finalFilters = {
    ...filter,
    // filter: {
    //   ...filter.filter,
    //   ...epicsFilter,
    // },
    widget: "progress_single_report"
  };
  return finalFilters;
};

export const mapFiltersForWidgetApiIssueProgressReportDrilldown = (filter: any) => {
  // THIS CODE IS COMMENTED BECAUSE WE ARE REVERTING THE CHANGES OF CDK GLOBAL
  // if(filter?.across && filter?.across === "epic"){
  //   const finalFilters = {
  //     ...filter,
  //     filter: {
  //       ...filter.filter,
  //       "inheritance": ["EPIC_AND_CHILDREN"]
  //     }
  //   };
  //   return finalFilters;
  // }
  return filter;
};
