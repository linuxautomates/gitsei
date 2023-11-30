import moment from "moment";
import { forEach } from "lodash";
import { TimeFilterOptionType } from "dashboard/dashboard-types/FEBasedFilterConfig.type";

export enum TimeConfigTypes {
  DAYS = "days",
  WEEKS = "weeks",
  MONTHS = "months",
  QUARTERS = "quarters"
}

export type TimeConfigType = {
  options: number[];
};

export type TimeFiltersConfigType = {
  [TimeConfigTypes.DAYS]?: TimeConfigType;
  [TimeConfigTypes.WEEKS]?: TimeConfigType;
  [TimeConfigTypes.MONTHS]?: TimeConfigType;
  [TimeConfigTypes.QUARTERS]?: TimeConfigType;
};

export type TimeFilterOptionsConfig = {
  valueType?: "string" | "number";
};

export const buildTimeFiltersOptions = (
  timeConfig: TimeFiltersConfigType,
  optionsConfig?: TimeFilterOptionsConfig
): TimeFilterOptionType[] => {
  let filterOptions: TimeFilterOptionType[] = [];

  forEach(Object.keys(timeConfig), config => {
    switch (config) {
      case TimeConfigTypes.DAYS: {
        const daysOptions = timeConfig[TimeConfigTypes.DAYS]?.options;
        forEach(daysOptions, option => {
          filterOptions.push({
            value: {
              $lt: moment().utc().startOf("d").unix(),
              $gt: moment().utc().startOf("d").subtract(option, "d").unix()
            },
            label: `Last ${option} Days`,
            id: `last_${option}_days`,
            mFactor: option
          });
        });
        break;
      }
      case TimeConfigTypes.WEEKS: {
        const weeksOptions = timeConfig[TimeConfigTypes.WEEKS]?.options;
        forEach(weeksOptions, option => {
          const gt = moment().utc().startOf("w").subtract(option, "w").unix();
          const lt = moment().utc().startOf("w").unix();
          filterOptions.push({
            value: {
              $lt: lt,
              $gt: gt
            },
            label: `Last ${option} Weeks`,
            id: `last_${option}_weeks`,
            mFactor: Math.round((lt - gt) / 86400)
          });
        });
        break;
      }
      case TimeConfigTypes.MONTHS: {
        const monthsOptions = timeConfig[TimeConfigTypes.MONTHS]?.options;
        forEach(monthsOptions, option => {
          const gt = moment().utc().startOf("month").subtract(option, "month").unix();
          const lt = moment().utc().startOf("month").unix();
          filterOptions.push({
            value: {
              $lt: lt,
              $gt: gt
            },
            label: `Last ${option} Months`,
            id: `last_${option}_months`,
            mFactor: Math.round((lt - gt) / 86400)
          });
        });
        break;
      }
      case TimeConfigTypes.QUARTERS: {
        const quartersOptions = timeConfig[TimeConfigTypes.QUARTERS]?.options;
        forEach(quartersOptions, option => {
          const gt = moment().utc().startOf("quarter").subtract(option, "quarter").unix();
          const lt = moment().utc().startOf("quarter").unix();
          filterOptions.push({
            value: {
              $lt: lt,
              $gt: gt
            },
            label: `Last ${option} Quarters`,
            id: `last_${option}_quarters`,
            mFactor: Math.round((lt - gt) / 86400)
          });
        });
        break;
      }
    }
  });

  const valueType = optionsConfig?.valueType ?? "string";

  if (valueType === "string") {
    filterOptions = filterOptions.map((option: TimeFilterOptionType) => ({
      ...option,
      value: { $gt: option.value.$gt.toString(), $lt: option.value.$lt.toString() }
    }));
  }

  return filterOptions;
};
