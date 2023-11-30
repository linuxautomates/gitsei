import { IntervalType } from "dashboard/constants/enums/jira-ba-reports.enum";
import {
  BARangeConfigType,
  effortInvestmentTrendInitialDataType,
  EITrendReportTimeRangeListFuncType,
  RangeType
} from "dashboard/dashboard-types/BAReports.types";
import { capitalize, cloneDeep, forEach, get, uniq } from "lodash";
import moment, { Moment } from "moment";
import { v1 as uuid } from "uuid";

export const getMinTickList = (
  timerangeList: BARangeConfigType[],
  selectedTimeRange: RangeType,
  timeRangeDisplayFormat = "MM/DD"
) => {
  const mappedTimeRange = getMappedSelectedTimeRange(selectedTimeRange);
  const mappedUnit = timeRangeToUnitMap[mappedTimeRange || ""];
  const clonedTimeRangeList = cloneDeep(timerangeList);
  switch (mappedUnit) {
    case "week":
      return (timerangeList || []).map(range =>
        moment.unix(range?.end_date).clone().utc().format(timeRangeDisplayFormat)
      );
    case "month":
      return (clonedTimeRangeList || []).map(range => {
        const diff = (range?.end_date || 0) - (range?.start_date || 0);
        const numOfDays = Math.round(diff / 86400) - 1;
        const midDay = Math.floor(numOfDays / 2);
        return moment.unix(range.start_date).utc().add(midDay, "d").format(timeRangeDisplayFormat);
      });
    case "quarter":
      let resultantList: string[] = [];
      (clonedTimeRangeList || []).forEach(range => {
        let start = range?.start_date;
        for (let i = 0; i < 3; i++) {
          const monthEnd = moment.unix(start).clone().utc().endOf("month").format(timeRangeDisplayFormat);
          resultantList.push(monthEnd);
          start = moment.unix(start).clone().utc().add(1, "month").unix();
        }
      });

      return resultantList;
  }
};

export const getSprintFilters = (filters: any) => {
  const keys = ["completed_at", "product_id", "integration_ids"];
  let newFilter: any = {};
  forEach(keys, key => {
    newFilter = {
      ...newFilter,
      [key]: get(filters, ["filter", key], "")
    };
  });
  return {
    ...filters,
    filter: newFilter
  };
};

// function to get range in $lt and $gt depending upon specific calculations
const getTimeRangeForBASprintReports = (
  startingMoment: Moment,
  interval: number,
  unit: "week" | "month" | "quarter",
  timeRangeDisplayFormat = "DD/MMM/YYYY"
) => {
  let $lt: Moment | number = moment.now(),
    $gt: Moment | number = moment.now();

  switch (unit) {
    case "week":
      $lt = startingMoment.clone().add(interval, unit);
      $gt = startingMoment.clone();
      return {
        formattedStartDate: $gt.format(timeRangeDisplayFormat),
        formattedEndDate: $lt.format(timeRangeDisplayFormat),
        $lt: $lt.unix(),
        $gt: $gt.unix()
      };
    case "month":
      $lt = startingMoment.clone().endOf(unit);
      $gt = startingMoment.clone();
      return {
        formattedStartDate: $gt.format(timeRangeDisplayFormat),
        formattedEndDate: $lt.format(timeRangeDisplayFormat),
        $lt: $lt.unix(),
        $gt: $gt.unix()
      };
    case "quarter":
      $lt = startingMoment.clone().endOf(unit);
      $gt = startingMoment.clone();
      return {
        formattedStartDate: $gt.format(timeRangeDisplayFormat),
        formattedEndDate: $lt.format(timeRangeDisplayFormat),
        $lt: $lt.unix(),
        $gt: $gt.unix()
      };
    default:
      return { $lt: moment().unix(), $gt: moment().unix() };
  }
};

// last time period
const getCurrentTimePeriodRange = (
  startingMoment: Moment,
  unit: "week" | "month" | "quarter",
  timeRangeDisplayFormat = "DD/MMM/YYYY"
) => {
  let $lt: Moment | number = moment.now(),
    $gt: Moment | number = moment.now();

  switch (unit) {
    case "week":
      $lt = moment().utc().startOf("d");
      $gt = startingMoment.clone().add(1, "d");
      return {
        formattedStartDate: $gt.format(timeRangeDisplayFormat),
        formattedEndDate: $lt.format(timeRangeDisplayFormat),
        $lt: $lt.unix(),
        $gt: $gt.unix()
      };
    case "month":
      $lt = moment().utc().startOf("d");
      $gt = startingMoment.clone();
      return {
        formattedStartDate: $gt.format(timeRangeDisplayFormat),
        formattedEndDate: $lt.format(timeRangeDisplayFormat),
        $lt: $lt.unix(),
        $gt: $gt.unix()
      };
    case "quarter":
      $lt = moment().utc().startOf("d");
      $gt = startingMoment.clone();
      return {
        formattedStartDate: $gt.format(timeRangeDisplayFormat),
        formattedEndDate: $lt.format(timeRangeDisplayFormat),
        $lt: $lt.unix(),
        $gt: $gt.unix()
      };
    default:
      return { $lt: moment().unix(), $gt: moment().unix() };
  }
};

// this function returns array of objects time ranges
const getListOfTimeRangesForBASprintReports = (
  startingMoment: Moment, // Starting moment of either week, month, year
  interval: number, // denotes interval between ranges
  unit: "week" | "month" | "quarter", // unit for calculation
  totalRanges: number,
  timeRangeDisplayFormat?: string
) => {
  totalRanges /= interval;
  let timeRangeLists: any[] = [];
  let curMoment = startingMoment.clone();
  for (let index = 0; index < totalRanges; index++) {
    let rangeObj: any = {};
    if (index !== totalRanges - 1) {
      const { $lt, $gt, formattedEndDate, formattedStartDate } = getTimeRangeForBASprintReports(
        curMoment,
        interval,
        unit,
        timeRangeDisplayFormat
      );
      rangeObj["start_date"] = $gt;
      rangeObj["end_date"] = $lt;
      rangeObj["name"] = `${capitalize(unit)} (${formattedStartDate} - ${formattedEndDate})`;
      rangeObj["id"] = uuid();
    } else {
      const { $lt, $gt, formattedEndDate, formattedStartDate } = getCurrentTimePeriodRange(
        curMoment,
        unit,
        timeRangeDisplayFormat
      );
      rangeObj["start_date"] = $gt;
      rangeObj["end_date"] = $lt;
      rangeObj["name"] = `${capitalize(unit)} (${formattedStartDate} - ${formattedEndDate})`;
      rangeObj["id"] = uuid();
    }
    curMoment = curMoment.add(interval, unit);
    timeRangeLists.push(rangeObj);
  }
  return timeRangeLists;
};

enum BATimeRangeDifferenceType {
  WEEK = 7,
  BI_WEEK = 14,
  MONTH = 30,
  QUATER = 120
}

const timeRangedifferenceMapping = {
  [BATimeRangeDifferenceType.WEEK]: "last_4_week",
  [BATimeRangeDifferenceType.MONTH]: "last_4_month",
  [BATimeRangeDifferenceType.BI_WEEK]: "last_8_week",
  [BATimeRangeDifferenceType.QUATER]: "last_4_quarter"
};

export const timeRangeToUnitMap: { [x: string]: string } = {
  last_4_week: "week",
  last_8_week: "week",
  last_4_month: "month",
  last_4_quarter: "quarter"
};

export const getMappedSelectedTimeRange = (selectedTimeRange: { $lt: string; $gt: string }) => {
  const diff = parseInt(selectedTimeRange.$lt) - parseInt(selectedTimeRange.$gt);
  const numOfDays = Math.round(diff / 86400);
  return timeRangedifferenceMapping[numOfDays as BATimeRangeDifferenceType];
};

export const getBATimeRanges = (
  selectedTimeRange: { $lt: string; $gt: string } | string,
  timeRangeDisplayFormat?: string
) => {
  const curTimeRange =
    typeof selectedTimeRange === "string" ? selectedTimeRange : getMappedSelectedTimeRange(selectedTimeRange);
  switch (curTimeRange) {
    case "last_4_week":
      const momentAtTheStartBeforeFourWeeks = moment().utc().startOf("week").subtract(3, "week"); // moment of sunday 4 weeks before
      return getListOfTimeRangesForBASprintReports(
        momentAtTheStartBeforeFourWeeks,
        1,
        "week",
        4,
        timeRangeDisplayFormat
      );
    case "last_4_month":
      const momentAtTheStartBeforeFourMonths = moment().utc().startOf("month").subtract(3, "month"); // moment of 1st day of month, 4 months before
      return getListOfTimeRangesForBASprintReports(
        momentAtTheStartBeforeFourMonths,
        1,
        "month",
        4,
        timeRangeDisplayFormat
      );
    case "last_8_week":
      const momentAtTheStartBeforeEightWeeks = moment().utc().startOf("week").subtract(8, "week"); // moment of sunday 8 weeks before
      return getListOfTimeRangesForBASprintReports(
        momentAtTheStartBeforeEightWeeks,
        2,
        "week",
        8,
        timeRangeDisplayFormat
      );
    case "last_4_quarter":
      const momentAtTheStartBeforeFourquarter = moment().utc().startOf("Q").subtract(3, "Q"); // moment of 1st day of month 4 quater before
      return getListOfTimeRangesForBASprintReports(
        momentAtTheStartBeforeFourquarter,
        1,
        "quarter",
        4,
        timeRangeDisplayFormat
      );
  }
};

// use this for extracting records from apidata
export const getAzureReportApiRecords = (records: any[]) => {
  return records.length ? get(Object.values(records[0])[0] || {}, ["records"], []) : [];
};

export const getEITrendReportTimeRangeList: EITrendReportTimeRangeListFuncType = (
  categoryWiseRecords: effortInvestmentTrendInitialDataType[],
  interval: IntervalType.WEEK | IntervalType.BI_WEEK | IntervalType.MONTH | IntervalType.QUARTER,
  timeRangeDisplayFormat: string
) => {
  let timeRangeLists: BARangeConfigType[] = [];
  let timeStampList: Array<number> = (categoryWiseRecords ?? []).reduce((acc, obj) => {
    const timeStamps: Array<number> = (obj.completed_points ?? []).map(curObj => parseInt(curObj?.key ?? ""));
    acc.push(...(timeStamps ?? []));
    return acc;
  }, [] as Array<number>);
  timeStampList = uniq(timeStampList ?? []);
  timeStampList.sort();
  let rangeObj: BARangeConfigType;
  const offset = interval === IntervalType.BI_WEEK ? 2 : 1;
  forEach(timeStampList ?? [], timeStamp => {
    rangeObj = {} as BARangeConfigType;
    let curMoment = moment.unix(timeStamp).utc().clone();
    const { $lt, $gt, formattedEndDate, formattedStartDate } = getTimeRangeForBASprintReports(
      curMoment,
      offset,
      interval === IntervalType.BI_WEEK ? IntervalType.WEEK : interval,
      timeRangeDisplayFormat
    );
    rangeObj["start_date"] = $gt;
    rangeObj["end_date"] = $lt;
    rangeObj["name"] = `${capitalize(interval)} (${formattedStartDate} - ${formattedEndDate})`;
    rangeObj["id"] = uuid();
    curMoment = curMoment.add(offset, interval === IntervalType.BI_WEEK ? IntervalType.WEEK : interval);
    timeRangeLists.push(rangeObj);
  });
  return timeRangeLists;
};
