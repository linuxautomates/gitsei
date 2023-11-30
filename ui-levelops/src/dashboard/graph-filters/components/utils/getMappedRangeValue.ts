import { RangePickerValue } from "antd/lib/date-picker/interface";
import moment from "moment";

export interface MappedRangeType {
  $gt?: number | string;
  $lt?: number | string;
}

const FORMAT = "YYYY-MM-DD";
/**
 * Input Note: dates moment object in local timezone
 * Output Note: Object with UTC timestamp
 * @param dates
 * @param value_type
 */
export function getMappedRangeValue(dates: RangePickerValue, value_type?: "string" | "number") {
  let val: MappedRangeType | undefined;

  if (Array.isArray(dates) && dates.length > 1) {
    val = {
      $gt: dates[0] ? moment.utc(dates[0].format(FORMAT), FORMAT).startOf("day").unix() : undefined,
      $lt: dates[1] ? moment.utc(dates[1].format(FORMAT), FORMAT).endOf("day").unix() : undefined
    };

    if (value_type === "string") {
      val = {
        $gt: val.$gt?.toString(),
        $lt: val.$lt?.toString()
      };
    }
  }
  return val;
}
