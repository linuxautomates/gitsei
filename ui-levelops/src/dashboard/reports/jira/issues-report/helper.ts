import moment from "moment";
import { AZURE_TIME_FILTERS_KEYS, GROUP_BY_TIME_FILTERS } from "constants/filters";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import { convertEpochToDate, DateFormats, isvalidTimeStamp } from "utils/dateUtils";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { idFilters } from "../commonJiraReports.constants";
import { cloneDeep, get, isArray, set, unset } from "lodash";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { getXAxisTimeLabel } from "utils/dateUtils";
import { EPIC_TEXT } from "./constants";
import { FilterTypes } from "constants/FilterTypes";
import { JIRA_SCM_COMMON_PARTIAL_FILTER_KEY } from "dashboard/constants/filter-key.mapping";

export const issuesReportXAxisLabelTransform = (params: any) => {
  const { interval, across, item = {}, weekDateFormat, CustomFieldType } = params;
  const { key, additional_key } = item;
  let newLabel = key;

  if (["fix_version"].includes(across)) {
    return newLabel;
  }
  const isValidDate = isvalidTimeStamp(newLabel);
  if (idFilters.includes(across)) {
    newLabel = additional_key;
    return newLabel;
  }
  if (
    (CustomTimeBasedTypes.includes(CustomFieldType) && isValidDate) ||
    (isValidDate && !GROUP_BY_TIME_FILTERS.includes(across))
  ) {
    newLabel = moment(parseInt(key)).format("DD-MM-YYYY HH:mm:ss");
    return newLabel;
  }
  if (GROUP_BY_TIME_FILTERS.includes(across)) {
    newLabel = getXAxisTimeLabel({ interval, key, options: { weekDateFormat } });
  }
  if (!newLabel) {
    newLabel = "UNRESOLVED";
  }
  return newLabel;
};

export const issuesReportGetStackFilterStatus = (filters: basicMappingType<any>) => {
  return filters && filters.visualization && filters.visualization === IssueVisualizationTypes.DONUT_CHART;
};

export const getTotalLabelIssuesReport = (data: any) => {
  const { unit } = data;
  return unit === "Story Points" ? "Total sum of story points" : "Total number of tickets";
};

export const getAcrossValue = (args: any) => {
  const filters = args?.allFilters;
  let across = filters?.across;
  if ([...GROUP_BY_TIME_FILTERS, "ticket_created", ...AZURE_TIME_FILTERS_KEYS].includes(across)) {
    const interval = get(filters, ["interval"]);
    if (interval) across = `${across}_${interval}`;
  }
  return across;
};
export const xAxisLabelTransform = (params: any) => {
  return getXAxisLabel(params);
};

export const addExtraFilters = (query: any, type: string, value: string, customEpics: any) => {
  let newQuery = cloneDeep(query);
  const findEpicLink = customEpics?.find(
    (epic: any) => epic?.key === value && epic?.name?.toLowerCase() === EPIC_TEXT.toLowerCase()
  );
  if (type === "stacks") {
    if (findEpicLink) {
      newQuery = {
        ...(newQuery ?? {}),
        fetch_epic_summary: true
      };
    } else {
      unset(newQuery, ["fetch_epic_summary"]);
    }
  }
  return newQuery;
};

export const mapFiltersBeforeCallIssueReport = (filter: any) => {
  let dependencyAnalysisFilter = get(filter, ["filter", "links"], []).length;
  if (dependencyAnalysisFilter) {
    unset(filter, ["ou_exclusions"]);
  }

  return filter;
};

export const generateBarColors = (str: string) => {
  let hash = 0;
  str.split("").forEach(char => {
    hash = char.charCodeAt(0) + ((hash << 5) - hash);
  });
  let colour = "#";
  for (let i = 0; i < 3; i++) {
    const value = (hash >> (i * 8)) & 0xff;
    colour += value.toString(16).padStart(2, "0");
  }
  return colour;
};

export const transformFinalFilters = (filters: any, supportedCustomFields: any) => {
  const finalFilters = cloneDeep(filters);
  const sprintCustomField = supportedCustomFields.find((item: any) =>
    (item.name || "").toLowerCase().includes("sprint")
  );
  const across = get(finalFilters, ["across"]);
  if (sprintCustomField && across && sprintCustomField.field_key === across) {
    set(finalFilters, ["across"], "sprint");
    if (finalFilters?.filter?.sort_xaxis?.includes("label")) {
      set(finalFilters, ["sort", 0, "id"], "sprint");
    }

    /** As we are changing the across, we have to replace occurances of custom field key with the name for filters. */
    const filterValues = finalFilters?.filter?.custom_fields?.[across];
    if (isArray(filterValues)) {
      set(finalFilters, ["filter", FilterTypes.SPRINT_NAMES], filterValues);
    }

    const partialMatchValue = finalFilters?.filter?.[JIRA_SCM_COMMON_PARTIAL_FILTER_KEY]?.[across];
    if (!!partialMatchValue) {
      unset(finalFilters, ["filter", JIRA_SCM_COMMON_PARTIAL_FILTER_KEY, across]);
      set(finalFilters, ["filter", JIRA_SCM_COMMON_PARTIAL_FILTER_KEY, FilterTypes.SPRINT_NAME], partialMatchValue);
    }
  }
  return finalFilters;
};
