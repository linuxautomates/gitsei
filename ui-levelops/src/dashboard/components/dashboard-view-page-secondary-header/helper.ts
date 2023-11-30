import { time } from "console";
import { capitalize } from "lodash";
import moment, { Moment } from "moment";
import { timeInterval } from "../../constants/devProductivity.constant";

export const getDashboardTimeRangeOptionTitle = (id: string) => {
  return id
    ? id
        .split("_")
        .map(key => capitalize(key))
        .join(" ")
    : "";
};

export const dateRangeFilterValue = (dateRange: any): any => {
  if (typeof dateRange !== "string") {
    return dateRange;
  }

  const currentDayOffset = moment(moment.utc().subtract("days", 1).format("YYYY-MM-DD")).endOf("day").unix();

  switch (dateRange.toLowerCase()) {
    case timeInterval.LAST_WEEK.toLowerCase():
      return {
        $gt: moment.utc().subtract("weeks", 1).startOf("isoWeek").unix(),
        $lt: moment.utc().subtract("weeks", 1).endOf("isoWeek").unix()
      };
    case timeInterval.LAST_TWO_MONTHS.toLowerCase():
      return {
        $gt: moment.utc().subtract("months", 2).startOf("month").unix(),
        $lt: moment.utc().subtract("months", 1).endOf("month").unix()
      };
    case timeInterval.LAST_7_DAYS.toLowerCase():
      return {
        $gt: moment.utc().subtract("days", 7).startOf("day").unix(),
        $lt: currentDayOffset
      };
    case timeInterval.LAST_TWO_WEEKS.toLowerCase():
    case timeInterval.LAST_2_WEEKS.toLowerCase():
      return {
        $gt: moment.utc().subtract("days", 14).startOf("day").unix(),
        $lt: currentDayOffset
      };
    case timeInterval.LAST_30_DAYS.toLowerCase():
      return {
        $gt: moment.utc().subtract("days", 30).startOf("day").unix(),
        $lt: currentDayOffset
      };
    case timeInterval.LAST_MONTH.toLowerCase():
      const lastMonth = moment().utc().subtract("months", 1);
      return {
        $gt: lastMonth.startOf("month").unix(),
        $lt: lastMonth.endOf("month").unix()
      };
    case timeInterval.LAST_THREE_MONTHS.toLowerCase():
    case timeInterval.LAST_3_MONTH.toLowerCase():
      const last = moment().utc().subtract("months", 3);
      const last1month = moment().utc().subtract("months", 1);
      return {
        $gt: last.startOf("month").unix(),
        $lt: last1month.endOf("month").unix()
      };
    case timeInterval.LAST_QUARTER.toLowerCase():
      const quarter = moment().utc().subtract("quarter", 1);
      return {
        $gt: quarter.startOf("quarter").unix(),
        $lt: quarter.endOf("quarter").unix()
      };
    case timeInterval.LAST_2_QUARTER.toLowerCase():
    case timeInterval.LAST_TWO_QUARTERS.toLocaleLowerCase():
      const quarters = moment().utc().subtract("quarter", 1);
      const last2quarter = moment().utc().subtract("quarter", 2);
      return {
        $gt: last2quarter.startOf("quarter").unix(),
        $lt: quarters.endOf("quarter").unix()
      };
    case timeInterval.LAST_12_MONTHS.toLowerCase():
    case timeInterval.LAST_TWELVE_MONTHS.toLocaleLowerCase():
      const last12months = moment().utc().subtract("months", 12);
      const lastmonth = moment().utc().subtract("months", 1);
      return {
        $gt: last12months.startOf("month").unix(),
        $lt: lastmonth.endOf("month").unix()
      };
    case timeInterval.LAST_YEAR.toLowerCase():
      const lastYear = moment().utc().subtract("year", 1);
      return {
        $gt: lastYear.startOf("year").unix(),
        $lt: lastYear.endOf("year").unix()
      };
    default:
      return {
        $gt: moment.utc().subtract("days", 30).startOf("day").unix(),
        $lt: moment.utc().endOf("day").unix()
      };
  }
};

export const getDashboardTimeRangeDate = (dateRange: any, key: string) => {
  return moment.unix(dateRangeFilterValue(dateRange)[key]).utc();
};

export const getDashboardTimeRangeDateValue = (dateRange: any, key: string, format: string = "MMM DD, YYYY") => {
  return getDashboardTimeRangeDate(dateRange, key).format(format);
};

export const timeStampToValue = (key: any, format: string = "MMM DD, YYYY") => {
  return moment.unix(key).utc().format(format);
};

export const getDashboardTimeGtValue = (dashboardTimeRange: any): any =>
  typeof dashboardTimeRange === "string"
    ? getDashboardTimeRangeDate(dashboardTimeRange || "last_quarter", "$gt").unix()
    : dashboardTimeRange?.[`$gt`] || {};

export const getDashboardTimeLtValue = (dashboardTimeRange: any): any =>
  typeof dashboardTimeRange === "string"
    ? getDashboardTimeRangeDate(dashboardTimeRange || "last_quarter", "$lt").unix()
    : dashboardTimeRange?.[`$lt`] || {};

export const dateRangeFilterValueString = (dateRange: string) => {
  if (typeof dateRange !== "string") {
    return dateRange;
  }

  const currentDayOffset = moment(moment.utc().subtract("days", 1).format("YYYY-MM-DD")).endOf("day").unix().toString();
  switch (dateRange.toLowerCase()) {
    case timeInterval.LAST_2_WEEKS.toLowerCase():
      return {
        $gt: moment.utc().subtract("days", 14).startOf("day").unix().toString(),
        $lt: currentDayOffset
      };
    case timeInterval.LAST_30_DAYS.toLowerCase():
      return {
        $gt: moment.utc().subtract("days", 30).startOf("day").unix().toString(),
        $lt: currentDayOffset
      };
    default:
      return {
        $gt: moment.utc().subtract("days", 30).startOf("day").unix().toString(),
        $lt: moment.utc().endOf("day").unix().toString()
      };
  }
};
