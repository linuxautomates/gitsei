import { IntegrationTypes } from "constants/IntegrationTypes";
import {
  ISSUE_MANAGEMENT_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import widgetConstants from "dashboard/constants/widgetConstants";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { get, unset } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { toTitleCase } from "utils/stringUtils";

// TEMPORARY
export const getListOfReportsWithFilterConfig = () => {
  const list = Object.keys(widgetConstants)
    .filter((key: string) => !!(widgetConstants as any)[key][REPORT_FILTERS_CONFIG])
    .map((key: string) => `(${(widgetConstants as any)[key]?.application}) ${(widgetConstants as any)[key]?.name}`);

  return {
    report_with_configs: list,
    total_report_with_configs: list.length,
    total_reports: Object.keys(widgetConstants).length
  };
};

/** Visualization options for Issue Report */
export const getVisualizationOptions = (args: any) => {
  const { allFilters } = args;
  if (!allFilters?.stacks?.length || allFilters?.stacks[0] === undefined) {
    return [
      { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
      { label: "Donut Chart", value: IssueVisualizationTypes.DONUT_CHART },
      { label: "Line Chart", value: IssueVisualizationTypes.LINE_CHART }
    ];
  }
  return [
    { label: "Bar Chart", value: IssueVisualizationTypes.BAR_CHART },
    { label: "Donut Chart", value: IssueVisualizationTypes.DONUT_CHART },
    { label: "Percentage Stacked Bar Chart", value: IssueVisualizationTypes.PERCENTAGE_STACKED_BAR_CHART },
    { label: "Multi-Line Chart", value: IssueVisualizationTypes.LINE_CHART }
  ];
};

export const getContextFilters = (props: any) => {
  const { finalFilters, contextFilter } = props;
  return { ...finalFilters, filter: { ...finalFilters.filter, ...contextFilter } };
};

export const jiraADOPrevQueryTransformer = (widget: any) => {
  const { query } = widget;
  let hygieneFiltersKey = "hygiene_types";
  const application = getWidgetConstant(widget?.type, "application");
  if (application === IntegrationTypes.AZURE) {
    hygieneFiltersKey = "workitem_hygiene_types";
  }
  const hygieneFilter = get(query, [hygieneFiltersKey]);
  if (
    ![
      JIRA_MANAGEMENT_TICKET_REPORT.TICKETS_REPORT,
      ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT,
      JIRA_MANAGEMENT_TICKET_REPORT.TICKET_ISSUE_SINGLE_STAT,
      ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT
    ].includes(widget?.type) &&
    hygieneFilter
  ) {
    unset(query, [hygieneFiltersKey]);
    unset(query, ["exclude", hygieneFiltersKey]);
  }
  return widget;
};

/** This function add the property hideFilter to the filter config which hides from the filter selection*/
export const filterSpecificFieldsFromFilterConfig = (filters: LevelOpsFilter[], filtersToHide: String[]) => {
  return (filters ?? []).map((filter: LevelOpsFilter) => {
    if (filtersToHide.includes(filter?.beKey)) {
      return {
        ...(filter ?? {}),
        hideFilter: true
      };
    }
    return filter;
  });
};

export const legendLabelTransform = (val: string, legendProps: any) => {
  return legendProps.unit === "Points"
    ? toTitleCase(val?.replace("_", " "))
    : toTitleCase(val?.replace("_", " ")).replace("Points", "Tickets");
};
