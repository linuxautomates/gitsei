import {
  DEFAULT_CATEGORY_NAME,
  UNCATEGORIZED_ID_SUFFIX
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import {
  INTERVAL_OPTIONS,
  TIME_RANGE_DISPLAY_FORMAT_CONFIG
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { IntervalType } from "dashboard/constants/enums/jira-ba-reports.enum";
import {
  BARangeConfigType,
  BASubRangeModifcationType,
  effortInvestmentTrendInitialDataType,
  UncategorizedNames
} from "dashboard/dashboard-types/BAReports.types";
import { basicMappingType, optionType } from "dashboard/dashboard-types/common-types";
import { capitalize, forEach, get, map, parseInt, uniq, uniqBy } from "lodash";
import moment from "moment";
import { getDecimalPercantageValue, getPercantageValue } from "shared-resources/charts/graph-stat-chart/helper";
import { NO_SPRINT_DATA } from "shared-resources/charts/jira-effort-allocation-chart/helper";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";

// it maps FE specified keys to BE received keys, inorder to get data value from data object
const mappingKeyToDatakey: any = {
  team_allocations: "assignees",
  priority: "priority_order",
  remaining: "total_tickets",
  new: "total_tickets",
  completed: "total_tickets"
};

// aggregation function for jira burndown report
const aggregatedRecordsInRange = (low: number, high: number, records: any[], dataKey: string) => {
  return records.reduce((acc, cur: any) => {
    const value = parseInt(cur?.key || "");
    const timestamp = isNaN(value) ? moment.now() : value;
    if (timestamp >= low && timestamp <= high) {
      acc = acc + get(cur, [mappingKeyToDatakey[dataKey]], 0);
    }
    return acc;
  }, 0);
};

// it seperates records on the basis of sprint range
const getReducedSprintList = (
  timeRangeList: any[],
  dataObj: any,
  aggregationFunction: (low: number, high: number, records: any[], dataKey: string) => any // it's  a function for aggregating data based upon sprint
) => {
  const resultantSprints = (timeRangeList || []).map(timeRange => {
    const low = parseInt(timeRange?.start_date);
    const high = parseInt(timeRange?.end_date);
    let timeRangeExtra: any = {};
    forEach(Object.keys(dataObj), key => {
      const records = get(dataObj, [key], []);
      const aggregatedData = aggregationFunction(low, high, records, key);
      timeRangeExtra = {
        ...timeRangeExtra,
        [key]: aggregatedData
      };
    });
    return { ...timeRange, ...timeRangeExtra };
  });

  return resultantSprints;
};

// function for finding team allocation trend with respect to previous sprint for epic priority report
const getSprintTeamAllocationTrend = (sprints: any[]) => {
  let adjTeamAllocationDiffList: number[] = [0];

  // creating a array of differences among adjacent entries
  for (let index = 1; index < (sprints || []).length; index++) {
    const diff =
      ((sprints?.[index] || {}).team_allocations || []).length -
      ((sprints?.[index - 1] || {}).team_allocations || []).length;
    adjTeamAllocationDiffList[index] = diff;
  }
  for (let index = 0; index < (sprints || []).length; index++) {
    const curSprint = sprints[index];
    curSprint["teamAllocationTrend"] = adjTeamAllocationDiffList[index];
  }
};

// aggregation function for epic priority report
const epicPriorityAggregationFunction = (low: number, high: number, records: any[], dataKey: string) => {
  return records.reduce(
    (acc, cur: any) => {
      const value = parseInt(cur?.key || "");
      const timestamp = isNaN(value) ? moment().unix() : value;
      if (timestamp >= low && timestamp <= high) {
        const value = get(cur, [mappingKeyToDatakey[dataKey]], undefined);
        if (value) {
          if (Array.isArray(value)) {
            acc.push(...value);
            acc = uniq(acc);
          } else {
            acc = Math.min(acc, value);
          }
        }
      }
      return acc;
    },
    dataKey === "team_allocations" ? [] : Number.MAX_VALUE
  );
};

export const jiraProgressReportTransformer = (
  completed_story_point_records: any[],
  initialTransformedDataRecords: any[],
  unit: "tickets_report" | "story_point_report" // it specifies which uri has been called and is used to decide dataKey
) => {
  let result = initialTransformedDataRecords.map((record: any) => {
    const id = record?.id;
    const completedSimilarStoryPoint = completed_story_point_records.find((record: any) => record?.key === id);
    let completedStoryPointPercent = 0;
    if (completedSimilarStoryPoint) {
      const pointsKey = unit === "tickets_report" ? "total_tickets" : "total_story_points";
      const numerator = parseInt(get(completedSimilarStoryPoint, [pointsKey], 0));
      const denominator = parseInt(get(record, [pointsKey], 1));
      const percentage = getPercantageValue(numerator, denominator);
      completedStoryPointPercent = percentage === "NaN" ? 0 : percentage;
    }
    return {
      ...(record || []),
      completed_percent_story_point: completedStoryPointPercent
    };
  });
  result.sort((a, b) => a?.priority_order - b?.priority_order);
  return result;
};

export const jiraEpicPriorityDataTransformer = (timeRangeList: any[], initialTransformedDataRecords: any[]) => {
  let finalTransformedData: any[] = [];
  forEach(initialTransformedDataRecords, (record: any) => {
    let dataObj: any = { name: Object.keys(record)[0], summary: record?.summary };
    const sprints = getReducedSprintList(timeRangeList, Object.values(record)[0], epicPriorityAggregationFunction);
    getSprintTeamAllocationTrend(sprints);
    dataObj = {
      ...dataObj,
      sprints
    };
    finalTransformedData.push(dataObj);
  });
  return finalTransformedData;
};

export const jiraBurnDownDataTransformer = (
  initialTransformedDataRecords: any[],
  sprintList: any[],
  categoryList?: any[]
) => {
  let finalTransformedData: any[] = [];
  forEach(initialTransformedDataRecords, (record: any) => {
    const key = Object.keys(record)[0] || "";
    let dataObj: any = { name: key, id: key, summary: record?.summary };
    if (categoryList) {
      // for categories key is id but we need name so finding name of respective category
      const category = categoryList.find(category => category?.id === key);
      if (category) {
        dataObj = {
          name: category?.name,
          id: category?.id
        };
      }
    }
    sprintList = (sprintList || []).map((sprint, index) => ({
      ...(sprint || {}),
      initial: `${capitalize(sprint?.name).substring(0, 1)} ${index + 1}`
    }));
    const sprints = getReducedSprintList(sprintList, Object.values(record)[0], aggregatedRecordsInRange);
    const totalAccumulation = sprints.reduce(
      (acc, curSprint: any) => {
        return {
          total_tickets_completed: acc.total_tickets_completed + get(curSprint, ["completed"], 0),
          total_tickets_remaining: acc.total_tickets_remaining + get(curSprint, ["remaining"], 0),
          total_tickets_new: acc.total_tickets_new + get(curSprint, ["new"], 0)
        };
      },
      {
        total_tickets_completed: 0,
        total_tickets_remaining: 0,
        total_tickets_new: 0
      }
    );
    dataObj = {
      ...dataObj,
      sprints,
      ...totalAccumulation
    };
    finalTransformedData.push(dataObj);
  });
  return {
    records: finalTransformedData,
    dataKeys: ["completed", "remaining", "new"]
  };
};

export const intervalMapping: { [x: string]: string } = {
  week: "week",
  biweekly: "week",
  month: "month",
  quarter: "quarter"
};

const convertNumericValueToPercentage = (
  dataObj: { [x: string]: number },
  unCategorizedKey = DEFAULT_CATEGORY_NAME
) => {
  let totalSum = 0;
  const keyMapping = {
    Other: unCategorizedKey
  };

  forEach(Object.values(dataObj), (value: number) => {
    totalSum += value;
  });

  let newDataObj: { [x: string]: number } = {};

  forEach(Object.keys(dataObj) || [], key => {
    const filterKey = get(keyMapping, [key], key);
    const percentageValue = getDecimalPercantageValue(dataObj?.[key] || 0, totalSum);
    newDataObj[filterKey] = percentageValue === "NaN" ? 0.0 : percentageValue;
  });

  return newDataObj;
};

const orderByLastData = (dataList: { [x: string]: number }[]) => {
  let listSize = (dataList || []).length;
  if (!listSize) return [];
  const lastDataObject = dataList[listSize - 1] || {};
  const lastDataObjectKeys = Object.keys(lastDataObject);
  (lastDataObjectKeys || []).sort(
    (key1: string, key2: string) => (lastDataObject[key2] || 0) - (lastDataObject[key1] || 0)
  );

  return map(dataList || [], data => {
    let newDataObject: { [x: string]: number } = {};
    forEach(lastDataObjectKeys || [], key => {
      newDataObject = {
        ...newDataObject,
        [key]: data[key]
      };
    });
    return newDataObject;
  });
};

const getLimitingRangeLowValue = (date: number, interval: string) => {
  if (interval === IntervalType.WEEK) {
    return Math.min(
      date,
      moment
        .unix(date)
        .utc()
        .startOf(interval as any)
        .add(1, "d")
        .unix()
    );
  }
  return Math.min(
    date,
    moment
      .unix(date)
      .utc()
      .startOf(interval as any)
      .unix()
  );
};

const effortInvestmentTrendUnit = (unit: string, effortInvestmentTrendYaxis: boolean) => {
  if (["story_point_report", "azure_effort_investment_story_point"].includes(unit)) {
    return "Story Point %";
  }

  if (["scm_jira_commits_count_ba", "azure_effort_investment_commit_count"].includes(unit)) {
    return "Commit Count %";
  }

  return effortInvestmentTrendYaxis ? "FTE %" : "Ticket %";
};

export const jiraEffortInvestmentTrendReportTransformer = (
  initialTransformedDataRecords: effortInvestmentTrendInitialDataType[],
  sprintList: BARangeConfigType[],
  unit: string,
  interval: string,
  reportType: string,
  effortInvestmentTrendYaxis: boolean
) => {
  let finalTransformedData: any[] = [];
  const dataKey = "fte";
  const unitToShow = effortInvestmentTrendUnit(unit, effortInvestmentTrendYaxis);
  const timeRangeLength = (sprintList || []).length;
  let low = sprintList[0]?.start_date;
  let high = sprintList[timeRangeLength - 1]?.start_date;

  let startTime = low;
  let dates: number[] = [];
  let modificationData: BASubRangeModifcationType = { offset: intervalMapping[interval], factor: 1 };
  if (interval === IntervalType.BI_WEEK) {
    modificationData.factor = 2;
  }

  while (startTime <= high) {
    dates.push(startTime);
    startTime = moment
      .unix(startTime)
      .utc()
      .add(modificationData.factor as number, modificationData.offset as any)
      .unix();
  }

  let categoriesAggregatedDataList: { [x: string]: number }[] = [];
  const intervalDisplayFormatMapping = getWidgetConstant(reportType, TIME_RANGE_DISPLAY_FORMAT_CONFIG);
  let uncategorizedCategoryKey = DEFAULT_CATEGORY_NAME;

  const intervalOptions: optionType[] = getWidgetConstant(reportType, INTERVAL_OPTIONS, []);
  const intervalAssociatedOption = intervalOptions.find(option => option.value === interval);
  const now = moment().utc().endOf("d").unix();

  forEach(dates, (date, index: number) => {
    if (index < (dates || []).length - 1) {
      low = date;
      high = dates[index + 1];
    } else {
      low = date;
      high = Math.min(
        now,
        moment
          .unix(date)
          .utc()
          .add(modificationData.factor as number, modificationData.offset as any)
          .unix()
      );
    }

    let categoriesDataObj: any = {};
    forEach(initialTransformedDataRecords || [], record => {
      if ((record?.id || "").includes(UNCATEGORIZED_ID_SUFFIX) && record?.name) {
        uncategorizedCategoryKey = record?.name;
      }
      const completedRecords = record?.completed_points || [];
      let sum = 0;
      forEach(completedRecords || [], dataRecords => {
        if (dataRecords?.key >= low && dataRecords?.key < high) {
          sum += dataRecords?.[dataKey] || 0;
        }
      });
      categoriesDataObj[record?.name || ""] = sum;
    });
    categoriesAggregatedDataList.push(categoriesDataObj);

    const rangeLowFormatted = moment.unix(date).utc().format(intervalDisplayFormatMapping[interval]);
    const rangeHighFormatted = moment
      .unix(high !== now ? high - 1 : high)
      .utc()
      .format(intervalDisplayFormatMapping[interval]);

    const xaxis = interval !== IntervalType.MONTH ? `${rangeLowFormatted} - ${rangeHighFormatted}` : rangeLowFormatted;

    finalTransformedData.push({
      name: xaxis,
      start_date: rangeLowFormatted,
      end_date: rangeHighFormatted,
      selected_interval: intervalAssociatedOption?.label || interval
    });
  });

  forEach(categoriesAggregatedDataList || [], (categoryData, index: number) => {
    categoriesAggregatedDataList[index] = convertNumericValueToPercentage(categoryData, uncategorizedCategoryKey);
  });

  const orderedCategoryDataList = orderByLastData(categoriesAggregatedDataList) || [];
  finalTransformedData = finalTransformedData.map((data, index) => ({
    ...data,
    ...(orderedCategoryDataList[index] || {})
  }));

  // aggregating all categories
  let allCategories: string[] = uniq(
    (initialTransformedDataRecords || []).reduce((acc: string[], data: effortInvestmentTrendInitialDataType) => {
      let key = data?.name ?? "";
      if (key === UncategorizedNames.OTHER) {
        key = uncategorizedCategoryKey;
      }
      acc.push(key);
      return acc;
    }, [])
  );

  const barProps = (allCategories ?? []).map(id => {
    return {
      name: id,
      dataKey: id,
      barSize: 70
    };
  });

  /** color mapping between category and it's corresponding color defined */
  let categoryColorMapping: basicMappingType<string> = {};
  forEach(initialTransformedDataRecords, (record, index) => {
    categoryColorMapping[`${record?.name ?? index}`] = record?.color ?? "";
  });

  return {
    records: finalTransformedData,
    barProps,
    unit: unitToShow,
    showXAxisTooltip: interval !== IntervalType.MONTH,
    digitsAfterDecimal: 1,
    categories: initialTransformedDataRecords,
    categoryColorMapping
  };
};

export const effortInvestmentTeamReportTransformer = (
  initialTransformedDataRecords: any[], // sprints with there apidata
  teamsList: any[], // teams
  assigneeTeamMap: any, // it gives assignees vs list of teams in which that assignee is present
  unit: "tickets_report" | "story_point_report"
) => {
  const dataKey = unit === "story_point_report" ? "total_story_points" : "total_tickets";
  const assignees = Object.keys(assigneeTeamMap);

  let assigneeTimeRangeList: any[] = map(assignees, assignee => {
    let dataObj: any = { name: assignee };
    let curDependencyList: string[] = [];
    const sprints = map(initialTransformedDataRecords, timeRange => {
      const apiRecords = get(timeRange, ["apiData"], []);
      const matchingRecords = apiRecords.filter((record: any) => record?.key === assignee);
      let categorySumObj: any = {};
      if (matchingRecords.length > 0) {
        const categoryRecords = get(matchingRecords[0], ["stacks"], []);
        forEach(categoryRecords, categoryRecord => {
          curDependencyList.push(categoryRecord?.key);
          categorySumObj[categoryRecord?.key] =
            get(categorySumObj, [categoryRecord?.key], 0) + categoryRecord?.[dataKey];
        });
      }
      return {
        name: timeRange?.name,
        ...categorySumObj,
        percentage_sprintdata: getPercentageObject(categorySumObj, Object.keys(categorySumObj))
      };
    });
    const average = effortInvestmentTeamAggregationFunction(sprints, uniq(curDependencyList));
    return {
      ...dataObj,
      sprints,
      average
    };
  });
  uniqBy(assigneeTimeRangeList, "name");
  forEach(assigneeTimeRangeList, assigneeRecord => {
    const assignee = assigneeRecord?.name;
    const teamsAssociatedIds: string[] = assigneeTeamMap[assignee];
    forEach(teamsList, team => {
      if (teamsAssociatedIds.includes(team?.id)) {
        team["assignees"] = [...get(team, ["assignees"], []), ...[assigneeRecord]];
      }
    });
  });
  const newTeamList = map(teamsList, team => {
    const assignees = get(team, ["assignees"], []);
    let dependency_ids: string[] = [];
    forEach(assignees, assignee => {
      const sprints = get(assignee, ["sprints"], []);
      forEach(sprints, sprint => {
        const keys = Object.keys(sprint?.percentage_sprintdata || {});
        dependency_ids = [...dependency_ids, ...keys];
      });
    });
    return {
      ...team,
      dependency_ids: uniq(dependency_ids)
    };
  });

  return {
    records: newTeamList,
    unit: unit === "story_point_report" ? "Total Points" : "Total Tickets"
  };
};

const effortInvestmentTeamAggregationFunction = (timeRanges: any[], dependencyList: string[]) => {
  let sumObj: any = {};
  forEach(dependencyList, dependency => {
    sumObj = {
      ...sumObj,
      [dependency]: 0
    };
  });
  forEach(dependencyList, (dependency, index) => {
    forEach(timeRanges, timeRange => {
      sumObj[dependency] += (timeRange || [])[dependency] || 0;
    });
    if ((timeRanges || []).length > 0) {
      sumObj[dependency] = Math.floor(sumObj[dependency] / timeRanges.length);
    }
  });
  const percentageAvg = getPercentageObject(sumObj, dependencyList);
  return {
    ...sumObj,
    percentage_avg: percentageAvg
  };
};

const getPercentageObject = (sumObj: { [x: string]: number }, dependencyList: string[]) => {
  const maxValue = Math.max(...(Object.values(sumObj) || [0]));
  if (maxValue === 0) {
    return {};
  }
  let totalSum = 0;
  forEach(Object.values(sumObj), (value: number) => {
    totalSum += value;
  });
  let percentageObj: any = {};
  let totalPercentExceptLast = 0;
  forEach(dependencyList, (key, index) => {
    if (index + 1 !== dependencyList.length) {
      const percent = getPercantageValue(sumObj[key], totalSum);
      percentageObj[key] = percent === "NaN" ? 0 : percent;
      totalPercentExceptLast += percentageObj[key];
    } else {
      percentageObj[key] = 100 - totalPercentExceptLast;
    }
  });
  if (Object.keys(percentageObj).length === 0) {
    return { [NO_SPRINT_DATA]: 100 };
  }
  return percentageObj;
};
