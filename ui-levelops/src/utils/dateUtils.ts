import moment, { Moment } from "moment";
import { TimeRangeLimit } from "shared-resources/components/range-picker/CustomRangePickerTypes";
import { TIME_INTERVAL_TYPES, WEEK_DATE_FORMAT } from "constants/time.constants";
import { timeInterval } from "dashboard/constants/devProductivity.constant";
import { timeStampToValue } from "dashboard/components/dashboard-view-page-secondary-header/helper";
import { truncatedString } from "./stringUtils";
import { convertToDays, convertToMins } from "./timeUtils";

export enum DateFormats {
  // doing this change for consistency
  DAY = "DD MMM YYYY",
  BA_DAY = "DD MMM YYYY",
  //WEEK = "ww-gggg",
  WEEK = "w-YYYY",
  ISO_WEEK = "WW-YYYY",
  MONTH = "MMM YYYY",
  MONTH_YEAR = "MMM-YYYY",
  YEAR = "YYYY",
  QUARTER = "[Q]Q-YYYY",
  QUARTER_MONTH = "Q-YYYY", // there is no direct way to convert Q-1 => Jan-Mar
  // Todo Need fix
  //  Repeating the DateFormats.DAY here until format locks to replace " D MMM " with " D MMM YYYY "
  DAY_MONTH = "DD MMM YYYY",
  DAY_TIME = "DD MMM YYYY HH:mm",
  MONTH_DATE_YEAR = "MM-DD-YYYY",
  DATE_MONTH_YEAR = "DD-MM-YYYY",
  MONTH_YEAR_NUMBER = "M-YYYY",
  HOUR_MINIT_TIME = "HH:mm"
}
export enum DateRange {
  FROM = "From",
  TO = "To"
}

const getQuarterInMonth = (epoch: moment.Moment) => {
  const quarter = epoch.format("Q");
  const year = epoch.format("YYYY");
  switch (quarter) {
    case "1":
      return `Jan-Mar ${year}`;
    case "2":
      return `Apr-Jun ${year}`;
    case "3":
      return `Jul-Sep ${year}`;
    case "4":
      return `Oct-Dec ${year}`;
    default:
      return `${quarter} ${year}`;
  }
};
const getISOWeek = function (_date: Date) {
  let d = new Date(_date);
  var date = new Date(d.getTime());
  date.setHours(0, 0, 0, 0);
  // Thursday in current week decides the year.
  date.setDate(date.getDate() + 3 - ((date.getDay() + 6) % 7));
  // January 4 is always in week 1.
  var week1 = new Date(date.getFullYear(), 0, 4);
  // Adjust to Thursday in week 1 and count number of weeks from date to week1.
  let isoWeek =
    1 +
    Math.round(((date.getTime() - week1.getTime()) / 86400000 - 3 + ((week1.getDay() + 6) % 7)) / 7) +
    "-" +
    date.getFullYear();
  return isoWeek;
};
function getWeek(_date: Date) {
  let d = new Date(_date);
  d = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));

  d.setUTCDate(d.getUTCDate() + 4 - (d.getUTCDay() || 7));
  var yearStart: any = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
  var weekNo = Math.ceil((((d as any) - yearStart) / 86400000 + 1) / 7);
  return weekNo + "-" + d.getUTCFullYear();
}
// This function accepts time stamp in second with a format, and returns utc moment in desired moment from timestamp
export const convertEpochToDate = (
  epoch: number | string,
  format: DateFormats | string = DateFormats.DAY,
  utc = false
) => {
  if (!utc) {
    return moment.unix(+epoch).format(format);
  } else {
    let newEpoch = getDateFromTimeStampInGMTFormat(epoch);
    if (format === DateFormats.QUARTER_MONTH) {
      return getQuarterInMonth(moment(newEpoch));
    }
    if (format === DateFormats.WEEK) {
      return getWeek(newEpoch.toDate());
    }
    if (format === DateFormats.ISO_WEEK) {
      return getISOWeek(newEpoch.toDate());
    }
    return newEpoch.format(format);
  }
};

export const getStartOfDayFromDate = (date: Moment | undefined | null, convert: boolean = true) => {
  if (!date) {
    return 0;
  }
  if (!convert) {
    return date.startOf("day").unix();
  }
  return date
    .add(-1 * moment().utcOffset(), "m")
    .startOf("day")
    .unix();
};

// converts startOfDay in UTC
export const getStartOfDayFromDateString = (date: string, format = "YYYY/MM/DD") => {
  if (!date) {
    return 0;
  }

  return moment.utc(date, format).startOf("day").unix();
};

// converts endOfDay in UTC
export const getEndOfDayFromDateString = (date: string, format = "YYYY/MM/DD") => {
  if (!date) {
    return 0;
  }

  return moment.utc(date, format).endOf("day").unix();
};

export const getEndOfDayFromDate = (date: Moment | undefined | null, convert: boolean = true) => {
  if (!date) {
    return 0;
  }
  if (!convert) {
    return date.endOf("day").unix();
  }
  return date
    .endOf("day")
    .add(-1 * moment().utcOffset(), "m")
    .unix();
};

export const getStartOfDayTimeStamp = (epoch: number | string, convert: boolean = true) => {
  if (!epoch) {
    return 0;
  }
  return getStartOfDayFromDate(moment.unix(+epoch), convert);
};

export const getStartOfDayTimeStampGMT = (epoch: number | string) => {
  if (!epoch) {
    return 0;
  }
  return getDateFromTimeStampInGMTFormat(epoch).startOf("day").unix();
};

export const getEndOfDayTimeStamp = (epoch: number | string, convert: boolean = true) => {
  if (!epoch) {
    return 0;
  }
  return getEndOfDayFromDate(moment.unix(+epoch), convert);
};

export const getEndOfDayTimeStampGMT = (epoch: number | string) => {
  if (!epoch) {
    return 0;
  }
  return getDateFromTimeStampInGMTFormat(epoch).endOf("day").unix();
};

export const getDateFromTimeStampInGMTFormat = (epoch: number | string) => {
  let newEpoch = moment.unix(+epoch);
  const isDST = newEpoch.isDST();
  newEpoch = newEpoch.add(-1 * moment().utcOffset(), "m");
  if (!isDST) {
    newEpoch = newEpoch.add(1, "hour");
  }
  return newEpoch;
};
// Date Range utils
export const getStartValue = (value: moment.Moment[]) => {
  let startValue;
  if (value && value[0] && value[0].isValid()) {
    startValue = value[0];
  }

  return startValue;
};

export const getEndValue = (value: moment.Moment[]) => {
  let endValue;
  if (value && value[1] && value[1].isValid()) {
    endValue = value[1];
  }

  return endValue;
};

export const WARNING_90_DAY = "Time range for trend reports cannot exceed 90 days";
export const WARNING_START = "Can't select a start date after the end date";
export const WARNING_END = "Can't select an end date before the start date";
export const isDisabledDate = (
  current_date: moment.Moment,
  picker_source: "start" | "end",
  start_date: moment.Moment | undefined,
  end_date: moment.Moment | undefined,
  rangeLimit: TimeRangeLimit | undefined
) => {
  let result = {
    is_disabled: false,
    reason: ""
  };

  if (current_date && rangeLimit) {
    let isWithinRange;
    if (start_date && end_date) {
      const top_limit = start_date.clone().add(rangeLimit.length, rangeLimit.units);
      const bottom_limit = end_date.clone().subtract(rangeLimit.length, rangeLimit.units);

      if (picker_source === "start") {
        let high_enough = bottom_limit <= current_date;
        let low_enough = current_date <= end_date;

        isWithinRange = high_enough && low_enough;
        if (!isWithinRange) {
          if (!high_enough) {
            result.reason = WARNING_90_DAY;
          } else if (!low_enough) {
            result.reason = WARNING_START;
          }
        }
      } else {
        let high_enough = start_date <= current_date;
        let low_enough = current_date <= top_limit;

        isWithinRange = high_enough && low_enough;
        if (!isWithinRange) {
          if (!high_enough) {
            result.reason = WARNING_END;
          } else if (!low_enough) {
            result.reason = WARNING_90_DAY;
          }
        }
      }
    } else if (start_date) {
      if (picker_source === "start") {
        isWithinRange = true;
      } else {
        let high_enough = start_date <= current_date;
        let low_enough = current_date <= start_date.clone().add(rangeLimit.length, rangeLimit.units);

        isWithinRange = high_enough && low_enough;
        if (!isWithinRange) {
          if (!high_enough) {
            result.reason = WARNING_END;
          } else if (!low_enough) {
            result.reason = WARNING_90_DAY;
          }
        }
      }
    } else if (end_date) {
      if (picker_source === "end") {
        isWithinRange = true;
      } else {
        let high_enough = end_date.clone().subtract(rangeLimit.length, rangeLimit.units) <= current_date;
        let low_enough = current_date <= end_date;

        isWithinRange = high_enough && low_enough;
        if (!isWithinRange) {
          if (!high_enough) {
            result.reason = WARNING_90_DAY;
          } else if (!low_enough) {
            result.reason = WARNING_START;
          }
        }
      }
    } else {
      isWithinRange = true;
    }

    result.is_disabled = isWithinRange ? false : true;
  } else if (current_date) {
    let isWithinRange;
    if (start_date && end_date) {
      if (picker_source === "start") {
        // Start date must not be greater than end date.
        isWithinRange = current_date <= end_date;
        if (!isWithinRange) {
          result.reason = WARNING_START;
        }
      } else {
        // End date must come after start date
        isWithinRange = start_date <= current_date;
        if (!isWithinRange) {
          result.reason = WARNING_END;
        }
      }
    } else if (start_date) {
      if (picker_source === "start") {
        isWithinRange = true;
      } else {
        // End date must be greater than start date
        isWithinRange = start_date <= current_date;
        if (!isWithinRange) {
          result.reason = WARNING_END;
        }
      }
    } else if (end_date) {
      if (picker_source === "end") {
        isWithinRange = true;
      } else {
        // Start date must less than end date
        isWithinRange = current_date <= end_date;
        if (!isWithinRange) {
          result.reason = WARNING_START;
        }
      }
    } else {
      isWithinRange = true;
    }

    result.is_disabled = isWithinRange ? false : true;
  }

  return result;
};

export const validateRange = (start_date: moment.Moment | undefined, end_date: moment.Moment | undefined) => {
  if (start_date && end_date) {
    return "";
  } else if (!start_date && !end_date) {
    return "";
  } else {
    return "error";
  }
};

export const getHelpText = (start_date: moment.Moment | undefined, end_date: moment.Moment | undefined) => {
  let help_text = "";
  if (start_date && end_date) {
    // No problems.
  } else if (start_date) {
    help_text = "Please select an end date.";
  } else if (end_date) {
    help_text = "Please select a start date.";
  }

  return help_text;
};

export const valueToUnixTime = (value: any) => {
  let moment_value;
  try {
    moment_value = moment.unix(value);

    if (!moment_value.isValid()) {
      moment_value = moment(value);
    }
  } catch (error) {}

  if (moment_value && moment_value.isValid()) {
    return moment_value.unix();
  }
};

export const valueToUtcUnixTime = (value: any, format = "MM/DD/YYYY") => {
  return moment.utc(value, format).unix();
};

export const DEFAULT_DATE_FORMAT = DateFormats.DAY;

export const getMomentFromInterval = (
  date: string | undefined,
  type:
    | TIME_INTERVAL_TYPES.YEAR
    | TIME_INTERVAL_TYPES.DAY
    | TIME_INTERVAL_TYPES.WEEK
    | TIME_INTERVAL_TYPES.ISOWEEK
    | TIME_INTERVAL_TYPES.MONTH
    | TIME_INTERVAL_TYPES.QUARTER = TIME_INTERVAL_TYPES.DAY,
  dateRange: DateRange,
  weekDayFormat?: WEEK_DATE_FORMAT.DATE | WEEK_DATE_FORMAT.NUMBER
) => {
  if (!date) {
    return null;
  }

  let format = "",
    modifiedDate = null,
    _date = null;

  switch (type) {
    case TIME_INTERVAL_TYPES.QUARTER:
      format = DateFormats.QUARTER_MONTH;
      break;
    case TIME_INTERVAL_TYPES.ISOWEEK:
    case TIME_INTERVAL_TYPES.WEEK:
      if (weekDayFormat === WEEK_DATE_FORMAT.NUMBER) {
        format = DateFormats.ISO_WEEK;
      } else {
        //since we reverted the format from week number to DD-MMM-YYYY
        format = DateFormats.DAY; //ISO_WEEK
      }
      break;
    case TIME_INTERVAL_TYPES.MONTH:
      format = DateFormats.MONTH_YEAR;
      break;
    case TIME_INTERVAL_TYPES.DAY:
      format = DateFormats.DAY;
      break;
    case TIME_INTERVAL_TYPES.YEAR:
      format = DateFormats.YEAR;
      break;
    default:
      // default case for month
      format = DateFormats.DATE_MONTH_YEAR;
  }

  modifiedDate = moment.utc(date, format);
  if (dateRange === DateRange.TO) _date = modifiedDate.endOf(type);
  else _date = modifiedDate.startOf(type);

  return _date;
};

/* export const getDateFromTimeInterval = (
  date: Moment | undefined | null | string,
  type:
    | TIME_INTERVAL_TYPES.DAY
    | TIME_INTERVAL_TYPES.WEEK
    | TIME_INTERVAL_TYPES.ISOWEEK
    | TIME_INTERVAL_TYPES.MONTH
    | TIME_INTERVAL_TYPES.QUARTER = TIME_INTERVAL_TYPES.DAY,
  dateRange: DateRange,
  format: string | null = null
) => {
  if (!date) {
    return 0;
  }
  let _date = null;
  let weekYear = null,
    quarterYear = null,
    quarter = null,
    year = null,
    week = null,
    startDate = null;

  switch (type) {
    case TIME_INTERVAL_TYPES.QUARTER:
      quarterYear = date.toString().split("-");
      quarter = parseInt(quarterYear[0]?.slice(-1), 10);
      year = parseInt(quarterYear[1], 10);
      startDate = new Date(year, quarter * 3 - 3, 1);
      if (dateRange === DateRange.TO) _date = new Date(year, startDate.getMonth() + 3, 0);
      else _date = startDate;
      break;
    case TIME_INTERVAL_TYPES.ISOWEEK:
      weekYear = date.toString().split("-");
      week = parseInt(weekYear[0], 10);
      year = parseInt(weekYear[1], 10);
      _date = getDateOfISOWeek(week, year, dateRange);
      break;
    case TIME_INTERVAL_TYPES.WEEK:
      weekYear = date.toString().split("-");
      week = parseInt(weekYear[0], 10);
      year = parseInt(weekYear[1], 10);
      _date = getDateOfWeek(week, year, dateRange);
      break;
    default:
      _date = moment(date);
      if (dateRange === DateRange.FROM) _date = _date.startOf(type).add(_date.utcOffset(), TIME_INTERVAL_TYPES.MINUTES);
      else _date = _date.endOf(type).add(_date.utcOffset(), TIME_INTERVAL_TYPES.MINUTES);
      break;
  }
  if (format) {
    _date = moment(_date).format(format);
  }
  return _date;
};

export const getDateOfWeek = (w: number, y: number, type: DateRange) => {
  var weekStart = new Date(y, 0, 1 + (w - 1) * 7);
  switch (type) {
    case DateRange.FROM:
      return weekStart;
    case DateRange.TO:
      return weekStart?.setDate(weekStart.getDate() + 6);
    default:
      return weekStart;
  }
};

export const getDateOfISOWeek = (w: number, y: number, type: DateRange) => {
  var simple = new Date(y, 0, 1 + (w - 1) * 7);
  var dow = simple.getDay();
  var ISOweekStart = simple;
  if (dow <= 4) ISOweekStart?.setDate(simple.getDate() - simple.getDay() + 1);
  else ISOweekStart?.setDate(simple.getDate() + 8 - simple.getDay());
  switch (type) {
    case DateRange.FROM:
      return ISOweekStart;
    case DateRange.TO:
      return ISOweekStart?.setDate(ISOweekStart.getDate() + 6);
    default:
      return ISOweekStart;
  }
}; */

export function getDayFromEveryWeekBetweenDates(start: Date, end: Date, dayName: string) {
  var result = [];
  var days: any = { sun: 0, mon: 1, tue: 2, wed: 3, thu: 4, fri: 5, sat: 6 };
  var day = days[dayName.toLowerCase().substr(0, 3)];
  // Copy start date
  var current = new Date(start);
  // Shift to next of required days
  current.setDate(current.getDate() + ((day - current.getDay() + 7) % 7));
  // While less than end date, add dates to result array
  while (current < end) {
    result.push(new Date(+current));
    current.setDate(current.getDate() + 7);
  }
  return result;
}

export function getDayFromEveryWeekBetweenTimeStamps(start: string | number, end: string | number, dayName: string) {
  return getDayFromEveryWeekBetweenDates(moment.unix(+start).toDate(), moment.unix(+end).toDate(), dayName);
}

export function getDayFromEvery2WeekBetweenDates(start: Date, end: Date, dayName: string) {
  var result = [];
  var days: any = { sun: 0, mon: 1, tue: 2, wed: 3, thu: 4, fri: 5, sat: 6 };
  var day = days[dayName.toLowerCase().substr(0, 3)];
  // Copy start date
  var current = new Date(start);
  current.setTime(current.getTime() - current.getTimezoneOffset() * 60 * 1000);
  // Shift to next of required days
  current.setDate(current.getDate() + ((day - current.getDay() + 7) % 7));
  // While less than end date, add dates to result array
  while (current < end) {
    result.push(new Date(+current));
    current.setDate(current.getDate() + 14);
  }
  return result;
}

export function getDayFromEvery2WeekBetweenTimeStamps(start: string | number, end: string | number, dayName: string) {
  const availableMondays = getDayFromEvery2WeekBetweenDates(
    moment.unix(+start).toDate(),
    moment.unix(+end).toDate(),
    dayName
  );
  return (availableMondays || []).map((day: Date) => {
    const timestamp = day.getTime() / 1000;
    return {
      $gt: moment.unix(timestamp).utc().startOf("d").unix(),
      $lt: moment.unix(timestamp).utc().add(1, "w").endOf("isoWeek").unix()
    };
  });
}

export function getDayFromEveryMonthsBetweenDates(start: Date, end: Date, dayName: string) {
  var result = [];
  var days: any = { sun: 0, mon: 1, tue: 2, wed: 3, thu: 4, fri: 5, sat: 6 };
  var day = days[dayName.toLowerCase().substr(0, 3)];
  // Copy start date
  var current = new Date(start);
  // Shift to next of required days
  current.setDate(current.getDate() + ((day - current.getDay() + 7) % 7));
  // While less than end date, add dates to result array
  while (current < end) {
    result.push(new Date(+current));
    let currentMonth = current.getMonth();
    let year = current.getFullYear();
    if (currentMonth === 11) {
      currentMonth = 0;
      current.setMonth(currentMonth);
      current.setFullYear(year + 1);
    } else {
      current.setMonth(currentMonth + 1);
    }
  }
  return result;
}

export function getDayFromEveryMonthsBetweenTimeStamps(start: string | number, end: string | number, dayName: string) {
  return getDayFromEveryMonthsBetweenDates(moment.unix(+start).toDate(), moment.unix(+end).toDate(), dayName);
}

export const isvalidTimeStamp = (timestamp: string | undefined) => {
  let validtimestamp = false;
  const timeStampInString = timestamp && timestamp.toString();
  if (
    timestamp &&
    timeStampInString &&
    !timeStampInString.includes("-") &&
    !timeStampInString.includes("/") &&
    timeStampInString?.length > 9
  ) {
    var re = new RegExp("[a-zA-Z]");
    const containChar = re.test(timeStampInString);
    if (!containChar && moment(parseInt(timestamp)).isValid()) {
      validtimestamp = true;
    }
  }
  return validtimestamp;
};

export const isValidDateHandler = (date: string | undefined) => {
  let validDate = false;
  if (date) {
    var re = new RegExp("[a-zA-Z]");
    const containChar = re.test(date);
    if (!containChar && moment(date, "DD-MM-YYYY HH:mm:ss").isValid() && date.length > 9) {
      validDate = true;
    }
  }
  return validDate;
};

// epoch is in seconds
export const convertToHours = (epoch: number) => {
  if (epoch <= 0) return 0;
  return Math.round(epoch / 3600);
};
/**
 * Get Date object from the time stamp
 * @param timestamp
 * @returns Moment object
 */
export const getDate = (timestamp: any): Moment => moment.unix(timestamp).utc();

/**
 * Format date with MMM DD, YYYY format
 * @param date Moment object
 * @returns formated string
 */
export const formatDate = (date: Moment): String => date.format(DateFormats.DAY);

/**
 * Convert date to unix timestap
 * @param date Moment object
 * @returns umix timestamp
 */
export const getTimeStamp = (date: Moment): number => date.unix();

interface GetXAxisTimeLabelProps {
  key: string;
  interval: string;
  options?: {
    weekDateFormat?: string;
  };
}

export function getXAxisTimeLabel(props: GetXAxisTimeLabelProps): string {
  const { key, interval, options } = props;
  const weekDateFormat = options?.weekDateFormat || WEEK_DATE_FORMAT.DATE;
  let newLabel = "";

  switch (interval) {
    case TIME_INTERVAL_TYPES.DAY:
      newLabel = convertEpochToDate(key, DateFormats.DAY, true);
      break;
    case TIME_INTERVAL_TYPES.WEEK:
      const format = weekDateFormat === WEEK_DATE_FORMAT.NUMBER ? DateFormats.WEEK : DateFormats.DAY;
      newLabel = convertEpochToDate(key, format, true);
      break;
    case TIME_INTERVAL_TYPES.MONTH:
      newLabel = convertEpochToDate(key, DateFormats.MONTH, true);
      break;
    case TIME_INTERVAL_TYPES.QUARTER:
      newLabel = convertEpochToDate(key, DateFormats.QUARTER, true);
      break;
    case TIME_INTERVAL_TYPES.YEAR:
      newLabel = convertEpochToDate(key, DateFormats.YEAR, true);
      break;
    case TIME_INTERVAL_TYPES.DAY_OF_WEEK:
      newLabel = key;
      break;
    default:
      newLabel = convertEpochToDate(key, DateFormats.DAY, true);
      break;
  }
  return newLabel;
}

export const convertUnixToDate = (date: number, format: string = DateFormats.DAY) => {
  return moment(+date).format(format);
};

/**
 * Get start and end of week starting monday
 * @param date current date
 * @returns start and end date of week
 */
export const getWeekStartEnd = (date: Moment): { start: Moment; end: Moment } => {
  const start = moment(date).startOf("isoWeek");
  const end = moment(date).endOf("isoWeek");

  return { start, end };
};

const getTwoQuarterInMonth = (epoch: moment.Moment) => {
  const quarter = epoch.format("Q");
  const year = epoch.format("YYYY");
  const prevYear = parseInt(year) - 1;
  switch (quarter) {
    case "1":
      return `Jan-Jun ${year}`;
    case "2":
      return `Apr-Sep ${year}`;
    case "3":
      return `Jul-Dec ${year}`;
    case "4":
      return `Oct ${prevYear}-Mar ${year}`;
    default:
      return `${quarter} ${year}`;
  }
};

const getYearInMonth = (epoch: moment.Moment) => {
  const currentMonth = epoch.format("MMM");
  const year = epoch.format("YYYY");
  const prevYear = parseInt(year) - 1;
  const prevMonth = epoch.add(-1, "m").format("MMM");
  return `${currentMonth} ${prevYear}-${prevMonth} ${year}`;
};

/**
 *  Get the interval string for the interval and the month-year key
 *
 */
export const getIntervalString = (dateKey: string, interval: TIME_INTERVAL_TYPES = TIME_INTERVAL_TYPES.MONTH) => {
  const epoch = moment(dateKey, DateFormats.MONTH_YEAR_NUMBER);
  switch (interval) {
    case TIME_INTERVAL_TYPES.MONTH:
      return epoch.format(DateFormats.MONTH_YEAR);
    case TIME_INTERVAL_TYPES.QUARTER:
      return getQuarterInMonth(epoch);
    case TIME_INTERVAL_TYPES.TWO_QUARTERS:
      return getTwoQuarterInMonth(epoch);
    case TIME_INTERVAL_TYPES.YEAR:
      return getYearInMonth(epoch);
    case TIME_INTERVAL_TYPES.WEEK:
      return getXAxisTimeLabel({ key: dateKey, interval });
    case TIME_INTERVAL_TYPES.BI_WEEK:
      return getXAxisTimeLabel({ key: dateKey, interval: TIME_INTERVAL_TYPES.WEEK });
    default:
      return "";
  }
};

export const dateRangeFromStartMoment = (startDate: Moment, interval: string): any => {
  switch (interval.toLowerCase()) {
    case timeInterval.LAST_MONTH.toLowerCase():
      return {
        $gt: moment(startDate).startOf("month").utc().unix(),
        $lt: moment(startDate).endOf("month").utc().unix()
      };
    case timeInterval.LAST_QUARTER.toLowerCase():
      return {
        $gt: moment(startDate).startOf("quarter").utc().unix(),
        $lt: moment(startDate).endOf("quarter").utc().unix()
      };
    case timeInterval.LAST_2_QUARTER.toLowerCase():
    case timeInterval.LAST_TWO_QUARTERS.toLocaleLowerCase():
      const quarters = moment(startDate).add("quarter", 1);
      const last2quarter = moment(startDate);
      return {
        $gt: last2quarter.startOf("quarter").utc().unix(),
        $lt: quarters.endOf("quarter").utc().unix()
      };
    default:
      return {
        $gt: moment(startDate).startOf("day").utc().unix(),
        $lt: moment(startDate).add("days", 30).endOf("day").utc().unix()
      };
  }
};

/**
 *  Get the Array of last 12 Months in format MMM YYYY
 *
 */
export const getLastTwelveMonthsWithyear = () => {
  let months = [];
  let monthsRequired = 12;
  for (let i = monthsRequired; i >= 1; i--) {
    months.push(moment().subtract(i, "months").format("MMM YYYY"));
  }
  return months;
};

/**
 *  Get the Array of last 4 Quarters in format MMM YYYY
 *
 */
export const getLastFourQuartersWithYear = () => {
  let quarters = [4, 3, 2, 1].map(i => moment().subtract(i, "Q").startOf("quarter").format("MMM YYYY"));
  return quarters;
};

/**
 *  Get the Array of last 4 Quarters in format M-YYYY
 *
 */
export const getLastFourQuartersWithMonthAndYear = () => {
  let quarters = [4, 3, 2, 1].map(i => moment().subtract(i, "Q").startOf("quarter").format("M-YYYY"));
  return quarters;
};
/**
 * convert a date range object to readable format
 * @param dateRange date range object
 * @returns readable date range
 */
export const getDateRangeEpochToString = (dateRange?: { $lt: string; $gt: string }) => {
  if (!dateRange) {
    return "";
  }

  const fromDate = timeStampToValue(dateRange.$gt, DateFormats.DAY);
  const toDate = timeStampToValue(dateRange.$lt, DateFormats.DAY);

  return `${fromDate} - ${toDate}`;
};

export const convertToSeconds = (value: string | number): string => {
  let asString = value.toString();

  // Making sure that the string is valid and contains only digits
  if (!asString && /^\d+\.\d+$/.test(asString)) return "";

  return asString.slice(0, 10);
};

/***
 * @retun start date timestamp of unix date
 * @param { number } unixDate unix timestamp
 * */
export const getUnixStartofDay = (unixDate: number) => {
  return moment.unix(unixDate)?.utc()?.startOf("day")?.unix();
};

export const compareCurrentDate = (date: string, format: DateFormats = DateFormats.MONTH_DATE_YEAR) => {
  return moment().format(format) < date;
};

/***
 * @return formatted date from unix timestamp if date is valid else returns NA it coverts always unix milliseconds to seconds
 * @param { string | number } date unix timestamp
 * @param { string } format date format
 * @param { number } truncateLen unix truncate length truncateLen
 * @param { boolean } fromNow calling fromNow
 * */
export const unixToDate = (date: string | number, fromNow: boolean = false, format: string = DateFormats.DAY) => {
  return date
    ? fromNow
      ? moment.unix(+truncatedString(date)).fromNow()
      : moment.unix(+truncatedString(date)).format(format)
    : "NA";
};

/***
 * @return formatted date from unix timestamp if date is valid else returns NA it coverts always unix milliseconds to seconds
 * @param { string | number } date unix timestamp
 * @param { string } format date format
 * */
export const unixUTCToDate = (date: string | number, format: string = DateFormats.DAY) => {
  return date ? moment.unix(+truncatedString(date)).utc().format(format) : "NA";
};

export const getTimeForTrellisProfile = (unix: number) => {
  unix = +truncatedString(unix);
  return `${moment.unix(unix).format(DateFormats.DAY)} ${moment.unix(unix).format(DateFormats.HOUR_MINIT_TIME)}`;
};

export const convertToDate = (tm: string) => {
  const timestamp = parseInt(tm);
  return convertEpochToDate(timestamp, DateFormats.DAY, true);
};

export const convertTimeData = (data: any, convertTo: any) => {
  return data.map((item: any) => {
    let conversionFunction = convertToMins;
    switch (convertTo) {
      case "days":
        conversionFunction = convertToDays;
        break;
      case "hours":
        conversionFunction = convertToHours;
        break;
    }

    let rData = {
      ...item,
      min: item.min === "No Data" ? item.min : conversionFunction(item.min),
      median: item.median === "No Data" ? item.median : conversionFunction(item.median),
      max: item.max === "No Data" ? item.max : conversionFunction(item.max)
    };
    if (item.mean !== undefined) {
      rData = {
        ...rData,
        mean: item.mean === "No Data" ? item.mean : conversionFunction(item.mean)
      };
    }
    if (item.p90 !== undefined) {
      rData = {
        ...rData,
        p90: item.p90 === "No Data" ? item.p90 : conversionFunction(item.p90)
      };
    }
    return rData;
  });
};
