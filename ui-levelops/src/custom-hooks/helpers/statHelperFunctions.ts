import moment from "moment";
import { RelativeTimeRangeUnits } from "../../shared-resources/components/relative-time-range/constants";

export const isValidRelativeTime = (relativeTime: any) => {
  const last = relativeTime?.last;
  const next = relativeTime?.next;
  const validLast = (last?.unit !== "today" && last?.num) || last?.unit === "today";
  const validNext = (next?.unit !== "today" && next?.num) || next?.unit === "today";
  return validLast && validNext;
};

const getDashAbsValueFromRelativeTime = (relativeTime: any, completeRange = false, secondHalf = false) => {
  const multiplyFactor = completeRange || secondHalf ? 2 : 1;

  if (Object.keys(relativeTime).length > 0) {
    const last = relativeTime.last;
    const next = relativeTime.next;
    if (!["day", "days"].includes(last.unit)) {
      if (secondHalf) {
        return {
          $gt: moment
            .utc()
            .subtract(last.num * multiplyFactor, last.unit)
            .startOf(last.unit)
            .unix(),
          $lt: moment.utc().subtract(last.num, last.unit).endOf(last.unit).unix()
        };
      }

      return {
        $lt: moment.utc().subtract(1, last.unit).endOf(last.unit).unix().toString(),
        $gt: moment
          .utc()
          .subtract(last.num * multiplyFactor, last.unit)
          .startOf(last.unit)
          .unix()
          .toString()
      };
    } else {
      let $gt;
      let $lt;
      if (last) {
        // calculate $gt
        const isToday = last?.unit === RelativeTimeRangeUnits.TODAY;
        if (isToday) {
          $gt = moment.utc().startOf("day").unix();
        }
        if (!isToday && last?.num !== undefined) {
          $gt = moment
            .utc()
            .subtract((last?.num * multiplyFactor) as any, last.unit)
            .startOf(last.unit as any)
            .unix();
        }
      }

      if (next) {
        // calculate $lt
        const isToday = next?.unit === RelativeTimeRangeUnits.TODAY && !secondHalf;
        if (isToday) {
          $lt = moment.utc().endOf("day").unix();
        }
        if (!isToday && (next?.num !== undefined || secondHalf)) {
          $lt = moment
            .utc()
            .add(secondHalf ? -last.num : (next?.num as any), "day")
            .endOf("day")
            .unix();
        }
      }

      if ($gt && $lt) {
        return {
          $gt: $gt.toString(),
          $lt: $lt.toString()
        };
      }
    }
  }
};

export const getDashboardTimeRange = (dashMeta: any, completeRange = false, secondHalf = false) => {
  let relativeTime = {};

  const dashValue = dashMeta.dashboard_time_range_filter || "last_month";

  if (typeof dashValue === "string") {
    // convert string into relative time
    const splitValue = dashValue.split("_");
    let _unit = splitValue[splitValue.length - 1]?.toLowerCase();

    let _num = 1;
    if (splitValue.length === 3) {
      _num = parseInt(splitValue[1] || "1");
    }

    if (_unit === "weeks") {
      _unit = "days";
      _num = _num * 7;
    }

    relativeTime = {
      last: {
        num: _num,
        unit: _unit
      },
      next: {
        unit: "today"
      }
    };
  } else if (typeof dashValue === "object" && Object.keys(dashValue).length > 0) {
    const valueKeys = Object.keys(dashValue);
    if (valueKeys.includes("$gt") && valueKeys.includes("$lt")) {
      const diff = parseInt(dashValue?.$lt?.toString() || 0) - parseInt(dashValue?.$gt?.toString() || 0);

      if (secondHalf) {
        return {
          $gt: parseInt(dashValue?.$gt?.toString() || 0) - (diff + 1),
          $lt: parseInt(dashValue.$gt.toString() || 0) - 1
        };
      }

      return {
        $gt: (parseInt(dashValue?.$gt?.toString() || 0) - (completeRange ? diff + 1 : 0))?.toString(),
        $lt: dashValue.$lt?.toString()
      };
    }
  }

  return getDashAbsValueFromRelativeTime(relativeTime, completeRange, secondHalf);
};
