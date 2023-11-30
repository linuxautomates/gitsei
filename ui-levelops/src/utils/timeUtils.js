import { forEach, unset, round } from "lodash";
import moment from "moment";
import { isSanitizedValue } from "./commonUtils";
import { TIME_INTERVAL_TYPES, intervalToMomentKeyMapper } from "constants/time.constants";

const DEFAULT_PRECISION_VALUE = 0;

export function timeRange(num, type) {
  // examples
  // timeRange(30,days)
  // timeRange(24,hours)
  let now = Math.floor(Date.now() / 1000);
  let incr = 0;
  switch (type) {
    case "days":
      incr = 60 * 60 * 24 * num;
      break;
    case "hours":
      incr = 60 * 60 * num;
      break;
    default:
      break;
  }
  return {
    start: now - incr,
    stop: now
  };
}

export const convertToDays = (epoch, ceil = false) => {
  if (epoch <= 0) return 0;
  if (ceil) return Math.ceil(epoch / 86400);
  return Math.round(epoch / 86400);
};

export const convertToFixedDays = epoch => {
  return Math.round((epoch / 86400) * 100) / 100;
};

export const convertToMins = (epoch, ceil = false) => {
  if (epoch <= 0) return 0;
  if (ceil) return Math.ceil(epoch / 60);
  return Math.round(epoch / 60);
};

export const convertToHours = (epoch, ceil = false) => {
  if (epoch <= 0) return 0;
  if (ceil) return Math.ceil(epoch / 3600);
  return Math.round(epoch / 3600);
};

export const convertEpochToHumanizedForm = (convertTo, timestamp, ceil = false) => {
  let time = timestamp;
  if (!time) {
    return "";
  }
  const mins = convertToMins(time, ceil);
  if (convertTo === "days" || mins > 60 * 24) {
    time = convertToDays(time, ceil);
    return `${time} ${time === 1 ? `day` : `days`}`;
  }
  if (convertTo === "hours" || mins > 60) {
    time = convertToHours(time, ceil);
    return `${time} ${time === 1 ? `hour` : `hours`}`;
  }
  return `${mins} ${mins === 1 ? `min` : `mins`}`;
};

export const previousTimeStamps = days => {
  let today = moment().unix();
  let result = [];
  while (days--) {
    result.push(today - 86400);
    today -= 86400;
  }
  return result;
};

export const momentTimestampConvert = (timestamp, format = "MM/DD") => moment.unix(parseInt(timestamp)).format(format);

export const getTimezone = () => new Date().toString().match(/([A-Z]+[\+-][0-9]+.*)/)[1];

// this functions increases th given range by the value passed
export const increaseRangeBy = (range, increaseBy = 2) => {
  const gt = parseInt(range?.$gt);
  const lt = parseInt(range?.$lt);
  const momentGt = moment.unix(gt);
  const momentLt = moment.unix(lt);
  const diffDays = moment(momentLt).diff(momentGt, "days") * increaseBy;
  const newGt = moment.unix(lt).subtract(diffDays, "days").unix();
  if (typeof range?.$gt === "string") {
    return { $gt: `${newGt}`, $lt: `${range?.$lt}` };
  }
  return { $gt: newGt, $lt: range?.$lt };
};

export const getDiffInRange = (range, diffBy = "days") => {
  const gt = parseInt(range?.$gt);
  const lt = parseInt(range?.$lt);
  const momentGt = moment.unix(gt);
  const momentLt = moment.unix(lt);
  return moment(momentLt).diff(momentGt, diffBy);
};

export const sanitizeTimeFilters = (filters, timeKeys) => {
  forEach(timeKeys, key => {
    // checking only at first level , so send filter object here
    const filter = filters[key];
    // check if the time filter is of form {$lt , $gt}
    if (typeof filter === "object" && filter?.$lt) {
      const lt = filter?.$lt;
      const gt = filter?.$gt;
      // check if the values are in number form or string form
      if (typeof lt === "number") {
        if (lt === NaN || gt === NaN) {
          unset(filters, key);
        }
      } else if (lt === "NaN" || gt === "NaN") {
        unset(filters, key);
      }
      // filter is not in the gt lt form , not check for number or string
    } else if (typeof filter === "number" && filter === NaN) {
      unset(filters, key);
    } else if (typeof filter === "string" && filter === "NaN") {
      unset(filters, key);
    }
  });
  return filters;
};

export const allTimestampsBetween = (min, max) => {
  const minDate = moment.unix(min).utc().startOf("d").unix();
  const maxDate = moment.unix(max).utc().endOf("d").unix();
  let dates = [];
  let i = 0;
  let firstDate = minDate;
  while (firstDate < maxDate) {
    const nextdate = 86400 * i + minDate;
    firstDate = nextdate;
    dates = [...dates, nextdate];
    i++;
  }
  return dates;
};

export const getCurrentTimezoneOffset = () => Math.abs(new Date().getTimezoneOffset());

export const isFilterDateTimeType = (filter, form = "LT_GT") => {
  if (form === "LT_GT") {
    return isSanitizedValue(filter?.$gt) && isSanitizedValue(filter?.$lt);
  }
  // ADD MORE CASES
  return false;
};

export const getDaysAndTimeWithUnit = (epoch, precisionValue = DEFAULT_PRECISION_VALUE, alwaysIndays = undefined) => {
  let data;
  if (alwaysIndays) {
    data = {
      time: round(epoch, precisionValue),
      unit: epoch === 1 ? "day" : "days"
    };
    return data;
  }
  if (epoch <= 59) {
    data = {
      time: Math.round(epoch),
      unit: epoch === 0 || epoch === 1 ? "second" : "seconds",
      extraTime: undefined,
      extraUnit: undefined
    };
  } else if (epoch <= 3599) {
    const time = round(epoch / 60, precisionValue);
    const extraTime = epoch % 60;
    data = {
      time,
      unit: time === 1 ? "minute" : "minutes",
      extraTime,
      extraUnit: extraTime === 0 || extraTime === 1 ? "second" : "seconds"
    };
  } else if (epoch <= 86399) {
    const time = round(epoch / 3600, precisionValue);
    const extraTime = epoch % 3600;
    data = {
      time,
      unit: time === 1 ? "hour" : "hours",
      extraTime,
      extraUnit: extraTime === 0 || extraTime === 1 ? "minute" : "minutes"
    };
  } else {
    const time =
      epoch < 172799 ? round(((epoch / 86400) * 10) / 10, precisionValue) : round(epoch / 86400, precisionValue);
    const extraTime = epoch % 86400;
    data = {
      time,
      unit: time === 1 ? "day" : "days",
      extraTime,
      extraUnit: extraTime === 0 || extraTime === 1 ? "hour" : "hours"
    };
  }
  return data;
};

export const getEffortInvestIssueResolvedAt = interval => {
  const momentKeys = Object.keys(intervalToMomentKeyMapper);
  if (momentKeys.includes(interval)) {
    let num = interval === TIME_INTERVAL_TYPES.BI_WEEK ? 2 : 1;
    let unit = intervalToMomentKeyMapper[interval];
    return {
      $lt: moment().unix().toString(),
      $gt: moment().subtract(num, unit).unix().toString()
    };
  }
  return null;
};

export const humanizeDuration = (epoc) => moment.duration(epoc).humanize()