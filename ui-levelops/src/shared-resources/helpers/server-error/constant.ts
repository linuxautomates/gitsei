export enum ServerErrorSource {
  WIDGET = "WIDGET",
  SERVER_PAGINATED_TABLE = "SERVER_PAGINATED_TABLE",
  STAT_WIDGET = "STAT_WIDGET"
}
export const ERROR_LINE1 = "Something went wrong while fetching data, please try again later.";
export const ERROR_LINE2 = "If this problem persists, contact support";
export const STAT_ERROR = "Unable to fetch data, please try again later.";
export const ERROR_CODE_RANGE_START = 500;
export const REQUEST_TIMEOUT_ERROR = 408;
export const ECONNABORTED = "ECONNABORTED";
