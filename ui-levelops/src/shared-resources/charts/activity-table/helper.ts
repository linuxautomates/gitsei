import { Moment } from "moment";
import { baseColumnConfig } from "utils/base-table-config";
import { dots } from "./dots";

export enum PR_STATUS {
  PR_CREATED = "PR_CREATED",
  PR_MERGED = "PR_MERGED",
  PR_CLOSED = "PR_CLOSED",
  PR_COMMENTS = "PR_COMMENTS",
  COMMIT_CREATED = "COMMIT_CREATED"
}

export const ACTIVITY_COLORS = {
  [PR_STATUS.PR_CREATED]: "#336AD5",
  [PR_STATUS.PR_MERGED]: "#DD8B39",
  [PR_STATUS.PR_CLOSED]: "#DCBF40",
  [PR_STATUS.PR_COMMENTS]: "#8D8BF3",
  [PR_STATUS.COMMIT_CREATED]: "#53BDC5"
};

export const PR_DATA_INDEX = {
  [PR_STATUS.PR_CREATED]: "num_prs_created",
  [PR_STATUS.PR_MERGED]: "num_prs_merged",
  [PR_STATUS.PR_CLOSED]: "num_prs_closed",
  [PR_STATUS.PR_COMMENTS]: "num_prs_comments",
  [PR_STATUS.COMMIT_CREATED]: "num_commits_created"
};

export const weekDays = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

const weekdaysObj = weekDays.reduce((acc, day: string) => ({ ...acc, [day]: 0 }), {});

export const emptyActivityList = Object.values(PR_STATUS).reduce(
  (acc: any, type: string) => ({
    ...acc,
    [type]: { ...weekdaysObj, total: 0 }
  }),
  {}
);
