import { RANGE_FILTER_CHOICE } from "dashboard/constants/filter-key.mapping";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import moment from "moment";

export const deploymentFrequencySingleStatFilters: basicMappingType<any> = {
  across: "trend"
};

export const deploymentFrequencySingleStatChartProps: basicMappingType<any> = {
  unit: "Per Day"
};

export const deploymentFrequencySingleStatDefaultQuery: basicMappingType<any> = {
  metric: "resolve",
  pr_merged_at: {
    $lt: moment.utc().unix().toString(),
    $gt: moment.utc().unix().toString()
  }
};

export const DEPLOYMENT_FREQUENCY_DESCRIPTION =
  "Deployment Frequency is a measure of how often a team successfully releases or deploys code to production. For the Elite performing teams, deployment frequency is greater than multiple deploys per day, the High is between once per day and once per week, the Medium is between once per week and once per month, and the Low is less than once per month.";

export const DefaultMetadata = {
  [RANGE_FILTER_CHOICE]: {
    pr_merged_at: {
      type: "relative",
      relative: {
        last: {
          num: 30,
          unit: "days"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};
export const MESSAGE = "This report is deprecated and is no more supported.";
