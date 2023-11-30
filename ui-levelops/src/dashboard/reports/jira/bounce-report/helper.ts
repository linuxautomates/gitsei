import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { idFilters } from "../commonJiraReports.constants";
import { get } from "lodash";
import widgetConstants from "../../../constants/widgetConstants";
import { trendReportTransformer } from "custom-hooks/helpers";
import { convertTimeData } from 'utils/dateUtils'
import { genericSeriesDataTransformer } from "custom-hooks/helpers/seriesData.helper";
import { TimeFieldKeys, UNASSIGNED } from "./constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const bounceReportOnChartClickPayloadHandler = (args: { data: basicMappingType<any>; across?: string }) => {
  const { data, across } = args;
  if (across && across.includes("customfield_")) {
    return data.name;
  } else if (across && [TimeFieldKeys.ISSUE_CREATED, TimeFieldKeys.ISSUE_UPDATED].includes(across)) {
    return data?.activeLabel;
  }
  if (across && idFilters.includes(across)) {
    return { id: data.key || UNASSIGNED, name: data.additional_key || UNASSIGNED };
  }
  return data.key;
};

export const bounceReportTransformer = (data: any) => {
  const { records, sortBy, reportType, widgetFilters, filters, supportedCustomFields } = data;
  let { apiData, timeFilterKeys } = data;
  const application = get(widgetConstants, [reportType, "application"], undefined);
  const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);
  const across = get(widgetFilters, ["across"], undefined);
  const labels = get(widgetFilters, ["filter", "labels"], undefined);

  if (labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }

  if (
    application === IntegrationTypes.JIRA &&
    across &&
    [TimeFieldKeys.ISSUE_CREATED, TimeFieldKeys.ISSUE_UPDATED, TimeFieldKeys.ISSUE_RESOLVED].includes(across)
  ) {
    return trendReportTransformer({ apiData, reportType, widgetFilters, timeFilterKeys });
  }

  let seriesData = genericSeriesDataTransformer(
    apiData,
    records,
    sortBy,
    reportType,
    {},
    filters,
    supportedCustomFields
  );

  if (convertTo) {
    seriesData = convertTimeData(seriesData, convertTo);
  }

  return {
    data: seriesData
  };
};
