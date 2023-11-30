import { TIME_INTERVAL_TYPES } from "constants/time.constants";
import { timeInterval } from "dashboard/constants/devProductivity.constant";

export const intervalToAggIntervalMapping: any = {
  [timeInterval.LAST_WEEK]: TIME_INTERVAL_TYPES.WEEK,
  [timeInterval.LAST_TWO_WEEKS]: TIME_INTERVAL_TYPES.BI_WEEK,
  [timeInterval.LAST_MONTH]: TIME_INTERVAL_TYPES.MONTH,
  [timeInterval.LAST_QUARTER]: TIME_INTERVAL_TYPES.QUARTER,
  [timeInterval.LAST_TWO_QUARTERS]: TIME_INTERVAL_TYPES.TWO_QUARTERS,
  [timeInterval.LAST_TWELVE_MONTHS]: TIME_INTERVAL_TYPES.YEAR
};

export const intervalToTrendStringMap: Record<string, string> = {
  [timeInterval.LAST_WEEK]: "week",
  [timeInterval.LAST_TWO_WEEKS]: "two weeks",
  [timeInterval.LAST_MONTH]: "month",
  [timeInterval.LAST_QUARTER]: "quarters",
  [timeInterval.LAST_TWO_QUARTERS]: "two quarters",
  [timeInterval.LAST_TWELVE_MONTHS]: "year"
};
