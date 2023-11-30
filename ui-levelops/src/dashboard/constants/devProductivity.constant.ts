export enum engineerRatingType {
  GOOD = "GOOD",
  ACCEPTABLE = "ACCEPTABLE",
  NEED_IMPROVEMENT = "NEED IMPROVEMENT",
  NO_SCORE = "NO SCORE"
}

// new schema changes
export enum engineerRatingTypeNew {
  GOOD = "GOOD",
  ACCEPTABLE = "ACCEPTABLE",
  NEEDS_IMPROVEMENT = "NEEDS_IMPROVEMENT",
  NO_SCORE = "NO_SCORE"
}
// new schema changes
export const ratingToLegendColorMappingNew = {
  [engineerRatingTypeNew.GOOD]: "#61BA14",
  [engineerRatingTypeNew.ACCEPTABLE]: "#789FE9",
  [engineerRatingTypeNew.NEEDS_IMPROVEMENT]: "#FFA940",
  [engineerRatingTypeNew.NO_SCORE]: "#BFBFBF"
};

export const ratingToLegendColorMapping = {
  [engineerRatingType.GOOD]: "#61BA14",
  [engineerRatingType.ACCEPTABLE]: "#789FE9",
  [engineerRatingType.NEED_IMPROVEMENT]: "#FFA940",
  [engineerRatingType.NO_SCORE]: "#BFBFBF"
};

export const ratingToColorMapping = {
  [engineerRatingType.GOOD]: "linear-gradient(243.38deg, #5B9B24 33.31%, #61BA14 92.91%)",
  [engineerRatingType.ACCEPTABLE]: "linear-gradient(260.91deg, #3D7CF4 8.19%, #789FE9 98.36%)",
  [engineerRatingType.NEED_IMPROVEMENT]: "linear-gradient(256.92deg, #F9AF32 -2.81%, #F7CD89 127.49%)",
  [engineerRatingType.NO_SCORE]: "linear-gradient(256.92deg, #F9AF32 -2.81%, #F7CD89 127.49%)"
};

// new schema changes
export const ratingKeyMapping:  Record<string, string> = {
  GOOD : "GOOD",
  ACCEPTABLE : "ACCEPTABLE",
  NEED_IMPROVEMENT : "NEED_IMPROVEMENT",
  NO_SCORE : "NO_SCORE"
}

export enum timeInterval {
  LAST_2_QUARTER = "LAST_2_QUARTER",
  LAST_TWO_QUARTERS = "LAST_TWO_QUARTERS",
  LAST_12_MONTHS = "LAST_12_MONTHS",
  LAST_TWELVE_MONTHS = "LAST_TWELVE_MONTHS",
  LAST_YEAR = "LAST_YEAR",
  LAST_QUARTER = "LAST_QUARTER",
  LAST_MONTH = "LAST_MONTH",
  LAST_3_MONTH = "LAST_3_MONTH",
  LAST_30_DAYS = "LAST_30_DAYS",
  LAST_2_WEEKS = "LAST_2_WEEKS",
  LAST_7_DAYS = "LAST_7_DAYS",
  LAST_14_DAYS = "LAST_14_DAYS",
  LAST_WEEK = "LAST_WEEK",
  LAST_TWO_WEEKS = "LAST_TWO_WEEKS",
  LAST_TWO_MONTHS = "LAST_TWO_MONTHS",
  LAST_THREE_MONTHS = "LAST_THREE_MONTHS"
}

//for interval dropdown labels
export enum timeIntervalLabel {
  LAST_MONTH_LABEL = "Last Month",
  LAST_QUARTER_LABEL = "Last Quarter",
  LAST_2_QUARTER_LABEL = "Last Two Quarters",
  LAST_12_MONTH_LABEL = "Last 12 Months",
  LAST_14_DAYS = "Last 14 Days",
  LAST_WEEK = "Last Week",
  LAST_TWO_WEEKS = "Last 2 Weeks",
  LAST_TWO_MONTHS = "Last 2 Months",
  LAST_THREE_MONTHS = "Last 3 Months"
}
