import {
  RelativeTimeRangeUnits,
  TimeRangeFilterType
} from "../../shared-resources/components/relative-time-range/constants";

export interface RelativeTimeRangeDropDownPayload {
  num?: any;
  unit: RelativeTimeRangeUnits;
}

export interface RelativeTimeRangePayload {
  last: RelativeTimeRangeDropDownPayload;
  next: RelativeTimeRangeDropDownPayload;
}

export interface AbsoluteTimeRange {
  $gt?: string | number;
  $lt?: string | number;
}

export interface TimeRangeAbsoluteRelativePayload {
  type: TimeRangeFilterType;
  relative: RelativeTimeRangePayload;
  absolute?: AbsoluteTimeRange; // time filters are optional
  required_error?: boolean;
  required_error_msg?: string;
}
