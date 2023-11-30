import moment from "moment";
import { getCodeCoverage } from "dashboard/constants/helper";
import { forEach, get, unset } from "lodash";
import { stringToNumber } from "utils/commonUtils";
import {
  ISSUE_MANAGEMENT_REPORTS,
  jenkinsTrendReports,
  SCM_MAX_RECORDS_TREND_REPORTS
} from "dashboard/constants/applications/names";
import widgetConstants, { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { convertTimeData  } from "utils/dateUtils";
import { convertToDays, convertToMins } from 'utils/timeUtils'
import { jiraBacklogKeyMapping } from "dashboard/graph-filters/components/Constants";
import { convertEpochToDate, DateFormats } from "utils/dateUtils";
import { ALLOWED_WIDGET_DATA_SORTING } from "dashboard/constants/filter-name.mapping";
import { TransformFunctionParamType, TransformFunctionReturnType } from "custom-hooks/transform-function.type";
import {
  AssessmentResponseTimeReports,
  LevelopsReportsForNewTrendTransformer,
  LevelopsReportsWithMaxRecords,
  LevelopsTrendReports,
  LEVELOPS_REPORTS
} from "dashboard/reports/levelops/constant";
import { allTimestampsBetween } from "utils/timeUtils";

const formatTrendsData = (data: any) => {
  let _slicedData = data || [];
  _slicedData.forEach((item: any) => {
    Object.keys(item).forEach(key => {
      if (item[key] === undefined) {
        delete item[key];
      }
    });
  });

  return _slicedData;
};

const getAllTimeStamps = (apiData: any) => {
  if (apiData && apiData.length > 0) {
    const allApiTimeStamps = apiData
      .map((data: any) => data?.key && parseInt(data.key))
      .filter((timeStamp: any) => !!timeStamp);
    //If apiData has only one element then there is no
    //need to find the min max value return the same

    if ((allApiTimeStamps || []).length === 1) return allApiTimeStamps;

    const startStamp = Math.min(...allApiTimeStamps);

    const endStamp = Math.max(...allApiTimeStamps);

    let dates: any = [];
    let i = 0;
    let firstDate = startStamp;
    while (firstDate < endStamp) {
      const nextdate = 86400 * i + startStamp;
      firstDate = nextdate;
      dates = [...dates, nextdate];
      i++;
    }
    return dates;
  } else {
    return [];
  }
};

export const sonarqubeTrendReportTransformer = (data: any) => {
  const { reportType, apiData, widgetFilters } = data;

  let metric =
    (Array.isArray(widgetFilters?.filter?.metrics)
      ? widgetFilters?.filter?.metrics[0]
      : widgetFilters?.filter?.metrics) || "";
  let filteredApiDataByMetric: any[] = apiData || [];
  if (metric && reportType === "sonarqube_code_complexity_trend_report") {
    filteredApiDataByMetric = (apiData || []).filter((data: any) => data.additional_key === metric);
  }

  const allTimeStamps = getAllTimeStamps(filteredApiDataByMetric);
  const modifiedData = modifiedApiData(filteredApiDataByMetric, reportType);
  let transformData = genericTrendTransformer(modifiedData, allTimeStamps, reportType);

  if (transformData?.length && reportType === "sonarqube_effort_report_trends") {
    transformData = transformData.map((_data: any) => {
      if ("min" in _data) delete _data.min;
      if ("max" in _data) delete _data.max;

      return _data;
    });
  }

  return {
    data: transformData
  };
};

const modifiedApiData = (apiData: any, reportType: any) => {
  return apiData && apiData.length > 0
    ? apiData.map((item: any) => {
        if (reportType.includes("cicd_pipeline_jobs_duration_trend_report")) {
          delete item.count;
          delete item.sum;
        }

        if (reportType.includes("testrails") && item.total_tests && item.median) {
          delete item.total_tests;
        }

        if (reportType.includes("sonarqube") && item.total_issues && item.sum) {
          delete item.total_issues;
        }

        if (reportType === "jira_salesforce_escalation_time_report") {
          delete item.total_cases;
        }

        if (reportType.includes("levelops")) {
          delete item.id;
          if (item.median) {
            delete item.total;
          }
        }

        if (reportType.includes("job_runs_test_duration_trend_report") && item.total_tests) {
          delete item.total_tests;
        }

        if (["sonarqube_metrics_trend_report", "sonarqube_code_complexity_trend_report"].includes(reportType)) {
          delete item.sum;
          delete item.total;
        }

        if (["sonarqube_code_duplication_trend_report"].includes(reportType)) {
          delete item.additional_key;
          delete item.total;
        }

        return {
          ...item
        };
      })
    : [];
};

const getSortedData = (apiData: any, reportType: string, widgetFilters: any, timeFilterKeys: string[]) => {
  const across = get(widgetFilters, ["across"], undefined);
  // sorting logic for trend reports for constant sort_order
  const getSortFromConstant = getWidgetConstant(reportType, ["getSortKey"]);
  if (getSortFromConstant) {
    const _sortBy = getSortFromConstant?.({ across });
    const _sortOrder = getWidgetConstant(reportType, ["getSortKey"]) || "desc";
    if (_sortBy) {
      if (_sortOrder === "desc") {
        apiData = apiData.sort((a: any, b: any) => (b[_sortBy] || 0) - (a[_sortBy] || 0));
      } else {
        apiData = apiData.sort((a: any, b: any) => (a[_sortBy] || 0) - (b[_sortBy] || 0));
      }
    }
  }

  // for sorting x-axis for trend reports for dynamic sort_order
  const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
  const sortKey = get(widgetFilters, ["sort", 0, "id"], "");
  const isTimeFilterKey = !!timeFilterKeys && timeFilterKeys.includes(sortKey);
  if (allowedWidgetDataSorting && widgetFilters.sort && widgetFilters.sort[0].desc && !isTimeFilterKey) {
    apiData.reverse();
  } else if (allowedWidgetDataSorting && isTimeFilterKey) {
    const sortValue = get(widgetFilters, ["filter", "sort_xaxis"], "");
    sortValue.includes("latest-old") && apiData.reverse();
  }

  return apiData;
};

const genericTrendTransformer = (
  apiData: any,
  allTimeStamps: any,
  reportType: any,
  format = DateFormats.DAY_MONTH,
  defaultValue: number | string = "No Data"
) => {
  if (apiData && apiData.length > 0) {
    const apiDataKeys = Object.keys(apiData[0]).filter((element: any) => element !== "name" && element !== "key");

    return allTimeStamps.map((time: any) => {
      const name = convertEpochToDate(time, format, true);

      const trend = apiData.find(
        (element: any) => moment.unix(element.key).startOf("d").unix() === moment.unix(time).startOf("d").unix()
      );

      let total_tickets = undefined;

      const remianingKeysObject = apiDataKeys?.reduce((acc: any, next: any) => {
        const value = trend && trend[next] !== undefined ? trend[next] : defaultValue;
        return { ...acc, [next]: value };
      }, {});

      if (
        [
          "tickets_report",
          "tickets_report_trends",
          "zendesk_tickets_report",
          "zendesk_tickets_report_trends",
          "salesforce_c2f_trends",
          "zendesk_c2f_trends",
          "resolution_time_report",
          ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
          "azure_tickets_report_trends"
        ].includes(reportType)
      ) {
        total_tickets = trend?.total_tickets !== undefined ? trend?.total_tickets : defaultValue;
      } else if (["stage_bounce_report", "azure_stage_bounce_report"].includes(reportType)) {
        total_tickets = trend?.total_tickets;
      }

      return {
        ...trend,
        key: trend?.key || time.toString(),
        name,
        ...remianingKeysObject,
        total_tickets
      };
    });
  } else {
    return [];
  }
};

export const makeDataContinuousWithPreviousData = (data: any[]): any[] => {
  const allApiTimeStamps = data
    .map((item: any) => item?.key && parseInt(item.key))
    .filter((timeStamp: any) => !!timeStamp);

  if ((allApiTimeStamps || []).length === 1) return data;

  const newData: any[] = [];

  const startStamp = Math.min(...allApiTimeStamps);

  const endStamp = Math.max(...allApiTimeStamps);

  const allTimestamps = allTimestampsBetween(startStamp, endStamp);

  forEach(allTimestamps, timestamp => {
    const currData = (data || []).find((item: any) => moment.unix(item.key).utc().startOf("d").unix() === timestamp);
    if (currData) {
      newData.push(currData);
    } else {
      let newCurrData: any = {
        ...(newData[newData.length - 1] ?? {}),
        key: timestamp.toString()
      };
      newData.push(newCurrData);
    }
  });

  return newData;
};

export const levelopsReportsTrendReportTransformer = (
  data: TransformFunctionParamType
): TransformFunctionReturnType => {
  const { metadata, apiData, reportType } = data;
  const maxRecords = get(metadata, ["max_records"], undefined);
  const dataWithSameDates: Record<string, any[]> = {};
  let mappedData = apiData ?? [];
  if (LevelopsTrendReports.includes(reportType as LEVELOPS_REPORTS)) {
    mappedData = makeDataContinuousWithPreviousData(apiData ?? []);
  }
  forEach(mappedData ?? [], item => {
    const timestamp = moment.unix(parseInt(item.key)).utc().startOf("d").unix().toString();
    dataWithSameDates[timestamp] = [...(dataWithSameDates[timestamp] ?? []), { ...item }];
  });
  const numberKeys = ["total", "median", "mean", "max"];
  let trendData: any[] = [];
  forEach(Object.keys(dataWithSameDates), date => {
    const data = (dataWithSameDates[date] ?? []).reduce((acc: Record<string, any>, curr: Record<string, any>) => {
      const newData: any = { ...curr, key: date };
      Object.keys(curr).forEach(key => {
        if (numberKeys.includes(key)) {
          newData[key] = acc[key] + curr[key];
        }
      });
      acc = newData;
      return acc;
    });
    trendData.push(data);
  });
  const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);

  if (convertTo) {
    trendData = convertTimeData(trendData, convertTo);
  }
  forEach(trendData, (item, index) => {
    const name = convertEpochToDate(parseInt(item.key), DateFormats.DAY_MONTH, true);
    trendData[index] = { ...item, name };
    unset(trendData[index], "id");
    if (AssessmentResponseTimeReports.includes(reportType as LEVELOPS_REPORTS)) {
      unset(trendData[index], "total");
    }
  });
  if (LevelopsReportsWithMaxRecords.includes(reportType as LEVELOPS_REPORTS)) {
    const sliceStart = trendData?.length > maxRecords ? trendData.length - maxRecords : 0;
    trendData = trendData.slice(sliceStart, trendData.length);
  }
  return { data: [...trendData] };
};

export const levelopsAssessmentTimeResponseTableTransformer = (
  data: TransformFunctionParamType
): TransformFunctionReturnType => {
  const { widgetFilters, apiData } = data;
  const across = get(widgetFilters, ["across"], undefined);
  const timeAcross: string[] = ["created", "updated"];
  if (timeAcross.includes(across)) {
    return levelopsReportsTrendReportTransformer(data);
  }
  return { data: apiData };
};

export const trendReportTransformer = (data: any) => {
  const { reportType, apiData, metadata, widgetFilters, timeFilterKeys } = data;
  if (LevelopsReportsForNewTrendTransformer.includes(reportType)) {
    return levelopsReportsTrendReportTransformer(data as TransformFunctionParamType);
  }
  if (apiData) {
    let _apiData = apiData;
    const maxRecords = get(metadata, ["max_records"], apiData.length);
    const interval = get(widgetFilters, ["interval"], undefined);
    const across = get(widgetFilters, ["across"], undefined);
    let dateFormat = DateFormats.DAY_MONTH;

    if (["salesforce_tickets_report_trends", "snyk_vulnerability_report"].includes(reportType)) {
      _apiData = _apiData.map((item: any) => {
        const timestamp = moment(item.key).unix();
        return {
          ...item,
          key: timestamp.toString()
        };
      });
    }
    if (
      [
        "resolution_time_report",
        "tickets_report",
        "zendesk_tickets_report",
        ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT,
        ...jenkinsTrendReports
      ].includes(reportType) &&
      interval
    ) {
      switch (interval) {
        case "day":
          dateFormat = DateFormats.DAY;
          break;
        case "week":
          dateFormat = DateFormats.DAY;
          break;
        case "month":
          dateFormat = DateFormats.MONTH;
          break;
        case "quarter":
          dateFormat = DateFormats.QUARTER;
      }
    }

    const allTimeStamps = getAllTimeStamps(_apiData);

    const modifiedData = modifiedApiData(_apiData, reportType);

    let trendsData = genericTrendTransformer(modifiedData, allTimeStamps, reportType, dateFormat, 0);

    const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);

    if (convertTo) {
      trendsData = convertTimeData(trendsData, convertTo);
    }

    trendsData = formatTrendsData(trendsData);
    if (SCM_MAX_RECORDS_TREND_REPORTS.includes(reportType)) {
      trendsData = trendsData.map((data: any) => {
        const { count } = data;
        unset(data, ["sum"]);
        unset(data, ["count"]);
        return {
          ...data,
          total_prs: count
        };
      });
      const sliceStart = trendsData?.length > maxRecords ? trendsData.length - maxRecords : 0;
      trendsData = trendsData.slice(sliceStart, trendsData.length);
    }

    // sorting logic for trend reports for constant sort_order
    let getSortFromConstant = getWidgetConstant(reportType, ["getSortKey"]);
    if (getSortFromConstant) {
      let _sortBy = getSortFromConstant?.({ across });
      let _sortOrder = getWidgetConstant(reportType, ["getSortKey"]) || "desc";
      if (_sortBy) {
        if (_sortOrder === "desc") {
          trendsData = trendsData.sort((a: any, b: any) => (b[_sortBy] || 0) - (a[_sortBy] || 0));
        } else {
          trendsData = trendsData.sort((a: any, b: any) => (a[_sortBy] || 0) - (b[_sortBy] || 0));
        }
      }
    }

    // for sorting x-axis for trend reports for dynamic sort_order
    const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
    const sortKey = get(widgetFilters, ["sort", 0, "id"], "");
    if (
      allowedWidgetDataSorting &&
      widgetFilters.sort &&
      widgetFilters.sort[0].desc &&
      !timeFilterKeys.includes(sortKey)
    ) {
      trendsData.reverse();
    } else if (allowedWidgetDataSorting && timeFilterKeys?.includes(sortKey)) {
      const sortValue = get(widgetFilters, ["filter", "sort_xaxis"], "");
      sortValue.includes("latest-old") && trendsData.reverse();
    }

    return {
      data: trendsData
    };
  }
};

export const cicdTrendReportTransform = (data: any) => {
  const { reportType, apiData, metadata, widgetFilters, timeFilterKeys } = data;
  if (apiData) {
    const maxRecords = get(metadata, ["max_records"], apiData.length);
    const interval = get(widgetFilters, ["interval"], "month");
    const across = get(widgetFilters, ["across"], undefined);

    let trendsData = apiData.map((item: any) => {
      let name = item.additional_key;
      const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
      name = xAxisLabelTransform?.({ item, interval, across }) || name;
      return {
        ...item,
        name
      };
    });

    const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);

    if (convertTo) {
      trendsData = convertTimeData(trendsData, convertTo);
    }

    trendsData = formatTrendsData(trendsData);
    if (SCM_MAX_RECORDS_TREND_REPORTS.includes(reportType)) {
      const sliceStart = trendsData?.length > maxRecords ? trendsData.length - maxRecords : 0;
      trendsData = trendsData.slice(sliceStart, trendsData.length);
    }

    // sorting logic for trend reports for constant sort_order
    let getSortFromConstant = getWidgetConstant(reportType, ["getSortKey"]);
    if (getSortFromConstant) {
      let _sortBy = getSortFromConstant?.({ across });
      let _sortOrder = getWidgetConstant(reportType, ["getSortKey"]) || "desc";
      if (_sortBy) {
        if (_sortOrder === "desc") {
          trendsData = trendsData.sort((a: any, b: any) => (b[_sortBy] || 0) - (a[_sortBy] || 0));
        } else {
          trendsData = trendsData.sort((a: any, b: any) => (a[_sortBy] || 0) - (b[_sortBy] || 0));
        }
      }
    }

    // for sorting x-axis for trend reports for dynamic sort_order
    const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
    const sortKey = get(widgetFilters, ["sort", 0, "id"], "");
    if (
      allowedWidgetDataSorting &&
      widgetFilters.sort &&
      widgetFilters.sort[0].desc &&
      !timeFilterKeys.includes(sortKey)
    ) {
      trendsData.reverse();
    } else if (allowedWidgetDataSorting && timeFilterKeys.includes(sortKey)) {
      const sortValue = get(widgetFilters, ["filter", "sort_xaxis"], "");
      !sortValue.includes("latest-old") && trendsData.reverse();
    }

    return {
      data: trendsData
    };
  }
};

export const cicdJobCountTransformer = (data: any) => {
  const { reportType, apiData, widgetFilters, timeFilterKeys } = data;
  if (apiData) {
    const interval = get(widgetFilters, ["interval"], undefined);
    const across = get(widgetFilters, ["across"], undefined);
    const trendsData = apiData.map((item: any) => {
      let name = item.additional_key;
      const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
      name = xAxisLabelTransform?.({ item, interval, across }) || name;
      return {
        ...item,
        name
      };
    });

    const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
    const sortKey = get(widgetFilters, ["sort", 0, "id"], "");

    if (allowedWidgetDataSorting && timeFilterKeys.includes(sortKey)) {
      const sortValue = get(widgetFilters, ["filter", "sort_xaxis"], "");
      sortValue.includes("old-latest") && trendsData.reverse();
    }
    return {
      data: trendsData
    };
  }
};

export const backlogTrendTransformer = (data: any) => {
  const { apiData, metadata, isMultiTimeSeriesReport } = data;
  let _apiData = apiData;
  if (apiData) {
    // const metrics = getMetrics(metadata);
    let metrics = get(metadata, ["metrics"], ["median", "total_tickets"]);
    if (metrics.length === 0) {
      metrics = ["median", "total_tickets"];
    }
    _apiData = apiData.map((item: any) => {
      const _item = Object.keys(item).reduce((acc: any, next: any) => {
        if (metrics.includes(next)) {
          const mappedKey = get(jiraBacklogKeyMapping, [next], "");
          return { ...acc, [mappedKey]: item[next] };
        }
        return acc;
      }, {});
      const name = convertEpochToDate(item.key, DateFormats.DAY, true);
      const mappedItem: any = {
        ..._item,
        name: name,
        key: item.key,
        additional_key: item.additional_key
      };
      if (isMultiTimeSeriesReport) {
        mappedItem["timestamp"] = item.key;
      }
      return mappedItem;
    });
    return {
      data: _apiData
    };
  }
};

export const scmCicdTrendTransformer = (data: any) => {
  const { reportType, apiData, widgetFilters, timeFilterKeys } = data;
  const across = get(widgetFilters, ["across"], undefined);
  const interval = get(widgetFilters, ["interval"], "day");
  const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);
  let _apiData = apiData || [];
  const maxRecords = get(data, ["metadata", "max_records"], _apiData?.length);
  if (_apiData) {
    _apiData = _apiData.map((item: any) => {
      const _item = Object.keys(item).reduce((acc: any, next: any) => {
        if (["min", "median", "max"].includes(next) && convertTo) {
          return {
            ...acc,
            [next]: convertTo === "days" ? convertToDays(item[next]) : convertToMins(item[next])
          };
        }
        return { ...acc, [next]: item[next] };
      }, {});

      let name = item.additional_key;
      const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
      name = xAxisLabelTransform?.({ item, interval, across }) || name;

      delete _item.additional_key;
      delete _item.sum;
      delete _item.total;
      delete _item.count;

      return {
        ..._item,
        name: name
      };
    });

    const slice_Start = _apiData?.length > maxRecords ? _apiData?.length - maxRecords : 0;
    const slice_end = _apiData?.length;
    _apiData = _apiData.slice(slice_Start, slice_end);
  }

  const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
  const sortKey = get(widgetFilters, ["sort", 0, "id"], "");

  if (allowedWidgetDataSorting && timeFilterKeys.includes(sortKey)) {
    const sortValue = get(widgetFilters, ["filter", "sort_xaxis"], "");
    sortValue.includes("old-latest") && _apiData.reverse();
  }
  return {
    data: _apiData
  };
};

export const bullseyeTrendTransformer = (data: any) => {
  const { reportType, apiData, filters } = data;
  const allTimeStamps = getAllTimeStamps(apiData);
  let trendsData = genericTrendTransformer(apiData, allTimeStamps, reportType);
  let dataKey = get(widgetConstants, [reportType, "dataKey"], undefined);
  if (filters?.filter?.metric) {
    dataKey = filters?.filter?.metric;
  }

  if (dataKey) {
    trendsData = trendsData.map((item: any) => {
      let dataValue = get(item, ["additional_counts", "bullseye_coverage_metrics", dataKey], 0);
      if (dataKey === "coverage_percentage") {
        dataValue = getCodeCoverage(item);
      }
      return {
        name: item.name,
        [dataKey]: stringToNumber(dataValue)
      };
    });

    return {
      data: trendsData
    };
  }
  return { data: [] };
};

export const azureTrendTransformer = (data: any) => {
  const { widgetFilters } = data;
  const across = get(widgetFilters, "across", "trend");
  const newApiData = get(data, ["apiData", "0", across, "records"], []);
  return trendReportTransformer({ ...data, apiData: newApiData });
};

export const jenkinsConfigChangeCountTrendTransformer = (data: any) => {
  const { reportType, apiData, widgetFilters, timeFilterKeys } = data;
  if (apiData) {
    let _apiData = apiData;
    const interval = get(widgetFilters, ["interval"], undefined);
    let dateFormat = DateFormats.DAY_MONTH;

    if (interval) {
      switch (interval) {
        case "day":
          dateFormat = DateFormats.DAY;
          break;
        case "week":
          dateFormat = DateFormats.DAY;
          break;
        case "month":
          dateFormat = DateFormats.MONTH;
          break;
        case "quarter":
          dateFormat = DateFormats.QUARTER;
      }
    }

    const allTimeStamps = apiData
      .map((data: any) => data?.key && parseInt(data.key))
      .filter((timeStamp: any) => !!timeStamp);

    const modifiedData = modifiedApiData(_apiData, reportType);

    let trendsData = genericTrendTransformer(modifiedData, allTimeStamps, reportType, dateFormat, 0);

    const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);

    if (convertTo) {
      trendsData = convertTimeData(trendsData, convertTo);
    }

    trendsData = formatTrendsData(trendsData);

    trendsData = getSortedData(trendsData, reportType, widgetFilters, timeFilterKeys);
    return {
      data: trendsData
    };
  }
};
