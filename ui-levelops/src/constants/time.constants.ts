export enum TIME_INTERVAL_TYPES {
  DAY = "day",
  WEEK = "week",
  BI_WEEK = "biweekly",
  MONTH = "month",
  QUARTER = "quarter",
  YEAR = "year",
  ISOWEEK = "isoWeek",
  DAY_OF_WEEK = "day_of_week",
  HOURS = "hours",
  MINUTES = "minutes",
  TWO_QUARTERS = "two_quarters",
  LAST_WEEK = "last_week",
  LAST_TWO_WEEKS = "last_two_weeks"
}

export enum WEEK_DATE_FORMAT {
  DATE = "date-format",
  NUMBER = "week-number"
}

export const WEEK_FORMAT_CONFIG_OPTIONS = [
  { label: "Date format", value: WEEK_DATE_FORMAT.DATE },
  { label: "Week number", value: WEEK_DATE_FORMAT.NUMBER }
];

export const intervalToMomentKeyMapper = {
  [TIME_INTERVAL_TYPES.MINUTES]: "minutes",
  [TIME_INTERVAL_TYPES.HOURS]: "hours",
  [TIME_INTERVAL_TYPES.DAY]: "days",
  [TIME_INTERVAL_TYPES.WEEK]: "weeks",
  [TIME_INTERVAL_TYPES.BI_WEEK]: "weeks",
  [TIME_INTERVAL_TYPES.MONTH]: "months",
  [TIME_INTERVAL_TYPES.QUARTER]: "quarters",
  [TIME_INTERVAL_TYPES.YEAR]: "years",
  [TIME_INTERVAL_TYPES.TWO_QUARTERS]: "two_quarters"
};
