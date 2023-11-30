import moment from "moment";
import { AbsoluteTimeRange } from "model/time/time-range";
import { RelativeTimeRangeUnits } from "shared-resources/components/relative-time-range/constants";
import { orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import { stringSortingComparator } from "dashboard/graph-filters/components/sort.helper";

export const getRelativeValueFromTimeRange = (data?: AbsoluteTimeRange) => {
  if (!data) {
    return undefined;
  }

  const startOfDay = moment().utc().startOf("day").unix();
  const absoluteEnd = data?.$lt;
  const absoluteStart = data?.$gt;
  if (absoluteEnd && absoluteStart && absoluteStart <= startOfDay) {
    const lastDays = Math.round((startOfDay - parseInt(absoluteStart.toString())) / 86400);
    return {
      num: lastDays,
      unit: lastDays > 0 ? RelativeTimeRangeUnits.DAYS : RelativeTimeRangeUnits.TODAY
    };
  }

  return undefined;
};

export const getValueFromTimeRange = (data: any) => {
  if (data?.unit === "days" && data?.num === "" && data?.unit === "today") {
    return 0;
  }
  const $lt = moment.utc().startOf("day").unix();
  let $gt;

  if (data?.unit === "today") {
    $gt = moment.utc().startOf("day").unix();
  } else {
    $gt = moment
      .utc()
      .add(-1 * data?.num, data?.unit)
      .startOf("day")
      .unix();
  }
  return Math.round(($lt - $gt) / 86400);
};

export const transformOUParentNodeOptions = (options: orgUnitJSONType[], ouName: string, ouExits?: boolean) => {
  let nOptions = (options ?? [])
    .map(option => {
      if (option.path) {
        const parents = option.path.split("/").filter(v => !!v);

        /** Checking for OU level.
         *  For inc the level change
         *  below check value to (Req. Level + 1)
         *  +1 as we are excluding the root level
         */
        if (parents.length === 11) {
          option.disabled = true;
        }
      }
      return option;
    })
    .sort(stringSortingComparator("name"));
  // checking for potential cycles
  if (ouExits) {
    nOptions = nOptions.map(option => {
      if (option.path) {
        const parents = option.path.split("/").filter(v => !!v);
        if (parents.includes(ouName)) {
          option.disabled = true;
        }
      }
      return option;
    });
  }
  return nOptions;
};
