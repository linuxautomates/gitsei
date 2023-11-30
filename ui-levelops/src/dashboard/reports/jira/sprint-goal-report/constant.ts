import { RANGE_FILTER_CHOICE } from "dashboard/constants/filter-key.mapping";

export const DEFAULT_META = {
  [RANGE_FILTER_CHOICE]: {
    completed_at: {
      type: "relative",
      relative: {
        last: {
          num: 4,
          unit: "weeks"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};

export const COMPLETED_DATE_OPTIONS = [
  {
    id: "last_week",
    label: "Last week",
    mFactor: 7
  },
  {
    id: "last_2_weeks",
    label: "Last 2 Weeks",
    mFactor: 14
  },
  {
    id: "last_month",
    label: "Last Month",
    mFactor: 30
  },
  {
    id: "last_3_months",
    label: "Last 3 Months",
    mFactor: 90
  }
];
