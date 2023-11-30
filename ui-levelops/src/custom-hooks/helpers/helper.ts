import { staticPriorties } from "./../../shared-resources/charts/jira-prioirty-chart/helper";
import { getGroupByRootFolderKey } from "configurable-dashboard/helpers/helper";
import { ALLOW_KEY_FOR_STACKS } from "dashboard/constants/filter-key.mapping";
import { FileReports, FILES_REPORT_ROOT_FOLDER_KEY } from "dashboard/constants/helper";
import { get, uniq, unset } from "lodash";
import moment from "moment";
import {
  CUSTOM_STACK_FILTER_NAME_MAPPING,
  DEFAULT_MAX_RECORDS,
  CUSTOM_FIELD_NAME_STORY_POINTS,
  DEFAULT_MAX_STACKED_ENTRIES
} from "dashboard/constants/constants";
import widgetConstants, { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { getMappedSortValue } from "shared-resources/charts/helper";
import { backlogSeriesDataTransformer } from "./seriesData.helper";
import { backlogTrendTransformer, trendReportTransformer } from "./trendReport.helper";
import { JiraReports } from "dashboard/constants/enums/jira-reports.enum";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { ALLOWED_WIDGET_DATA_SORTING } from "dashboard/constants/filter-name.mapping";
import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import {
  issueManagementReports,
  ISSUE_MANAGEMENT_REPORTS,
  JENKINS_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT
} from "dashboard/constants/applications/names";
import { METRIC_VALUE_KEY_MAPPING } from "dashboard/reports/azure/issues-report/constant";
// @ts-ignore
import randomColor from "randomcolor";
import { convertTimeData } from "utils/dateUtils";

const colorArray = ["blue", "red", "green", "orange", "purple", "yellow", "pink"];

export const getStacksData = (stacks: Array<any>, filters: any, reportType: string, stackKey: string = "count") => {
  let stackedData = {};
  if (stacks) {
    if (stacks.length > 10) {
      stackedData = stacks.slice(0, 10).reduce((pre: any, next: any) => {
        const name = getStackName(next, filters, reportType);
        return {
          ...pre,
          [name]: next[stackKey]
        };
      }, {});
      const other = stacks.slice(10, stacks.length).reduce(
        (acc: any, item: any) => {
          return {
            Other: item[stackKey] + acc.Other
          };
        },
        { Other: 0 }
      );
      stackedData = { ...stackedData, ...other };
    } else {
      stackedData = stacks.reduce((pre: any, next: any) => {
        const name = getStackName(next, filters, reportType);
        return {
          ...pre,
          [name]: next[stackKey]
        };
      }, {});
    }
  }

  return stackedData;
};

const getStackName = (next: any, filters: any, reportType: string) => {
  let name: string;
  if (filters?.stacks) {
    if (filters.stacks.includes("trend") && reportType !== JENKINS_REPORTS.CICD_PIPELINE_JOBS_COUNT_REPORT) {
      return moment.unix(parseInt(next.key)).utc().format("MM/DD");
    }
    if (filters.stacks.includes("triage_rule")) {
      return next.key;
    }
  }
  if (
    [JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_REPORT, ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_REPORT].includes(
      reportType as any
    )
  ) {
    name = next.additional_key || next.key;
  } else {
    name = next.additional_key ? `${next.additional_key}/${next.key}` : next.key;
  }
  return name;
};

export const scmFilesTransform = (data: any) => {
  const { apiData, reportType, widgetFilters } = data;
  let subTitleKey = "filename";
  let dataKey =
    reportType === "scm_jira_files_report"
      ? "num_issues"
      : widgetFilters.sort
      ? getMappedSortValue(widgetFilters.sort[0].id) || "num_commits"
      : "num_commits";

  if (
    [FileReports.SCM_FILES_REPORT, FileReports.SCM_JIRA_FILES_REPORT].includes(reportType) &&
    get(data, ["metadata", getGroupByRootFolderKey(reportType)], undefined)
  ) {
    dataKey = reportType?.includes("jira") ? "total_issues" : "count";
    subTitleKey = "key";
  }
  if (
    [FileReports.JIRA_ZENDESK_FILES_REPORT, FileReports.JIRA_SALESFORCE_FILES_REPORT].includes(reportType) &&
    get(data, ["metadata", getGroupByRootFolderKey(reportType)], undefined)
  ) {
    dataKey = reportType?.includes("salesforce") ? "total_cases" : "total_tickets";
    subTitleKey = "key";
  }

  const repos = Object.keys(
    (apiData || []).reduce((acc: { [x: string]: boolean }, obj: { repo_id: any }) => {
      const repo = obj.repo_id;
      acc[repo] = true;
      return acc;
    }, {})
  );

  const fileColors = (apiData || []).reduce((acc: any, obj: { repo_id: string; filename: string }) => {
    const file = (obj as any)[subTitleKey];
    if (Object.keys(acc).includes(file)) {
      return acc;
    }
    const repo = obj.repo_id;
    const hue = colorArray[repos.indexOf(repo) % colorArray.length];
    acc[file] = randomColor({ hue: hue });
    return acc;
  }, {});

  let total = 0;
  let filesData = apiData
    ? apiData
        .sort((a: any, b: any) => a.repo_id.localeCompare(b.repo_id) || b[dataKey] - a[dataKey])
        .map((item: any) => {
          total = total + item[dataKey];
          return {
            ...item,
            title: item.repo_id
              ? {
                  label: "Repo Id",
                  value: item.repo_id
                }
              : {},
            subTitle: item[subTitleKey]
              ? {
                  label:
                    subTitleKey === "key"
                      ? reportType === FileReports.SCM_JIRA_FILES_REPORT
                        ? "Scm Module"
                        : "Module"
                      : "File Name",
                  value: item[subTitleKey].split("/").pop()
                }
              : {},
            color: fileColors[item[subTitleKey]]
          };
        })
    : [];

  if (
    [
      FileReports.JIRA_SALESFORCE_FILES_REPORT,
      FileReports.JIRA_ZENDESK_FILES_REPORT,
      FileReports.SCM_FILES_REPORT,
      FileReports.SCM_JIRA_FILES_REPORT
    ].includes(reportType) &&
    get(data, ["metadata", getGroupByRootFolderKey(reportType)], undefined)
  ) {
    filesData = filesData.map((item: any) => {
      let newItem = { ...item, [FILES_REPORT_ROOT_FOLDER_KEY]: item.key || "" };
      unset(newItem, ["key"]);
      return newItem;
    });
  }

  return {
    data: filesData,
    total: total,
    dataKey
  };
};

export const jenkinsJobConfigChangeTransform = (data: any) => {
  const { apiData, filters, reportType, records } = data;
  const change_data =
    apiData && apiData.length > 0
      ? apiData.slice(0, records || 20).map((item: any) => {
          const { key, count, stacks } = item;
          let stackedData = {};
          if (stacks) {
            stackedData = getStacksData(stacks, filters, reportType, "count");
          }
          return {
            count,
            name: key,
            ...stackedData
          };
        })
      : [];

  return { data: change_data };
};

// created a new constant because not all keys are supported from ADDITIONAL_KEY_FILTER constant
const ISSUE_ADDITIONAL_FILTER_KEY = ["assignee", "reporter"];

export const genericTicketsReportTransformer = (
  data: any,
  dataKey: string = "total_tickets",
  transformEmptyKeyTo = "UNKNOWN"
) => {
  const { records, widgetFilters, reportType, metadata, isMultiTimeSeriesReport, supportedCustomFields } = data;
  let { apiData, timeFilterKeys } = data;

  const labels = get(widgetFilters, ["filter", "labels"], undefined);
  const appliedStacks = get(widgetFilters, ["stacks"], []);
  const across = get(widgetFilters, ["across"], undefined);
  const interval = get(widgetFilters, ["interval"], undefined);
  const partialLabels = get(widgetFilters, ["filter", "partial_match", "labels"], {});
  const partialLabelKey = Object.keys(partialLabels);
  const filtersStacks = get(widgetFilters, ["stacks", 0], undefined);
  const isCustomStacks = filtersStacks === "custom_field" ? true : false;
  const customField = get(widgetFilters, ["filter", "custom_stacks", 0], undefined);
  const application = get(widgetConstants, [reportType, "application"], undefined);
  const fetchEpic = get(widgetFilters, ["filter", "fetch_epic_summary"], undefined);
  let CustomFieldType: any = undefined;
  if (across.includes("customfield_")) {
    CustomFieldType = supportedCustomFields?.find((item: any) => across === item?.field_key)?.field_type;
  }

  if (reportType !== JiraReports.JIRA_TICKETS_REPORT && labels?.length && across === "label") {
    apiData = apiData?.filter((filters: any) => labels.includes(filters.key));
  }

  if (reportType !== JiraReports.JIRA_TICKETS_REPORT && partialLabelKey.length && across === "label") {
    apiData = apiData?.filter((filters: any) => filters?.key.includes(partialLabels[partialLabelKey[0]]));
  }

  const getShouldSliceFromEnd = getWidgetConstant(reportType, ["shouldSliceFromEnd"]);
  const shouldSliceFromEnd = getShouldSliceFromEnd?.({ interval });
  let slice_start = apiData?.length > 0 && shouldSliceFromEnd ? apiData.length - records : 0;
  const slice_end = apiData?.length > 0 && shouldSliceFromEnd ? apiData.length : records || DEFAULT_MAX_RECORDS;
  if (slice_start < 0) slice_start = 0;

  let finalApiData = [];
  if (apiData && apiData.length > 0) {
    const sortApiDataHandler = getWidgetConstant(reportType, ["sortApiDataHandler"]);
    finalApiData = sortApiDataHandler?.({ across, apiData }) || [];
    finalApiData = apiData.slice(slice_start, slice_end).map((item: any) => {
      let { key, stacks } = item;

      let name = key;
      const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
      const maxStackEntries = getWidgetConstant(reportType, ["maxStackEntries"], DEFAULT_MAX_STACKED_ENTRIES);
      const weekDateFormat = get(data, ["metadata", "weekdate_format"], undefined);
      name = xAxisLabelTransform?.({ item, interval, across, weekDateFormat, CustomFieldType }) || name;

      let stackedData = {};
      let stackedTicketsTotal = 0;
      if (stacks) {
        let stackedTicketsOtherTotal = stacks.slice(maxStackEntries, stacks.length).reduce((acc: number, obj: any) => {
          acc = acc + obj[dataKey];
          return acc;
        }, 0);

        if (issueManagementReports.includes(reportType) && appliedStacks?.[0] === "priority") {
          stacks = stacks.map((item: any) => ({ ...item, key: (staticPriorties as any)?.[item.key] ?? item.key }));
        }
        const CustomFieldType = supportedCustomFields.find((item: any) => item?.field_key === customField)?.field_type;
        stackedData = stacks
          .sort((a: any, b: any) => b[dataKey] - a[dataKey])
          .slice(0, maxStackEntries)
          .reduce((acc: any, obj: any) => {
            // if key = "" then replace it with UNKNOWN
            // Update: Instead of UNKNOWN, it will take the transformEmptyKeyTo param
            let objKey = obj.key;
            // considering filtersStacks value to be string, not array of string
            if (
              application &&
              ["jira", "azure_devops"].includes(application) &&
              (ISSUE_ADDITIONAL_FILTER_KEY.includes(across) ||
                ISSUE_ADDITIONAL_FILTER_KEY.includes(filtersStacks) ||
                fetchEpic)
            ) {
              objKey = obj?.additional_key || obj?.key;
            }
            if (isCustomStacks && customField && CustomTimeBasedTypes.includes(CustomFieldType)) {
              objKey = moment(parseInt(objKey)).format("DD-MM-YYYY h:mm:ss");
            }
            acc[objKey || transformEmptyKeyTo] = (acc[objKey || transformEmptyKeyTo] || 0) + obj[dataKey];
            stackedTicketsTotal += obj[dataKey];
            return acc;
          }, {});
        const missingTickets =
          stackedTicketsOtherTotal + Math.max(item[dataKey] - (stackedTicketsTotal + stackedTicketsOtherTotal), 0);
        const missingTicketsStackKey =
          (metadata?.[CUSTOM_STACK_FILTER_NAME_MAPPING] || "").toLowerCase() === CUSTOM_FIELD_NAME_STORY_POINTS
            ? "Unestimated"
            : "Other";
        if (missingTickets > 0) {
          stackedData = {
            ...stackedData,
            [missingTicketsStackKey]: missingTickets
          };
        }

        let curStackedData: any = {
          name,
          id: item.id ?? name,
          ...stackedData
        };

        if (isMultiTimeSeriesReport) {
          curStackedData["timestamp"] = key;
        }

        const allowKeyInStacksdata = getWidgetConstant(reportType, ALLOW_KEY_FOR_STACKS);
        if (!!allowKeyInStacksdata) {
          curStackedData = {
            ...(curStackedData || {}),
            key
          };
        }
        return curStackedData;
      }
      let mappedItem: any = {
        key,
        name,
        id: item.id ?? name,
        [dataKey]: item[dataKey],
        ...stackedData
      };
      if (isMultiTimeSeriesReport) {
        mappedItem["timestamp"] = key;
      }
      return mappedItem;
    });

    const getShouldReverseApiData = getWidgetConstant(reportType, ["shouldReverseApiData"]);
    const shouldReverseApiData = getShouldReverseApiData?.({ interval, across });

    const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
    const sortKey = get(widgetFilters, ["sort", 0, "id"], "");
    const sortValue = get(widgetFilters, ["filter", "sort_xaxis"], "");

    if (
      shouldReverseApiData ||
      (allowedWidgetDataSorting && timeFilterKeys.includes(sortKey) && sortValue && sortValue.includes("old-latest"))
    ) {
      finalApiData.reverse();
    }
  }

  const visualization = get(widgetFilters, ["filter", "visualization"], IssueVisualizationTypes.BAR_CHART);
  if (
    visualization === IssueVisualizationTypes.PERCENTAGE_STACKED_BAR_CHART &&
    [JiraReports.JIRA_TICKETS_REPORT, ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT].includes(reportType)
  ) {
    finalApiData = finalApiData.map((item: any) => {
      let newItem = { ...item };
      const allKeys = Object.keys(item).filter((key: any) => !["id", "key", "name"].includes(key));
      const total = allKeys.reduce((acc: any, next: any) => {
        return acc + item[next];
      }, 0);
      allKeys.forEach((key: any) => {
        newItem = {
          ...newItem,
          [key]: (item[key] / (total || 1)) * 100
        };
      });
      return {
        ...newItem
      };
    });
  }
  return finalApiData;
};

const formatData = (data: any) => {
  let slicedData = data.map((item: any) => {
    Object.keys(item).forEach(key => {
      if (item[key] === undefined) {
        delete item[key];
      }
    });
    return item;
  });

  return slicedData;
};

export const jiraTicketsReportChangeTransform = (data: any) => {
  const { widgetFilters, apiData, reportType, supportedCustomFields } = data;
  const across = get(widgetFilters, ["across"], undefined);
  const interval = get(widgetFilters, ["interval"], undefined);
  let CustomFieldType: any = undefined;
  if (across.includes("customfield_")) {
    CustomFieldType = supportedCustomFields?.find((item: any) => across === item?.field_key)?.field_type;
  }

  const metrics = get(widgetFilters, ["filter", "metric"], "ticket");
  const isDonut =
    get(widgetFilters, ["filter", "visualization"], IssueVisualizationTypes.BAR_CHART) ===
    IssueVisualizationTypes.DONUT_CHART;

  let _data = [];
  let valueKey = metrics === "ticket" ? "total_tickets" : "total_story_points";

  if (Array.isArray(metrics)) {
    valueKey = metrics[0] === "ticket" ? "total_tickets" : "total_story_points";
  }

  if (isDonut) {
    _data = apiData
      ?.map((item: any) => {
        let name = item.key;
        const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
        name = xAxisLabelTransform?.({ item, interval, across, CustomFieldType }) || name;
        return {
          name: name,
          value: item[valueKey],
          key: item.key
        };
      })
      .sort((a: any, b: any) => b.value - a.value);
  } else {
    if (metrics === "story_point") {
      _data = genericTicketsReportTransformer(data, "total_story_points");
    } else {
      _data = genericTicketsReportTransformer(data, undefined, "");
    }
  }

  return { data: _data, all_data: apiData };
};

export const coverityDefectsReportTransform = (data: any) => ({
  data: genericTicketsReportTransformer(data, "total_defects")
});

export const mappingKeys = (apiData: any, reportType: any, matrics: String) => {
  return apiData && apiData.length > 0
    ? apiData.map((item: any) => {
        if (["jira_backlog_trend_report"].includes(reportType)) {
          switch (matrics) {
            case "median_age":
              return {
                min: item.min,
                max: item.max,
                median: item.median
              };
            case "90_percentile_age":
              return {
                name: item.name,
                p90: item.p90
              };
            case "average_age":
              return {
                stat: parseInt(item.mean),
                statTrend: parseInt(item.mean)
              };
            case "number_of_tickets":
              return {
                total_tickets: item.total_tickets
              };
            case "total_story_points":
              return {
                total_story_points: item.total_story_points
              };
          }
        }
      })
    : [];
};

export const jiraBacklogTransformerWrapper = (data: any) => {
  const { apiData, metadata, reportType, widgetFilters, timeFilterKeys } = data;
  const graphType = get(metadata, ["graphType"], "bar_chart");
  let seriesData = apiData;
  if (apiData) {
    switch (graphType) {
      case "bar_chart":
        seriesData = backlogSeriesDataTransformer(data);
        break;
      case "line_chart":
        seriesData = backlogTrendTransformer(data);
        break;
    }
  }

  let mappedData = seriesData?.data || [];

  const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
  const sortKey = get(widgetFilters, ["sort", 0, "id"], "");

  if (allowedWidgetDataSorting && timeFilterKeys.includes(sortKey)) {
    const sortValue = get(widgetFilters, ["filter", "sort_xaxis"], "");
    sortValue.includes("old-latest") && mappedData && mappedData.reverse();
  }
  return {
    data: seriesData?.data || []
  };
};

export const sonarqubeIssuesReportTransformer = (data: any) => {
  return { data: genericTicketsReportTransformer(data, "total_issues") };
};

export const microsoftIssuesReportTransformer = (data: any) => {
  const generic_result = genericTicketsReportTransformer(data, "count");
  const result = { data: generic_result };
  return result;
};

export const levelopsAsssessmentCountReportTransformer = (data: any) => {
  const { widgetFilters } = data;

  const across = get(widgetFilters, ["across"], undefined);

  if (across && ["created", "updated"].includes(across)) {
    return trendReportTransformer(data);
  }

  let reportData = genericTicketsReportTransformer(data, "total");

  if (widgetFilters.stacks && ["completed", "submitted"].includes(widgetFilters.stacks[0])) {
    const replaceHash = {
      true: widgetFilters.stacks[0],
      false: `Not ${widgetFilters.stacks[0]}`
    };
    reportData = reportData.map((record: any) => {
      let newRec = {};
      Object.keys(record).forEach((key: string) => {
        if (Object.keys(replaceHash).includes(key)) {
          // @ts-ignore
          newRec[replaceHash[key]] = record[key];
        } else {
          // @ts-ignore
          newRec[key] = record[key];
        }
      });
      return newRec;
    });
  }

  return { data: formatData(reportData) };
};

export const timeDurationGenericTransform = (data: any) => {
  const { apiData, records, reportType } = data;

  const convertTo = get(widgetConstants, [reportType, "convertTo"], undefined);

  let duration_data =
    apiData && apiData.length > 0
      ? apiData.map((item: any) => {
          const { key, count, ...data } = item;
          return {
            ...data,
            name: key
          };
        })
      : [];

  if (convertTo) {
    duration_data = convertTimeData(duration_data, convertTo);
  }

  const max = Math.min(records || DEFAULT_MAX_RECORDS, duration_data.length);

  return { data: duration_data.slice(0, max) };
};

export const jenkinsPipelineJobsCountTransformer = (data: any) => {
  const { filters, apiData } = data;
  const maxRecords = get(data, ["metadata", "max_records"], apiData?.length);
  if (!apiData || (apiData && apiData.length === 0)) {
    return {
      data: [
        {
          color: "#84b8f4",
          noData: "No Jobs Found",
          subTitle: {
            label: "Job",
            value: "No Jobs Found"
          }
        }
      ],
      total: 1,
      dataKey: "noData"
    };
  }
  const _apiData = [...apiData].slice(0, maxRecords);
  const stackSelected = filters.stacks && filters.stacks.length > 0;
  const key = "count";
  const totalCount = (_apiData || []).reduce((acc: any, next: any) => {
    return acc + next[key];
  }, 0);

  const mappedData = (_apiData || []).map((item: any, index: number) => {
    return {
      ...item,
      // title: {
      //   label: stackSelected ? "Date" : "Job",
      //   value: stackSelected ? moment.unix(parseInt(item.key)).format("MM/DD") : item.key
      // },
      subTitle: {
        label: stackSelected ? "Date" : "Job",
        value: stackSelected ? moment.unix(parseInt(item.key)).format("MM/DD") : item.key
      },
      color: randomColor({ hue: colorArray[index % colorArray.length] })
    };
  });

  data = {
    data: mappedData,
    total: totalCount,
    dataKey: key
  };

  return data;
};

export const cicdSCMJobCountTransformer = (data: any) => {
  const { filters, apiData, records, reportType } = data;
  let jobs_count_data = [];

  if (filters.stacks !== "qualified_job_name" && apiData && apiData.length > 0) {
    jobs_count_data = apiData.map((item: any) => {
      const { key, count, stacks, additional_key } = item;
      let stackedData = {};
      if (stacks) {
        stackedData = getStacksData(stacks, filters, reportType, "count");
      }

      const result: any = {
        count,
        name: key,
        ...stackedData
      };

      if (additional_key) {
        result.additional_key = additional_key;
      }

      return result;
    });
  }

  if (filters.stacks === "qualified_job_name" && apiData && apiData.length > 0) {
    const uniqKeys = uniq(apiData.reduce((acc: any, next: any) => [...acc, next.key], []));
    uniqKeys.forEach(key => {
      const items = apiData.filter((f: any) => f.key === key);

      const combinedItem = items.reduce(
        (acc: any, next: any) => {
          return {
            ...acc,
            count: acc.count + next.count,
            ...getStacksData(next.stacks, filters, reportType, "count")
          };
        },
        { name: key, count: 0 }
      );

      jobs_count_data.push(combinedItem);
    });
  }
  const max = Math.min(records || DEFAULT_MAX_RECORDS, jobs_count_data.length);

  return { data: jobs_count_data.slice(0, max) };
};

export const jobsChangeVolumeTransform = (data: any) => {
  const { reportType, widgetFilters, records, timeFilterKeys } = data;
  let { apiData } = data;
  const across = get(widgetFilters, ["across"], undefined);
  const interval = get(widgetFilters, ["interval"], "day");
  if (apiData && apiData.length) {
    apiData = apiData.map((item: any) => {
      const { key, ...data } = item;
      let name = item.additional_key;
      const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
      name = xAxisLabelTransform?.({ item, interval, across }) || name;
      return {
        name,
        key: item.key,
        lines_added_count: data.total_lines_added || 0,
        files_changed_count: data.total_files_changed || 0,
        lines_removed_count: data.total_lines_removed * -1 || 0
      };
    });

    const start = apiData.length > records ? apiData.length - records : 0;
    const end = apiData.length;
    apiData = apiData.slice(start, end);
  }

  const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
  const sortKey = get(widgetFilters, ["sort", 0, "id"], "");

  if (allowedWidgetDataSorting && timeFilterKeys.includes(sortKey)) {
    const sortValue = get(widgetFilters, ["filter", "sort_xaxis"], "");
    sortValue.includes("old-latest") && apiData && apiData.reverse();
  }

  return {
    data: apiData
  };
};

export const praetorianIssuesReportTransform = (data: any) => ({
  data: genericTicketsReportTransformer(data, "count")
});

export const nccGroupIssuesReportTransform = (data: any) => ({
  data: genericTicketsReportTransformer(data, "count")
});

export const snykIssuesReportTransform = (data: any) => {
  const { apiData, reportType, widgetFilters } = data;

  const across = get(widgetFilters, ["across"], undefined);

  if (across && ["trend"].includes(across)) {
    return trendReportTransformer({ apiData, reportType });
  }
  return { data: genericTicketsReportTransformer(data, "total") };
};

export const azureTicketsReportChangeTransform = (data: any) => {
  const { filters, widgetFilters, reportType } = data;
  const { across } = filters;
  let _data = [];
  const metrics = get(widgetFilters, ["filter", "metric"], "ticket");
  const interval = get(widgetFilters, ["interval"], undefined);
  const isDonut =
    get(filters, ["visualization"], IssueVisualizationTypes.BAR_CHART) === IssueVisualizationTypes.DONUT_CHART;

  const newApiData = get(data, ["apiData", "0", across, "records"], []);

  let valueKey = METRIC_VALUE_KEY_MAPPING[metrics || ""];

  if (Array.isArray(metrics)) {
    valueKey = METRIC_VALUE_KEY_MAPPING[metrics[0] || ""];
  }

  if (isDonut) {
    _data = newApiData
      ?.map((item: any) => {
        let name = item.key;
        const xAxisLabelTransform = getWidgetConstant(reportType, ["xAxisLabelTransform"]);
        name = xAxisLabelTransform?.({ item, interval, across }) || name;
        return {
          name: name,
          value: item[valueKey],
          key: item.key
        };
      })
      .sort((a: any, b: any) => b.value - a.value);
  } else {
    _data = genericTicketsReportTransformer(
      { ...data, apiData: newApiData },
      valueKey || METRIC_VALUE_KEY_MAPPING.ticket,
      ""
    );
  }
  return { data: _data, all_data: newApiData };
};

export const azureBacklogTransformerWrapper = (data: any) => {
  const { widgetFilters, metadata, reportType, timeFilterKeys } = data;
  const { across } = widgetFilters;
  const newApiData = get(data, ["apiData", "0", across, "records"], []);
  const graphType = get(metadata, ["graphType"], "bar_chart");
  let seriesData = newApiData;
  if (newApiData) {
    switch (graphType) {
      case "bar_chart":
        seriesData = backlogSeriesDataTransformer({ ...data, apiData: newApiData });
        break;
      case "line_chart":
        seriesData = backlogTrendTransformer({ ...data, apiData: newApiData });
        break;
    }
  }

  let mappedData = seriesData?.data || [];

  const allowedWidgetDataSorting = get(widgetConstants, [reportType, ALLOWED_WIDGET_DATA_SORTING], false);
  const sortKey = get(widgetFilters, ["sort", 0, "id"], "");

  if (allowedWidgetDataSorting && timeFilterKeys.includes(sortKey)) {
    const sortValue = get(widgetFilters, ["filter", "sort_xaxis"], "");
    sortValue.includes("old-latest") && mappedData && mappedData.reverse();
  }

  return {
    data: mappedData || []
  };
};

export const fixedNDecimalPlaces = (value: any, places: number) => {
  return value.toFixed(places);
};

export const widgetFilterPreviewTransformer = (filters: any) => {
  return filters.map((filter: any) =>
    filter.label === "X-Axis" && filter.value === "questionnaire_template_id"
      ? (filter = { ...filter, value: "assessment" })
      : filter
  );
};
