import { timeInterval, timeIntervalLabel } from "../../../constants/devProductivity.constant";

export const DEV_PRODUCTIVITY_INTERVAL_OPTIONS = [
  { value: timeInterval.LAST_WEEK, label: timeIntervalLabel.LAST_WEEK },
  { value: timeInterval.LAST_TWO_WEEKS, label: timeIntervalLabel.LAST_TWO_WEEKS },
  { value: timeInterval.LAST_MONTH, label: timeIntervalLabel.LAST_MONTH_LABEL },
  { value: timeInterval.LAST_TWO_MONTHS, label: timeIntervalLabel.LAST_TWO_MONTHS },
  { value: timeInterval.LAST_THREE_MONTHS, label: timeIntervalLabel.LAST_THREE_MONTHS },
  { value: timeInterval.LAST_QUARTER, label: timeIntervalLabel.LAST_QUARTER_LABEL },
  { value: timeInterval.LAST_TWO_QUARTERS, label: timeIntervalLabel.LAST_2_QUARTER_LABEL },
  { value: timeInterval.LAST_TWELVE_MONTHS, label: timeIntervalLabel.LAST_12_MONTH_LABEL }
];

export const DEV_PRODUCTIVITY_INTERVAL_OPTIONS_FOR_RAW_STATS = [
  { value: timeInterval.LAST_WEEK, label: timeIntervalLabel.LAST_WEEK },
  { value: timeInterval.LAST_TWO_WEEKS, label: timeIntervalLabel.LAST_TWO_WEEKS },
  { value: timeInterval.LAST_MONTH, label: timeIntervalLabel.LAST_MONTH_LABEL },
  { value: timeInterval.LAST_TWO_MONTHS, label: timeIntervalLabel.LAST_TWO_MONTHS },
  { value: timeInterval.LAST_THREE_MONTHS, label: timeIntervalLabel.LAST_THREE_MONTHS },
  { value: timeInterval.LAST_QUARTER, label: timeIntervalLabel.LAST_QUARTER_LABEL },
  { value: timeInterval.LAST_TWO_QUARTERS, label: timeIntervalLabel.LAST_2_QUARTER_LABEL },
  { value: timeInterval.LAST_TWELVE_MONTHS, label: timeIntervalLabel.LAST_12_MONTH_LABEL }
];

export const OLD_INTERVAL = [
  { value: timeInterval.LAST_MONTH, label: timeIntervalLabel.LAST_MONTH_LABEL },
  { value: timeInterval.LAST_QUARTER, label: timeIntervalLabel.LAST_QUARTER_LABEL },
  { value: timeInterval.LAST_TWO_QUARTERS, label: timeIntervalLabel.LAST_2_QUARTER_LABEL },
  { value: timeInterval.LAST_TWELVE_MONTHS, label: timeIntervalLabel.LAST_12_MONTH_LABEL }
];

export const DEV_PRODUCTIVITY_INTERVAL_OPTIONS_FOR_RAW_STATS_TEMP = [
  { value: timeInterval.LAST_MONTH, label: timeIntervalLabel.LAST_MONTH_LABEL }
];

export const getInterval = (timeRange: string) => {
  switch (timeRange) {
    case timeInterval.LAST_2_QUARTER:
      return timeInterval.LAST_TWO_QUARTERS;
    case timeInterval.LAST_12_MONTHS:
      return timeInterval.LAST_TWELVE_MONTHS;
    default:
      return timeRange;
  }
};

export const DEV_CHILD_CHECKBOX_LABEL = "Display the scores of the immediate child Collections only";
